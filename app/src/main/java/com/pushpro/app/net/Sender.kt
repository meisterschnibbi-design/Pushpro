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

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

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

    private fun matchContains(filterCsv: String?, title: String, text: String, pkg: String): Boolean {
        val f = (filterCsv ?: "").split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (f.isEmpty()) return true
        val hay = (title + " " + text + " " + pkg).lowercase(Locale.US)
        return f.any { tok -> hay.contains(tok.lowercase(Locale.US)) }
    }

    private fun allowByWhitelist(wlCsv: String?, title: String, text: String, pkg: String): Boolean {
        val wl = (wlCsv ?: "").split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (wl.isEmpty()) return true
        if (wl.any { it == "*" }) return true
        val hay = (title + " " + text + " " + pkg).lowercase(Locale.US)
        return wl.any { token -> hay.contains(token.lowercase(Locale.US)) }
    }

    private fun resolveTpl(tpl: String, title: String, text: String, pkg: String, time: String): String =
        tpl.replace("{title}", title)
            .replace("{text}", text)
            .replace("{package}", pkg)
            .replace("{time}", time)

    // ---------- Helpers for nicer text & package ----------
    private fun cleanOneLine(s: String): String =
        s.replace("\\n", " ")
            .replace("\\r", " ")
            .replace("\\t", " ")
            .replace("\r", " ")
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun compactMultiline(s: String): String =
        s.replace(Regex("(\\r?\\n){2,}"), "\n").trim()

    private fun normalizePkg(pkg: String): String =
        pkg.replace(Regex("\\.clone(\\d+)\\.clone\\1$"), ".clone$1")

    // Public tests
    fun sendWebhookTest(
        ctx: Context,
        url: String,
        methodIndex: Int,
        templateIndex: Int,
        headersJson: String?
    ) {
        url.split(Regex("""[,;\s]+"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .forEach { one ->
                thread {
                    sendWebhook(
                        ctx, one, methodIndex, templateIndex, headersJson,
                        "PushPro Test", "It works", ctx.packageName
                    )
                }
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
        chatId.split(Regex("""[,;\s]+"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .forEach { one ->
                thread {
                    sendTelegram(
                        ctx, token, one, parseModeIdx, headerPrefix,
                        "Test from PushPro", null, disablePreview, silent, protect
                    )
                }
            }
    }

    // Forward real pushes
    fun forward(ctx: Context, title: String, text: String, pkg: String) {
        val cleanTitle = cleanOneLine(title)
        val cleanText = cleanOneLine(text)

        // Gate: Hauptschalter global_enabled
        val pStatus = ctx.getSharedPreferences("pushpro_prefs", Context.MODE_PRIVATE)
        if (!pStatus.getBoolean("global_enabled", false)) return

        val p = ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE)

        // Global blacklist check
        run {
            val b = ctx.getSharedPreferences("pushpro_prefs", Context.MODE_PRIVATE)
            val blocked = b.getStringSet("blacklist_set", emptySet()) ?: emptySet()
            if (blocked.contains(pkg)) {
                LogUtil.append(ctx, "Blocked by blacklist: $pkg")
                return
            }
        }

        // EMAIL
        if (p.getBoolean("email_enabled", false) &&
            allowByWhitelist(p.getString("email_input_whitelist", ""), cleanTitle, cleanText, pkg) &&
            matchContains(p.getString("email_input_contains", ""), cleanTitle, cleanText, pkg)
        ) {
            val host = p.getString("email_input_host", "") ?: ""
            val port = p.getString("email_input_port", "") ?: ""
            val user = p.getString("email_input_user", "") ?: ""
            val pass = p.getString("email_input_pass", "") ?: ""
            val tls = p.getInt("email_input_tls_mode", 1) // 0=None, 1=STARTTLS, 2=SSL/TLS
            val to = p.getString("email_input_recipient", "") ?: ""
            val prefix = p.getString("email_input_subject_prefix", "") ?: ""
            val subject = (if (prefix.isNotBlank()) "$prefix " else "") + cleanTitle
            val body = buildString {
                append(cleanText).append('\n')
                append(normalizePkg(pkg)).append('\n')
                append(sdf.format(Date()))
            }
            to.split(Regex("""[,;\s]+"""))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .forEach { one ->
                    thread { EmailSender.sendEmail(ctx, host, port, user, pass, tls, one, subject, body) }
                }
        }

        // WEBHOOK
        if (p.getBoolean("wh_enabled", false) &&
            allowByWhitelist(p.getString("wh_whitelist", ""), cleanTitle, cleanText, pkg) &&
            matchContains(p.getString("wh_contains", ""), cleanTitle, cleanText, pkg)
        ) {
            val url = p.getString("wh_url", "") ?: ""
            val methodIdx = p.getInt("wh_method", 1)
            val tplIdx = p.getInt("wh_template", 0)
            val headers = p.getString("wh_headers", "")
            url.split(Regex("""[,;\s]+"""))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .forEach { one ->
                    thread { sendWebhook(ctx, one, methodIdx, tplIdx, headers, cleanTitle, cleanText, pkg) }
                }
        }

        // TELEGRAM
        if (p.getBoolean("tg_enabled", false) &&
            allowByWhitelist(p.getString("tg_whitelist", ""), cleanTitle, cleanText, pkg) &&
            matchContains(p.getString("tg_contains", ""), cleanTitle, cleanText, pkg)
        ) {
            val token = p.getString("tg_token", "") ?: ""
            val chatId = p.getString("tg_chat_id", "") ?: ""
            val parseIdx = p.getInt("tg_parse_mode", 0)
            val header = p.getString("tg_header_prefix", "") ?: ""
            val disablePreview = p.getBoolean("tg_disable_preview", false)
            val silent = p.getBoolean("tg_silent", false)
            val protect = p.getBoolean("tg_protect", false)

            chatId.split(Regex("""[,;\s]+"""))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .forEach { one ->
                    thread {
                        // packageForTpl = pkg (Absender-Paket anzeigen)
                        sendTelegram(
                            ctx, token, one, parseIdx, header,
                            "$cleanTitle\n$cleanText", pkg, disablePreview, silent, protect
                        )
                    }
                }
        }
    }

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
            val kinds = listOf("json", "form", "plain", "xml")
            val kind = kinds.getOrElse(templateIndex) { "json" }
            val now = sdf.format(Date())

            val px = ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE)
            val stored = px.getString("wh_tpl_$kind", "") ?: ""
            val defaultBody = when (kind) {
                "json" -> """{ "title": "{title}", "text": "{text}", "package": "{package}", "time": "{time}" }"""
                "form" -> """title={title}&text={text}&package={package}&time={time}"""
                "plain" -> "{title}\n{text}\n{package}\n{time}"
                else -> """<push><title>{title}</title><text>{text}</text><package>{package}</package><time>{time}</time></push>"""
            }
            val body = resolveTpl(
                if (stored.isNotBlank()) stored else defaultBody,
                title,
                text,
                normalizePkg(pkg),
                now
            )

            val method = listOf("GET", "POST", "PUT", "PATCH").getOrElse(methodIndex) { "POST" }
            val contentType = when (kind) {
                "json" -> "application/json; charset=utf-8"
                "form" -> "application/x-www-form-urlencoded; charset=utf-8"
                "plain" -> "text/plain; charset=utf-8"
                else -> "application/xml; charset=utf-8"
            }

            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = method
                doInput = true
                if (method != "GET") doOutput = true
                setRequestProperty("Content-Type", contentType)
                headersJson?.let { applyUserHeaders(this, it) }
            }
            if (conn.doOutput) {
                BufferedOutputStream(conn.outputStream).use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            try { BufferedReader(InputStreamReader(conn.inputStream)).readText() } catch (_: Throwable) {}
            run {
                val isTest = (title == "PushPro Test")
                LogUtil.append(
                    ctx,
                    if (code in 200..299)
                        if (isTest) "Webhook test OK" else "Webhook OK"
                    else
                        if (isTest) "Webhook test FAILED (code=$code)" else "Webhook FAILED (code=$code)"
                )
            }
            if (title == "PushPro Test") toast(ctx, if (code in 200..299) "Webhook test OK" else "Webhook test failed")
            ctx.getSharedPreferences("pushpro_prefs", Context.MODE_PRIVATE).edit()
                .putBoolean("last_send_error_webhook", code !in 200..299).apply()
            conn.disconnect()
        } catch (e: Throwable) {
            run {
                val isTest = (title == "PushPro Test")
                LogUtil.append(
                    ctx,
                    if (isTest) "Webhook test FAILED (${e.message ?: "error"})"
                    else "Webhook FAILED (${e.message ?: "error"})"
                )
            }
            if (title == "PushPro Test") toast(ctx, "Webhook test failed")
            ctx.getSharedPreferences("pushpro_prefs", Context.MODE_PRIVATE).edit()
                .putBoolean("last_send_error_webhook", true).apply()
        }
    }

    // packageForTpl steuert, welcher Paketname in {package} landet.
    private fun sendTelegram(
        ctx: Context,
        token: String,
        chatId: String,
        parseModeIdx: Int,
        headerPrefix: String?,
        text: String,
        packageForTpl: String?,
        disablePreview: Boolean,
        silent: Boolean,
        protect: Boolean
    ) {
        try {
            val parse = listOf("None", "Markdown", "HTML").getOrElse(parseModeIdx) { "None" }
            val url = "https://api.telegram.org/bot$token/sendMessage"

            val px = ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE)
            val tplStored = px.getString("tg_tpl", "") ?: ""
            val tplDefault = "{title}\n{text}\n{package}\n{time}"
            val now = sdf.format(Date())
            val title = headerPrefix?.takeIf { it.isNotBlank() } ?: ""
            val effectivePkg = normalizePkg(packageForTpl ?: ctx.packageName)
            val rawMsg = resolveTpl(if (tplStored.isNotBlank()) tplStored else tplDefault, title, text, effectivePkg, now)
            val msg = compactMultiline(rawMsg)

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
            run {
                val isTest = (text == "Test from PushPro")
                LogUtil.append(
                    ctx,
                    if (code in 200..299)
                        if (isTest) "Telegram test OK" else "Telegram OK"
                    else
                        if (isTest) "Telegram test FAILED (code=$code)" else "Telegram FAILED (code=$code)"
                )
            }
            if (text == "Test from PushPro") toast(ctx, if (code in 200..299) "Telegram test OK" else "Telegram test failed")
            ctx.getSharedPreferences("pushpro_prefs", Context.MODE_PRIVATE).edit()
                .putBoolean("last_send_error_tg", code !in 200..299).apply()
            conn.disconnect()
        } catch (e: Throwable) {
            run {
                val isTest = (text == "Test from PushPro")
                LogUtil.append(
                    ctx,
                    if (isTest) "Telegram test FAILED (${e.message ?: "error"})"
                    else "Telegram FAILED (${e.message ?: "error"})"
                )
            }
            if (text == "Test from PushPro") toast(ctx, "Telegram test failed")
            ctx.getSharedPreferences("pushpro_prefs", Context.MODE_PRIVATE).edit()
                .putBoolean("last_send_error_tg", true).apply()
        }
    }

    // Headers helper (simple JSON oder "Key: Value"-Zeilen)
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
}
