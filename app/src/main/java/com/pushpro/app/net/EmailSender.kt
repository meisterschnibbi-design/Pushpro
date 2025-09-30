package com.pushpro.app.net

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.pushpro.app.util.LogUtil
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlin.concurrent.thread

object EmailSender {

    private fun toast(ctx: Context, msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun sendEmailTest(ctx: Context, to: String, subject: String, body: String) {
        thread {
            try {
                val props = Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        // TODO: Zugangsdaten einfügen oder aus Settings laden
                        return PasswordAuthentication("user@example.com", "password")
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress("user@example.com"))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                    setSubject(subject)
                    setText(body)
                }

                Transport.send(message)

                LogUtil.append(ctx, "Email test OK")
                toast(ctx, "Email test OK")   // <<< Toast hinzugefügt
            } catch (e: Exception) {
                LogUtil.append(ctx, "Email test FAILED (${e.message ?: "error"})")
                toast(ctx, "Email test failed")   // <<< Toast hinzugefügt
            }
        }
    }

    fun sendEmail(ctx: Context, to: String, subject: String, body: String) {
        thread {
            try {
                val props = Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication("user@example.com", "password")
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress("user@example.com"))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                    setSubject(subject)
                    setText(body)
                }

                Transport.send(message)

                LogUtil.append(ctx, "Email OK")
            } catch (e: Exception) {
                LogUtil.append(ctx, "Email FAILED (${e.message ?: "error"})")
            }
        }
    }
}
