package Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.User;
import services.EmailService;
import services.SmsService;
import services.UserService;

/**
 * Contrôleur de la page "Mot de passe oublié" (forgot_password.fxml).
 *
 * Flux :
 *  1. L'utilisateur saisit son email OU son téléphone
 *  2. Il clique sur "Envoyer par email" ou "Envoyer par SMS"
 *  3. La section code OTP apparaît (codeSection)
 *  4. Il saisit le code → handleVerifyCode()
 *  5. La section nouveau mot de passe apparaît (passwordSection)
 *  6. Il confirme → handleResetPassword()
 */
public class ForgotPasswordController {

    // ── Champs de saisie ─────────────────────────────────────────────────────
    @FXML private TextField emailField;
    @FXML private TextField phoneField;

    // ── Section code OTP ─────────────────────────────────────────────────────
    @FXML private VBox   codeSection;
    @FXML private TextField codeField;
    @FXML private Label  codeSentToLabel;

    // ── Section nouveau mot de passe ──────────────────────────────────────────
    @FXML private VBox   passwordSection;
    @FXML private TextField    newPasswordField;
    @FXML private PasswordField newPasswordFieldMasked;
    @FXML private Button toggleNewPwdBtn;

    // ── Feedback général ─────────────────────────────────────────────────────
    @FXML private Label feedbackLabel;

    // ── État interne ─────────────────────────────────────────────────────────
    private String  generatedCode;
    private User    targetUser;
    private boolean showingPassword = false;

