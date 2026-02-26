package Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.User;
import services.UserService;

public class ResetPasswordController {

    @FXML private PasswordField passwordField;
    @FXML private TextField     visiblePasswordField;
    @FXML private Button        togglePwdBtn;

    @FXML private PasswordField confirmField;
    @FXML private TextField     visibleConfirmField;
    @FXML private Button        toggleConfirmBtn;

    @FXML private Label feedbackLabel;

    private boolean pwdVisible     = false;
    private boolean confirmVisible = false;

    private User targetUser;
    private final UserService userService = new UserService();

    // ✅ Appelé depuis ForgotPasswordController après vérification du code
    public void setTargetUser(User user) {
        this.targetUser = user;
    }

    @FXML
    public void initialize() {
        // Sync password fields
        passwordField.textProperty().addListener((o, ov, nv) -> visiblePasswordField.setText(nv));
        visiblePasswordField.textProperty().addListener((o, ov, nv) -> passwordField.setText(nv));
        confirmField.textProperty().addListener((o, ov, nv) -> visibleConfirmField.setText(nv));
        visibleConfirmField.textProperty().addListener((o, ov, nv) -> confirmField.setText(nv));

        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);
    }

    @FXML private void togglePassword() {
        pwdVisible = !pwdVisible;
        passwordField.setVisible(!pwdVisible);        passwordField.setManaged(!pwdVisible);
        visiblePasswordField.setVisible(pwdVisible);  visiblePasswordField.setManaged(pwdVisible);
        togglePwdBtn.setText(pwdVisible ? "🙈" : "👁️");
    }

    @FXML private void toggleConfirm() {
        confirmVisible = !confirmVisible;
        confirmField.setVisible(!confirmVisible);        confirmField.setManaged(!confirmVisible);
        visibleConfirmField.setVisible(confirmVisible);  visibleConfirmField.setManaged(confirmVisible);
        toggleConfirmBtn.setText(confirmVisible ? "🙈" : "👁️");
    }

    @FXML private void handleResetPassword() {
        String pwd     = pwdVisible     ? visiblePasswordField.getText() : passwordField.getText();
        String confirm = confirmVisible ? visibleConfirmField.getText()  : confirmField.getText();

        // ── Validation ──
        if (pwd.length() < 8) {
            showFeedback("❌ Minimum 8 caractères requis.", false); return;
        }
        if (!pwd.matches(".*[A-Z].*")) {
            showFeedback("❌ Au moins 1 lettre majuscule requise.", false); return;
        }
        if (!pwd.matches(".*\\d.*")) {
            showFeedback("❌ Au moins 1 chiffre requis.", false); return;
        }
        if (!pwd.equals(confirm)) {
            showFeedback("❌ Les mots de passe ne correspondent pas.", false); return;
        }
        if (targetUser == null) {
            showFeedback("❌ Session expirée. Recommencez.", false); return;
        }

        try {
            targetUser.setMotDePasse(pwd);
            userService.update(targetUser);
            showFeedback("✅ Mot de passe réinitialisé avec succès !", true);

            // Retour login après 1.5s
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(1.5));
            pause.setOnFinished(e -> handleBackToLogin());
            pause.play();

        } catch (Exception e) {
            showFeedback("❌ Erreur : " + e.getMessage(), false);
        }
    }

    @FXML public void handleBackToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) passwordField.getScene().getWindow();
            stage.setWidth(1280);
            stage.setHeight(780);
            stage.centerOnScreen();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showFeedback(String message, boolean success) {
        feedbackLabel.setText(message);
        feedbackLabel.setVisible(true);
        feedbackLabel.setManaged(true);
        if (success) {
            feedbackLabel.setStyle(
                    "-fx-font-size: 12px; -fx-background-radius: 10px; -fx-border-radius: 10px; " +
                            "-fx-padding: 12 16; -fx-border-width: 1px; " +
                            "-fx-text-fill: #166534; " +
                            "-fx-background-color: #f0fdf4; " +
                            "-fx-border-color: #bbf7d0;");
        } else {
            feedbackLabel.setStyle(
                    "-fx-font-size: 12px; -fx-background-radius: 10px; -fx-border-radius: 10px; " +
                            "-fx-padding: 12 16; -fx-border-width: 1px; " +
                            "-fx-text-fill: #dc2626; " +
                            "-fx-background-color: #fef2f2; " +
                            "-fx-border-color: #fecaca;");
        }
    }
}
