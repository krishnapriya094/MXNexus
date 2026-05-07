package com.example.mxnexus.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailSender {

    // ── CONFIGURE THESE ──────────────────────────────────────────────────
    // 1. Use a Gmail address as the sender account
    // 2. Enable 2-Factor Authentication on that Gmail account
    // 3. Go to Google Account → Security → App Passwords → generate one
    // 4. Paste the 16-char App Password (no spaces) into SENDER_APP_PASSWORD
    private const val SENDER_EMAIL       = "mxnexus.noreply@gmail.com"   // ← replace
    private const val SENDER_APP_PASSWORD = "chsk udei nzga vivj"   // ← replace
    private const val APP_NAME           = "MXNexus"
    // ─────────────────────────────────────────────────────────────────────

    private fun createSession(): Session {
        val props = Properties().apply {
            put("mail.smtp.auth",            "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host",            "smtp.gmail.com")
            put("mail.smtp.port",            "587")
            put("mail.smtp.ssl.protocols",   "TLSv1.2")
        }
        return Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() =
                PasswordAuthentication(SENDER_EMAIL, SENDER_APP_PASSWORD)
        })
    }

    /** Send approval confirmation email to the alumni. */
    suspend fun sendApprovalEmail(toEmail: String, toName: String) =
        withContext(Dispatchers.IO) {
            val session = createSession()
            val msg = MimeMessage(session).apply {
                setFrom(InternetAddress(SENDER_EMAIL, APP_NAME))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                subject = "🎉 Your $APP_NAME account has been approved!"
                setContent(buildApprovalHtml(toName), "text/html; charset=utf-8")
            }
            Transport.send(msg)
        }

    /** Send rejection email to the alumni. */
    suspend fun sendRejectionEmail(toEmail: String, toName: String) =
        withContext(Dispatchers.IO) {
            val session = createSession()
            val msg = MimeMessage(session).apply {
                setFrom(InternetAddress(SENDER_EMAIL, APP_NAME))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                subject = "Update on your $APP_NAME registration"
                setContent(buildRejectionHtml(toName), "text/html; charset=utf-8")
            }
            Transport.send(msg)
        }

    private fun buildApprovalHtml(name: String) = """
        <!DOCTYPE html>
        <html>
        <body style="font-family:Arial,sans-serif;background:#f5f5f5;margin:0;padding:20px;">
          <div style="max-width:560px;margin:auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
            <div style="background:linear-gradient(135deg,#6A1B9A,#FF00CC);padding:32px;text-align:center;">
              <h1 style="color:#fff;margin:0;font-size:28px;">🎉 Welcome to MX Nexus!</h1>
            </div>
            <div style="padding:32px;">
              <p style="font-size:16px;color:#333;">Hi <strong>$name</strong>,</p>
              <p style="font-size:15px;color:#555;line-height:1.6;">
                Great news! Your Alumni account has been <strong style="color:#6A1B9A;">approved</strong> by our admin team.
                You can now log in to the MX Nexus app and start connecting with students and fellow alumni.
              </p>
              <div style="text-align:center;margin:32px 0;">
                <div style="display:inline-block;background:linear-gradient(135deg,#6A1B9A,#FF00CC);color:#fff;padding:14px 36px;border-radius:24px;font-size:16px;font-weight:bold;">
                  Open MX Nexus App
                </div>
              </div>
              <p style="font-size:13px;color:#aaa;text-align:center;">If you have any questions, contact your institution's administrator.</p>
            </div>
          </div>
        </body>
        </html>
    """.trimIndent()

    private fun buildRejectionHtml(name: String) = """
        <!DOCTYPE html>
        <html>
        <body style="font-family:Arial,sans-serif;background:#f5f5f5;margin:0;padding:20px;">
          <div style="max-width:560px;margin:auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
            <div style="background:#1A1A2E;padding:32px;text-align:center;">
              <h1 style="color:#fff;margin:0;font-size:24px;">MX Nexus — Registration Update</h1>
            </div>
            <div style="padding:32px;">
              <p style="font-size:16px;color:#333;">Hi <strong>$name</strong>,</p>
              <p style="font-size:15px;color:#555;line-height:1.6;">
                We regret to inform you that your Alumni registration could not be approved at this time.
                This may be due to incomplete information or verification issues.
              </p>
              <p style="font-size:15px;color:#555;line-height:1.6;">
                Please reach out to your institution's administrator for more details or to re-apply.
              </p>
            </div>
          </div>
        </body>
        </html>
    """.trimIndent()
}
