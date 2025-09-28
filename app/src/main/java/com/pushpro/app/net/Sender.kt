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

    // ---------------- Public API ----------------

    fun sendWebhookTest(
        ctx: Context,
        url: String,
        methodIndex: Int,
        templateIndex: Int,
        headersJson: String?
    ) {
        thread {
            sendWebhook(
                ctx = ctx,
                url = url,
                methodIndex = methodIndex,
                templateIndex = templateIndex,
                headersJson = headersJson,
                title = "PushPro Test",
                text = "It works",
                pkg = ctx.packageName
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
                ctx = ctx,
                token = token,
                chatId = chatId,
                parseModeIdx = parseModeIdx,
                headerPrefix = headerPrefix,
                text = "Test from PushPro",
                disablePreview = disablePreview,
                silent = silent,
                protect = protect
            )
        }
    }

    /** Weiterleitung realer Benachrichtigungen (vom NotificationListener). */
    fun forward(ctx: Context, title: String, text: String, pkg: String) {
        val p = ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE)

        // ----- EMAIL -----
        if (p.getBoolean("email_enabled", false) &&
            allowByWhitelist(p.getString("email_input_whitelist", ""), title, text, pkg)
        ) {
            val host = p.getString("email_input_host", "") ?: ""
            val port = p.getString("email_input_port", "") ?: ""
            val user = p.getString("email_input_user", "") ?: ""
            val pass = p.getString("email_input_pass", "") ?: ""
            val tls  = p.getInt("email_input_tls_mode", 1) // 0=None, 1=STARTTLS, 2=SSL
            val to   = p.getString("email_input_recipient", "") ?: ""
            val prefix = p.getString("email_input_subject_prefix", "") ?: ""
            val subject = (if (prefix.isNotBlank()) "$prefix " else "") + title
            val body = buildString {
                append(text).append('\n')
                append(pkg).append('\n')
                append(sdf.format(Date()))
            }
            thread {
                EmailSender.sendEmail(ctx, host, port, user, pass, tls, to, subject, body)
            }
        }

        // ----- WEBHOOK -----
        if (p.getBoolean("wh_enabled", false) &&
            allowByWhitelist(p.getString("wh_whitelist", ""), title, text, pkg)
        ) {
            val url = p.getString("wh_url", "") ?: ""
            val methodIdx = p.getInt("wh_method", 1) // POST
            val tplIdx = p.getInt("wh_template", 0)  // json
            val headers = p.getString("wh_headers", "")
            thread { sendWebhook(ctx, url, methodIdx, tplIdx, headers, title, text, pkg) }
        }

        // ----- TELEGRAM -----
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
                    ctx = ctx,
                    token = token,
                    chatId = chatId,
                    parseModeIdx = parseIdx,
                    headerPrefix = header,
                    text = "$title\n$text",
                    disablePreview = disablePreview,
                    silent = silent,
                    protect = protect
                )
            }
        }
    }

    // ---------------- Implementierung ----------------

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
            require(url.startsWith("http")) { "Invalid URL" }

            val method = listOf("GET", "POST", "PUT", "PATCH").getOrElse(methodIndex) { "POST" }
            val template = listOf("json", "form", "plain", "xml").getOrElse(templateIndex) { "json" }

            val now = sdf.format(Date())

            val payload: String
            val contentType: String
            when (template) {
                "json" -> {
                    payload =
                        "{\"title\":\"${escapeJson(title)}\"," +
                        "\"text\":\"${escapeJson(text)}\"," +
                        "\"package\":\"${escapeJson(pkg)}\"," +
                        "\"time\":\"${escapeJson(now)}\"}"
                    contentType = "application/json; charset=utf-8"
                }
                "form" -> {
                    payload = "title=${enc(title)}&text=${enc(text)}&package=${enc(pkg)}&time=${enc(now)}"
                    contentType = "application/x-www-form-urlencoded; charset=utf-8"
                }
                "plain" -> {
                    payload = "$title\n$text\n$pkg\n$now"
                    contentType = "text/plain; charset=utf-8"
                }
                else -> { // xml
                    payload =
                        "<push><title>${escapeXml(title)}</title>" +
                        "<text>${escapeXml(text)}</text>" +
                        "<package>${escapeXml(pkg)}</package>" +
                        "<time>${escapeXml(now)}</time></push>"
                    contentType = "application/xml; charset=utf-8"
                }
            }

            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = method
                doInput = true
                if (method != "GET") doOutput = true
                setRequestProperty("Content-Type", contentType)
                // optionale Header (einfaches JSON oder "Key: Value"-Zeilen)
                headersJson?.let { applyUserHeaders(this, it) }
            }

            if (conn.doOutput) {
                BufferedOutputStream(conn.outputStream).use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            }

            val code = conn.responseCode
            try { BufferedReader(InputStreamReader(conn.inputStream)).readText() } catch (_: Throwable) {}

            LogUtil.append(ctx, if (code in 200..299) "Webhook test OK" else "Webhook test FAILED (code=$code)")
            if (title == "PushPro Test") toast(ctx, if (code in 200..299) "Webhook test OK" else "Webhook test failed")

            ctx.getSharedPreferences("pushpro_prefs", Context.MODE_PRIVATE).edit()
                .putBoolean("last_send_error_webhook", code !in 200..299)
                .apply()

            conn.disconnect()
        } catch (e: Throwable) {
            LogUtil.append(ctx, "Webhook test FAILED (${e.message ?: "error"})")
            if (title == "PushPro Test") toast(ctx, "Webhook test failed")
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
            require(token.isNotBlank() && chatId.isNotBlank()) { "Missing token/chatId" }

            val parse = listOf("None", "Markdown", "HTML").getOrElse(parseModeIdx) { "None" }
            val url = "https://api.telegram.org/bot$token/sendMessage"

            val msg = (headerPrefix?.takeIf { it.isNotBlank() }?.let { "$it\n" } ?: "") + text
            val params = mutableMapOf(
                "chat_id" to chatId,
                "text" to msg
            )
            if (parse != "None") params["parse_mode"] = parse
            if (disablePreview) params["disable_web_page_preview"] = "true"
            if (silent) params["disable_notification"] = "true"
            if (protect) params["protect_content"] = "true"

            val body = params.entries.joinToString("&") { (k, v) -> "${enc(k)}=${enc(v)}" }

            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
            }
            BufferedOutputStream(conn.outputStream).use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            try { BufferedReader(InputStreamReader(conn.inputStream)).readText() } catch (_: Throwable) {}

            LogUtil.append(ctx, if (code in 200..299) "Telegram test OK" else "Telegram test FAILED (code=$code)")
            if (text == "Test from PushPro") toast(ctx, if (code in 200..299) "Telegram test OK" else "Telegram test failed")

            ctx.getSharedPreferences("pushpro_prefs", Context.MODE_PRIVATE).edit()
                .putBoolean("last_send_error_tg", code !in 200..299)
                .apply()

            conn.disconnect()
        } catch (e: Throwable) {
            LogUtil.append(ctx, "Telegram test FAILED (${e.message ?: "error"})")
            if (text == "Test from PushPro") toast(ctx, "Telegram test failed")
            ctx.getSharedPreferences("pushpro_prefs", Context.MODE_PRIVATE).edit()
                .putBoolean("last_send_error_tg", true)
                .apply()
        }
    }

    // ---------------- Helpers ----------------

    private fun allowByWhitelist(whitelistCsv: String?, title: String, text: String, pkg: String): Boolean {
        val wl = (whitelistCsv ?: "").split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (wl.isEmpty()) return true
        if (wl.any { it == "*" }) return true
        val hay = (title + " " + text + " " + pkg).lowercase(Locale.US)
        return wl.any { token -> hay.contains(token.lowercase(Locale.US)) }
    }

    /** Akzeptiert einfache „Key: Value“-Zeilen oder sehr simples JSON {"K":"V","H":"W"}. */
    private fun applyUserHeaders(conn: HttpURLConnection, headers: String) {
        val t = headers.trim()
        if (t.startsWith("{") && t.endsWith("}")) {
            val inner = t.substring(1, t.length - 1)
            inner.split(',').forEach { part ->
                val kv = part.split(':', limit = 2)
                if (kv.size == 2) {
                    val k = kv[0].trim().trim('"', '\'')
                    val v = kv[1].trim().trim('"', '\'')
                    if (k.isNotEmpty()) conn.setRequestProperty(k, v)
                }
            }
        } else {
            t.lines().forEach { line ->
                val idx = line.indexOf(':')
                if (idx > 0) {
                    val k = line.substring(0, idx).trim()
                    val v = line.substring(idx + 1).trim()
                    if (k.isNotEmpty()) conn.setRequestProperty(k, v)
                }
            }
        }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    private fun escapeJson(s: String): String =
        s.replace("\\", "\\\\")
         .replace("\"", "\\\"")
         .replace("\n", "\\n")
         .replace("\r", "\\r")

    private fun escapeXml(s: String): String =
        s.replace("&", "&amp;")
         .replace("<", "&lt;")
         .replace(">", "&gt;")
         .replace("\"", "&quot;")
         .replace("'", "&apos;")

    private fun toast(ctx: Context, msg: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
