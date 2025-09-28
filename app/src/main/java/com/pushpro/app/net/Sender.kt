package com.pushpro.app.net

import android.content.Context
import com.pushpro.app.util.LogUtil
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

object Sender {

    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    // Public API

    fun sendWebhookTest(
        ctx: Context,
        url: String,
        methodIndex: Int,
        templateIndex: Int,
        headersJson: String?
    ) {
        thread {
            sendWebhook(
                ctx,
                url,
                methodIndex,
                templateIndex,
                headersJson,
                "PushPro Test",
                "It works",
                ctx.packageName
            )
        }
    }

    fun sendTelegramTest(
        ctx: Context,
        token: String,
        chatId: String,
        parseModeIdx: Int,
        headerPrefix: String?,
        disablePreview: Boolean,
        silent: Boolean,
        protect: Boolean
    ) {
        thread {
            sendTelegram(
                ctx,
                token,
                chatId,
                parseModeIdx,
                headerPrefix,
                "Test from PushPro",
                disablePreview,
                silent,
                protect
            )
        }
    }

    fun forward(ctx: Context, title: String, text: String, pkg: String) {
        val p = ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE)

        // WEBHOOK
        if (p.getBoolean("wh_enabled", false) &&
            allowByWhitelist(p.getString("wh_whitelist", ""), title, text, pkg)
        ) {
            val url = p.getString("wh_url", "") ?: ""
            val methodIdx = p.getInt("wh_method", 1) // default POST
            val tplIdx = p.getInt("wh_template", 0) // default json
            val headers = p.getString("wh_headers", "")
            thread { sendWebhook(ctx, url, methodIdx, tplIdx, headers, title, text, pkg) }
        }

