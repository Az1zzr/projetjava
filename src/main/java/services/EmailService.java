package services;

import java.io.*;
import java.net.Socket;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class EmailService {

    // ══════════════════════════════════════════════════
    // *** CONFIGURER ICI AVEC VOS VRAIES INFORMATIONS ***
    // ══════════════════════════════════════════════════
    private static final String SENDER_EMAIL    = "maramsoltani175@gmail.com"; // ← VOTRE VRAI EMAIL
    private static final String SENDER_PASSWORD = "zvru szvk ogph owrt"; // ← MOT DE PASSE D'APPLICATION
    private static final String SMTP_HOST       = "smtp.gmail.com";
    private static final int    SMTP_PORT       = 587;
    // ══════════════════════════════════════════════════

    public static void sendResetCode(String toEmail, String code) throws Exception {
        // Vérification que l'email est configuré
        if (SENDER_EMAIL.equals("votre.email@gmail.com") || SENDER_PASSWORD.equals("votre-mot-de-passe-app")) {
            System.err.println("⚠️  ATTENTION: EmailService non configuré!");
            System.err.println("📧  Code de test pour " + toEmail + " : " + code);
            return;
        }

        try (Socket socket = new Socket(SMTP_HOST, SMTP_PORT)) {
            socket.setSoTimeout(10000);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            // Lire bannière SMTP
            String response = reader.readLine();
            System.out.println("SMTP: " + response);

            // EHLO
            writer.println("EHLO localhost");
            while ((response = reader.readLine()) != null) {
                System.out.println("EHLO: " + response);
                if (response.startsWith("250 ")) break;
            }

            // STARTTLS
            writer.println("STARTTLS");
            response = reader.readLine();
            System.out.println("STARTTLS: " + response);
            if (!response.startsWith("220")) {
                throw new Exception("STARTTLS failed: " + response);
            }

            // Upgrade socket vers SSL/TLS
            SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslSocket = (SSLSocket) sslFactory.createSocket(
                    socket, SMTP_HOST, SMTP_PORT, true);
            sslSocket.startHandshake();

            BufferedReader sslReader = new BufferedReader(
                    new InputStreamReader(sslSocket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter sslWriter = new PrintWriter(
                    new OutputStreamWriter(sslSocket.getOutputStream(), StandardCharsets.UTF_8), true);

            // EHLO après TLS
            sslWriter.println("EHLO localhost");
            while ((response = sslReader.readLine()) != null) {
                System.out.println("EHLO TLS: " + response);
                if (response.startsWith("250 ")) break;
            }

            // AUTH LOGIN
            sslWriter.println("AUTH LOGIN");
            response = sslReader.readLine();
            System.out.println("AUTH: " + response);

            // Envoyer email encodé en Base64
            sslWriter.println(Base64.getEncoder().encodeToString(SENDER_EMAIL.getBytes(StandardCharsets.UTF_8)));
            response = sslReader.readLine();
            System.out.println("USER: " + response);

            sslWriter.println(Base64.getEncoder().encodeToString(SENDER_PASSWORD.getBytes(StandardCharsets.UTF_8)));
            response = sslReader.readLine();
            System.out.println("PASS: " + response);

            if (!response.startsWith("235")) {
                throw new Exception("Authentification échouée: " + response);
            }

            // MAIL FROM
            sslWriter.println("MAIL FROM:<" + SENDER_EMAIL + ">");
            response = sslReader.readLine();
            System.out.println("MAIL FROM: " + response);

            // RCPT TO
            sslWriter.println("RCPT TO:<" + toEmail + ">");
            response = sslReader.readLine();
            System.out.println("RCPT TO: " + response);

            // DATA
            sslWriter.println("DATA");
            response = sslReader.readLine();
            System.out.println("DATA: " + response);

            // Corps du message
            sslWriter.println("From: LocalTrade <" + SENDER_EMAIL + ">");
            sslWriter.println("To: " + toEmail);
            sslWriter.println("Subject: Code de réinitialisation - LocalTrade");
            sslWriter.println("MIME-Version: 1.0");
            sslWriter.println("Content-Type: text/html; charset=UTF-8");
            sslWriter.println();
            sslWriter.println("<html><body style='font-family:Arial,sans-serif;'>");
            sslWriter.println("<h2 style='color:#f97316;'>LocalTrade</h2>");
            sslWriter.println("<p>Votre code de réinitialisation est :</p>");
            sslWriter.println("<h1 style='color:#1e3a5f; font-size:32px;'>" + code + "</h1>");
            sslWriter.println("<p>Ce code expire dans 10 minutes.</p>");
            sslWriter.println("<p>Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.</p>");
            sslWriter.println("</body></html>");
            sslWriter.println(".");

            response = sslReader.readLine();
            System.out.println("FINISH: " + response);

            // QUIT
            sslWriter.println("QUIT");
            sslSocket.close();

            System.out.println("✅ Email envoyé avec succès à " + toEmail);

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}