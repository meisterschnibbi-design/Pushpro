package com.pushpro.app.net

import android.content.Context
import android.util.Base64
import com.pushpro.app.util.LogUtil
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Minimaler SMTP-Client für Test- und Echtversand.
 * Unterstützt: NONE (25), STARTTLS (587), SSL/TLS (465)
 *
 * Wichtig: Viele Provider (z. B. Gmail) verlangen App-Passwörter / spezielle Ports.
 */
object EmailSender {

    /** tlsMode: 0=None, 1=STARTTLS, 2=SSL/TLS */
    fun sendTestEmail(
        ctx: Context,
        host: String,
        portStr: String,
        user: String,
        pass: String,
        tlsMode: Int,
        recipient: String,
        subjectPrefix: String?
    ): Pair<Boolean, String> {
        val subject = ((subjectPrefix ?: "").takeIf { it.isNotBlank() }?.let { "$it " } ?: "") + "PushPro Test"
        val now = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val body = "Test from PushPro at $now"
        return sendEmail(ctx, host, portStr, user, pass, tlsMode, recipient, subject, body)
    }

    /** Echter Versand (für Weiterleitung realer Pushes) */
    fun sendEmail(
        ctx: Context,
        host: String,
        portStr: String,
        user: String,
        pass: String,
        tlsMode: Int,
        recipient: String,
        subject: String,
        body: String
    ): Pair<Boolean, String> {
        return try {
            val port = portStr.toIntOrNull() ?: when (tlsMode) { 2 -> 465; 1 -> 587; else -> 25 }
            val domain = "android.pushpro"

            fun connectPlain(): Triple<Socket, BufferedReader, BufferedWriter> {
                val sock = Socket()
                sock.soTimeout = 10000
                sock.connect(InetSocketAddress(host, port), 10000)
                val r = BufferedReader(InputStreamReader(sock.getInputStream(), Charsets.UTF_8))
                val w = BufferedWriter(OutputStreamWriter(sock.getOutputStream(), Charsets.UTF_8))
                return Triple(sock, r, w)
            }

            fun upgradeToTls(sock: Socket): Triple<Socket, BufferedReader, BufferedWriter> {
                val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
                val ssl = factory.createSocket(sock, host, port, true) as SSLSocket
                ssl.soTimeout = 10000
                ssl.startHandshake()
                val r = BufferedReader(InputStreamReader(ssl.getInputStream(), Charsets.UTF_8))
                val w = BufferedWriter(OutputStreamWriter(ssl.getOutputStream(), Charsets.UTF_8))
                return Triple(ssl, r, w)
            }

            fun send(w: BufferedWriter, s: String) { w.write(s); w.write("\r\n"); w.flush() }
            fun readLine(r: BufferedReader): String = r.readLine() ?: ""

            fun readExpect(r: BufferedReader, code: String): String {
                val line = readLine(r)
                if (!line.startsWith(code)) throw RuntimeException("Expected $code but got: $line")
                return line
            }

            fun readEhlo(r: BufferedReader) {
                // Erste Zeile muss 250 sein; ggf. 250-Feature-Liste, bis finale 250 <OK>
                var line = readLine(r)
                if (!line.startsWith("250")) throw RuntimeException("EHLO not accepted: $line")
                while (line.startsWith("250-")) line = readLine(r)
                if (!line.startsWith("250")) throw RuntimeException("EHLO end not OK: $line")
            }

            var (sock, r, w) = if (tlsMode == 2) {
                // SMTPS (SSL/TLS)
                val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
                val ssl = factory.createSocket(host, port) as SSLSocket
                ssl.soTimeout = 10000
                ssl.startHandshake()
                Triple(ssl as Socket,
                    BufferedReader(InputStreamReader(ssl.getInputStream(), Charsets.UTF_8)),
                    BufferedWriter(OutputStreamWriter(ssl.getOutputStream(), Charsets.UTF_8)))
            } else {
                connectPlain()
            }

            // 220 Greeting
            readExpect(r, "220")

            // EHLO
            send(w, "EHLO $domain"); readEhlo(r)

            if (tlsMode == 1) {
                // STARTTLS 220 -> TLS-Upgrade -> EHLO erneut
                send(w, "STARTTLS")
                readExpect(r, "220")
                val up = upgradeToTls(sock)
                sock = up.first; r = up.second; w = up.third
                send(w, "EHLO $domain"); readEhlo(r)
            }

            // AUTH LOGIN (falls user gesetzt)
            if (user.isNotBlank()) {
                send(w, "AUTH LOGIN")
                readExpect(r, "334") // username?
                send(w, Base64.encodeToString(user.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
                readExpect(r, "334") // password?
                send(w, Base64.encodeToString(pass.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
                readExpect(r, "235") // authenticated
            }

            val from = if (user.isNotBlank()) user else "noreply@pushpro"

            // MAIL / RCPT / DATA
            send(w, "MAIL FROM:<$from>"); readExpect(r, "250")
            send(w, "RCPT TO:<$recipient>"); readExpect(r, "250")
            send(w, "DATA"); readExpect(r, "354")

            // Nachricht
            w.write("Subject: $subject\r\n")
            w.write("From: $from\r\n")
            w.write("To: $recipient\r\n")
            w.write("MIME-Version: 1.0\r\n")
            w.write("Content-Type: text/plain; charset=UTF-8\r\n")
            w.write("\r\n")
            w.write(body)
            w.write("\r\n.\r\n")
            w.flush()

            readExpect(r, "250")
            send(w, "QUIT")
            sock.close()

            run { val isTest = subject == "PushPro Test"; LogUtil.append(ctx, (if (isTest) "Email test OK" else "Email OK") + " → " + recipient + " via " + host + ":" + portStr + " (mode=" + tlsMode + ")") }
            true to "OK"
        } catch (e: Throwable) {
            run { val isTest = subject == "PushPro Test"; LogUtil.append(ctx, if (isTest) "Email test FAILED (${e.message ?: "error"})" else "Email FAILED (${e.message ?: "error"})") }
            false to (e.message ?: "error")
        }
    }
}
