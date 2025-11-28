package com.example.gestioneventosdsm.util

import android.os.StrictMode
import java.util.Properties
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class sendMail {
    fun enviarEmail(destinatario: String, asunto: String, mensaje: String) {
        // Permite operaciones de red en el hilo principal (no recomendado para producción)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val correo = "rnld.renderos24@gmail.com"
        val contraseña = "jciz oirf iyzx zkyu"

        val propiedades = Properties().apply {
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.socketFactory.port", "465")
            put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
            put("mail.smtp.auth", "true")
            put("mail.smtp.port", "465")
        }

        val sesion = Session.getInstance(propiedades, object : javax.mail.Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(correo, contraseña)
            }
        })

        try {
            val mensaje = MimeMessage(sesion).apply {
                setFrom(InternetAddress(correo))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario))
                subject = asunto
                setText(mensaje)
            }

            Transport.send(mensaje)
            println("Email enviado correctamente")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error al enviar el email: ${e.message}")
        }
    }
}