        // TELEGRAM
        if (p.getBoolean("tg_enabled", false) &&
            allowByWhitelist(p.getString("tg_whitelist", ""), title, text, pkg)
        ) {
            val token = p.getString("tg_token", "") ?: ""
            val chatId = p.getString("tg_chat_id", "") ?: ""
            val parseIdx = p.getInt("tg_parse_mode", 0)
            val header = p.getString("tg_header_prefix", "") ?: ""
            val disablePreview = p.getBoolean("tg_disable_preview", false)
            val silent = p.getBoolean("tg_silent", false)
            val protect = p.getBoolean("tg_protect", false)
            thread {
                sendTelegram(
                    ctx,
                    token,
                    chatId,
                    parseIdx,
                    header,
                    "$title\n$text",
                    disablePreview,
                    silent,
                    protect
                )
            }
        }
    }

    // Impl

    private fun sendWebhook(
        ctx: Context,
        url: String,
        methodIndex: Int,
        templateIndex: Int,
        headersJson: String?,
        title: String,
        text: String,
        pkg: String
    ) {
        try {
            if (!url.startsWith("http")) throw IllegalArgumentException("Invalid URL")
            val method = listOf("GET", "POST", "PUT", "PATCH").getOrElse(methodIndex) { "POST" }
            val template = listOf("json", "form", "plain", "xml").getOrElse(templateIndex) { "json" }

            val payload = when (template) {
                "json" -> """{"title":"${escapeJson(title)}","text":"${escapeJson(text)}","package":"${escapeJson(pkg)}","time":"${sdf.format(Date())}"}"""
                "form" -> "title=" + enc(title) + "&text=" + enc(text) +
                          "&package=" + enc(pkg) + "&time=" + enc(sdf.format(Date()))
                "plain" -> "$title\n$text\n$pkg\n${sdf.format(Date())}"
                else -> "<push><title>${escapeXml(title)}</title><text>${escapeXml(text)}</text><package>${escapeXml(pkg)}</package><time>${escapeXml(sdf.format(Date()))}</time></push>"
            }

            val u = URL(url)
            val conn = (u.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = method
                doInput = true
                if (method != "GET") doOutput = true
                if (template == "json") setRequestProperty("Content-Type", "application/json; charset=utf-8")
                if (template == "form") setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                headersJson?.let { applyUserHeaders(this, it) }
            }

            if (conn.doOutput) {
                val bodyBytes = payload.toByteArray(Charsets.UTF_8)
                BufferedOutputStream(conn.outputStream).use { it.write(bodyBytes) }
            }

            val code = conn.responseCode
            val resp = try { BufferedReader(InputStreamReader(conn.inputStream)).readText() }
                       catch (_: Throwable) { "" }
            LogUtil.append(ctx, "Webhook send -> code=$code; resp_len=${resp.length}")
            ctx.getSharedPreferences("pushpro_prefs", Context.MODE_PRIVATE).edit()
                .putBoolean("last_send_error_webhook", code !in 200..299)
                .apply()
            conn.disconnect()
        } catch (e: Throwable) {
            LogUtil.append(ctx, "Webhook error: ${e.message}")
            ctx.getSharedPreferences("pushpro_prefs", Context.MODE_PRIVATE).edit()
                .putBoolean("last_send_error_webhook", true)
                .apply()
        }
    }

    private fun sendTelegram(
        ctx: Context,
        token: String,
        chatId: String,
        parseModeIdx: Int,
        headerPrefix: String?,
        text: String,
        disablePreview: Boolean,
        silent: Boolean,
        protect: Boolean
    ) {
        try {
            if (token.isBlank() || chatId.isBlank()) throw IllegalArgumentException("Missing token/chatId")
            val parse = listOf("None", "Markdown", "HTML").getOrElse(parseModeIdx) { "None" }
            val url = "https://api.telegram.org/bot${token}/sendMessage"

            val msg = (headerPrefix?.takeIf { it.isNotBlank() }?.let { "$it\n" } ?: "") + text
            val qp = mutableMapOf(
                "chat_id" to chatId,
                "text" to msg
            )
            if (parse != "None") qp["parse_mode"] = parse
            if (disablePreview) qp["disable_web_page_preview"] = "true"
            if (silent) qp["disable_notification"] = "true"
            if (protect) qp["protect_content"] = "true"

            val body = qp.map { (k, v) -> enc(k) + "=" + enc(v) }.joinToString("&")
            val u = URL(url)
            val conn = (u.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
            }
            BufferedOutputStream(conn.outputStream).use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val resp = try { BufferedReader(InputStreamReader(conn.inputStream)).readText() }
                       catch (_: Throwable) { "" }
            LogUtil.append(ctx, "Telegram send -> code=$code; resp_len=${resp.length}")
            ctx.getSharedPreferences("pushpro_prefs", Context.MODE_PRIVATE).edit()
                .putBoolean("last_send_error_tg", code !in 200..299)
                .apply()
            conn.disconnect()
        } catch (e: Throwable) {
            LogUtil.append(ctx, "Telegram error: ${e.message}")
            ctx.getSharedPreferences("pushpro_prefs", Context.MODE_PRIVATE).edit()
                .putBoolean("last_send_error_tg", true)
                .apply()
        }
    }

    private fun allowByWhitelist(whitelistCsv: String?, title: String, text: String, pkg: String): Boolean {
        val wl = (whitelistCsv ?: "").split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (wl.isEmpty()) return true // empty -> allow all
        if (wl.any { it == "*" }) return true
        val hay = (title + " " + text + " " + pkg).lowercase(Locale.US)
        return wl.any { token -> hay.contains(token.lowercase(Locale.US)) }
    }

    private fun applyUserHeaders(conn: HttpURLConnection, headers: String) {
        val trimmed = headers.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            val parts = trimmed.removePrefix("{").removeSuffix("}")
                .split(',').map { it.trim() }.filter { it.contains(":") }
            for (p in parts) {
                val kv = p.split(':', limit = 2)
                val k = kv[0].trim().trim('"', '\'')
                val v = kv[1].trim().trim('"', '\'')
                if (k.isNotEmpty()) conn.setRequestProperty(k, v)
            }
        } else {
            trimmed.lines().forEach {
                val idx = it.indexOf(':')
                if (idx > 0) {
                    val k = it.substring(0, idx).trim()
                    val v = it.substring(idx + 1).trim()
                    if (k.isNotEmpty()) conn.setRequestProperty(k, v)
                }
            }
        }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    private fun escapeJson(s: String): String = s.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

    private fun escapeXml(s: String): String = s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
