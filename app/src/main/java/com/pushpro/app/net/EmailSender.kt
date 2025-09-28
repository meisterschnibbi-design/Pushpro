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

/** Minimal SMTP client for test sends (None/STARTTLS/SSL). */
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
        return try {
            val port = portStr.toIntOrNull() ?: when (tlsMode) { 2 -> 465; 1 -> 587; else -> 25 }
            val domain = "android.pushpro"

            fun connectPlain(): Triple<Socket, BufferedReader, BufferedWriter> {
                val sock = Socket()
                sock.soTimeout = 8000
                sock.connect(InetSocketAddress(host, port), 8000)
                val reader = BufferedReader(InputStreamReader(sock.getInputStream(), Charsets.UTF_8))
                val writer = BufferedWriter(OutputStreamWriter(sock.getOutputStream(), Charsets.UTF_8))
                return Triple(sock, reader, writer)
            }

            fun upgradeToTls(sock: Socket): Triple<Socket, BufferedReader, BufferedWriter> {
                val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
                val ssl = factory.createSocket(sock, host, port, true) as SSLSocket
                ssl.startHandshake()
                val reader = BufferedReader(InputStreamReader(ssl.getInputStream(), Charsets.UTF_8))
                val writer = BufferedWriter(OutputStreamWriter(ssl.getOutputStream(), Charsets.UTF_8))
                return Triple(ssl, reader, writer)
            }

            fun readLine(r: BufferedReader): String = r.readLine() ?: ""
            fun send(w: BufferedWriter, s: String) { w.write(s + "\r\n"); w.flush() }

            var (sock, r, w) = if (tlsMode == 2) {
                val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
                val ssl = factory.createSocket(host, port) as SSLSocket
                ssl.soTimeout = 8000
                ssl.startHandshake()
                Triple(ssl as Socket,
                    BufferedReader(InputStreamReader(ssl.getInputStream(), Charsets.UTF_8)),
                    BufferedWriter(OutputStreamWriter(ssl.getOutputStream(), Charsets.UTF_8)))
            } else {
                connectPlain()
            }

            // Greeting
            readLine(r)

            // EHLO
            send(w, "EHLO $domain"); readLine(r)

            if (tlsMode == 1) {
                // STARTTLS
                send(w, "STARTTLS")
                val resp = readLine(r)
                if (!resp.startsWith("220")) throw RuntimeException("STARTTLS failed: $resp")
                val t = upgradeToTls(sock)
                sock = t.first; r = t.second; w = t.third
                send(w, "EHLO $domain"); readLine(r)
            }

            if (user.isNotBlank()) {
                send(w, "AUTH LOGIN"); readLine(r)
                send(w, Base64.encodeToString(user.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)); readLine(r)
                send(w, Base64.encodeToString(pass.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
                val authResp = readLine(r)
                if (!authResp.startsWith("235")) throw RuntimeException("AUTH failed: $authResp")
            }

            val from = user.ifBlank { "noreply@pushpro" }

            send(w, "MAIL FROM:<$from>"); readLine(r)
            send(w, "RCPT TO:<$recipient>"); readLine(r)
            send(w, "DATA"); readLine(r)

            val now = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
            val subject = ((subjectPrefix ?: "").takeIf { it.isNotBlank() }?.let { "$it " } ?: "") + "PushPro Test"
            val body = "Test from PushPro at $now"

            w.write("Subject: $subject\r\n")
            w.write("From: $from\r\n")
            w.write("To: $recipient\r\n")
            w.write("MIME-Version: 1.0\r\n")
            w.write("Content-Type: text/plain; charset=UTF-8\r\n")
            w.write("\r\n")
            w.write(body)
            w.write("\r\n.\r\n")
            w.flush()

            val dataResp = readLine(r)
            if (!dataResp.startsWith("250")) throw RuntimeException("DATA failed: $dataResp")

            send(w, "QUIT")
            sock.close()

            LogUtil.append(ctx, "Email test OK → $recipient via $host:$port (mode=$tlsMode)")
            Pair(true, "OK")
        } catch (e: Throwable) {
            LogUtil.append(ctx, "Email test FAILED (${e.message ?: "error"})")
            Pair(false, e.message ?: "error")
        }
    }
}