    private final UserService userService = new UserService();
    //SmsService.sendResetCode(normalizedPhone, generatedCode);
    //  Appelé depuis LoginController pour pré-remplir l'email
    public void prefillEmail(String email) {
        if (emailField != null && email != null) {
            emailField.setText(email);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ÉTAPE 1A — Envoyer le code par EMAIL
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    private void handleSendByEmail() {
        String email = emailField.getText().trim();
        if (email.isBlank()) {
            showFeedback("Veuillez entrer votre adresse email.", false);
            return;
        }
        try {
            targetUser = userService.findByEmail(email);
            if (targetUser == null) {
                showFeedback("❌ Aucun compte trouvé pour cet email.", false);
                return;
            }
            generatedCode = generateCode();
            EmailService.sendResetCode(email, generatedCode);

            codeSentToLabel.setText("Code envoyé à : " + email);
            showFeedback("✅ Code envoyé à " + email + " — vérifiez votre boîte mail.", true);
            showCodeSection();

        } catch (Exception e) {
            showFeedback("❌ Erreur d'envoi : " + e.getMessage(), false);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ÉTAPE 1B — Envoyer le code par SMS (Twilio)
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    private void handleSendBySms() {
        String phone = phoneField.getText().trim();
        if (phone.isBlank()) {
            showFeedback("Veuillez entrer votre numéro de téléphone.", false);
            return;
        }

        String normalizedPhone = normalizePhone(phone);

        try {
            targetUser = userService.findByPhone(normalizedPhone);
            if (targetUser == null) {
                showFeedback("❌ Aucun compte trouvé pour ce numéro.", false);
                return;
            }
            generatedCode = generateCode();
            SmsService.sendResetCode(normalizedPhone, generatedCode);

            codeSentToLabel.setText("Code envoyé au : " + normalizedPhone);
            showFeedback("✅ SMS envoyé au " + normalizedPhone + " via Twilio.", true);
            showCodeSection();

        } catch (Exception e) {
            showFeedback("❌ Erreur SMS : " + e.getMessage(), false);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ÉTAPE 2 — Vérifier le code OTP
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    private void handleVerifyCode() {
        String entered = codeField.getText().trim();
        if (entered.isBlank()) {
            showFeedback("Entrez le code à 6 chiffres.", false);
            return;
        }
        if (entered.equals(generatedCode)) {
            // ✅ Code correct → naviguer vers la page reset_password
            navigateToResetPassword();
        } else {
            showFeedback("❌ Code incorrect. Vérifiez le code reçu.", false);
            codeField.clear();
            codeField.requestFocus();
        }
    }

    private void navigateToResetPassword() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/reset_password.fxml"));
            javafx.scene.Parent root = loader.load();

            // ✅ Passer l'utilisateur au ResetPasswordController
            ResetPasswordController ctrl = loader.getController();
            ctrl.setTargetUser(targetUser);

            javafx.stage.Stage stage = (javafx.stage.Stage) codeField.getScene().getWindow();
            stage.setWidth(560);
            stage.setHeight(700);
            stage.centerOnScreen();
            codeField.getScene().setRoot(root);
        } catch (Exception e) {
            showFeedback("❌ Erreur navigation : " + e.getMessage(), false);
            e.printStackTrace();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Renvoyer le code
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    private void handleResendCode() {
        // Relaunch the same channel
        if (targetUser == null) return;
        try {
            generatedCode = generateCode();
            boolean byEmail = !emailField.getText().isBlank();
            if (byEmail) {
                EmailService.sendResetCode(emailField.getText().trim(), generatedCode);
            } else {
                SmsService.sendResetCode(normalizePhone(phoneField.getText().trim()), generatedCode);
            }
            showFeedback("✅ Nouveau code envoyé !", true);
            codeField.clear();
        } catch (Exception e) {
            showFeedback("❌ Erreur lors du renvoi : " + e.getMessage(), false);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ÉTAPE 3 — Réinitialiser le mot de passe
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    private void handleResetPassword() {
        String pwd = showingPassword
                ? newPasswordField.getText()
                : newPasswordFieldMasked.getText();
        pwd = pwd.trim();

        if (pwd.length() < 6) {
            showFeedback("❌ Le mot de passe doit contenir au moins 6 caractères.", false);
            return;
        }
        if (!pwd.matches(".*[A-Z].*")) {
            showFeedback("❌ Au moins 1 lettre majuscule requise.", false);
            return;
        }
        if (!pwd.matches(".*\\d.*")) {
            showFeedback("❌ Au moins 1 chiffre requis.", false);
            return;
        }
        try {
            targetUser.setMotDePasse(pwd);
            new services.UserService().update(targetUser);
            showFeedback("✅ Mot de passe réinitialisé ! Redirection...", true);

            // ✅ Retour au login après 1.5s
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(1.5));
            pause.setOnFinished(e -> handleBackToLogin());
            pause.play();

        } catch (Exception e) {
            showFeedback("❌ Erreur : " + e.getMessage(), false);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Toggle affichage mot de passe
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    private void toggleNewPassword() {
        showingPassword = !showingPassword;
        if (showingPassword) {
            newPasswordField.setText(newPasswordFieldMasked.getText());
            newPasswordField.setVisible(true);
            newPasswordField.setManaged(true);
            newPasswordFieldMasked.setVisible(false);
            newPasswordFieldMasked.setManaged(false);
            toggleNewPwdBtn.setText("🙈");
        } else {
            newPasswordFieldMasked.setText(newPasswordField.getText());
            newPasswordFieldMasked.setVisible(true);
            newPasswordFieldMasked.setManaged(true);
            newPasswordField.setVisible(false);
            newPasswordField.setManaged(false);
            toggleNewPwdBtn.setText("👁️");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Retour login
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    public void handleBackToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setWidth(1200);
            stage.setHeight(700);
            stage.centerOnScreen();
            stage.getScene().setRoot(root);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    /** Afficher la section code OTP */
    private void showCodeSection() {
        codeSection.setVisible(true);
        codeSection.setManaged(true);
        codeField.clear();
    }

    /** Générer un code à 6 chiffres */
    private String generateCode() {
        return String.format("%06d", new java.util.Random().nextInt(1_000_000));
    }

    /**
     * Normaliser le numéro de téléphone au format E.164.
     * Tunisie : 8 chiffres → +216XXXXXXXX
     */
    private String normalizePhone(String raw) {
        String digits = raw.replaceAll("[^0-9+]", "");
        // Numéro tunisien local (8 chiffres)
        if (digits.matches("[2-9][0-9]{7}")) {
            return "+216" + digits;
        }
        // Déjà avec indicatif
        if (digits.startsWith("+")) return digits;
        return "+" + digits;
    }

    /** Afficher un message de feedback coloré */
    private void showFeedback(String message, boolean success) {
        feedbackLabel.setText(message);
        feedbackLabel.setVisible(true);
        feedbackLabel.setManaged(true);
        if (success) {
            feedbackLabel.setStyle(
                    "-fx-font-size: 13px; -fx-background-radius: 12px; -fx-border-radius: 12px; " +
                            "-fx-padding: 14 18; -fx-border-width: 1px; " +
                            "-fx-text-fill: #86efac; " +
                            "-fx-background-color: rgba(16,185,129,0.1); " +
                            "-fx-border-color: rgba(16,185,129,0.3);"
            );
        } else {
            feedbackLabel.setStyle(
                    "-fx-font-size: 13px; -fx-background-radius: 12px; -fx-border-radius: 12px; " +
                            "-fx-padding: 14 18; -fx-border-width: 1px; " +
                            "-fx-text-fill: #fca5a5; " +
                            "-fx-background-color: rgba(220,38,38,0.12); " +
                            "-fx-border-color: rgba(220,38,38,0.3);"
            );
        }
    }

}