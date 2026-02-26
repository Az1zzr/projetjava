package Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;
import models.User;
import services.UserService;
import services.EmailService;
import services.SmsService;
import utils.SessionManager;

import java.io.IOException;
import javafx.application.Platform;
import java.util.regex.Pattern;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField     visiblePasswordField;
    @FXML private Button        togglePasswordButton;
    @FXML private Label         errorLabel;
    private final UserService userService = new UserService();
    private boolean pwdVisible = false;

    // ══════════════════════════════════════════════════
    // ✅ ADMIN HARDCODÉ — modifier ici si besoin
    // ══════════════════════════════════════════════════
    private static final String ADMIN_EMAIL    = "admin@localtrade.com";
    private static final String ADMIN_PASSWORD = "Admis200";
    // ══════════════════════════════════════════════════

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE =
            Pattern.compile("^(\\+\\d{1,3}[- ]?)?\\d{10}$");

    @FXML
    public void initialize() {
        passwordField.textProperty().addListener((o, ov, nv) -> visiblePasswordField.setText(nv));
        visiblePasswordField.textProperty().addListener((o, ov, nv) -> passwordField.setText(nv));
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        emailField.textProperty().addListener((o, ov, nv) -> {
            if (nv.isEmpty()) resetStyle(emailField);
            else if (EMAIL.matcher(nv.trim()).matches()) setValid(emailField);
            else setInvalid(emailField);
        });
        passwordField.setOnAction(e -> handleLogin());
        visiblePasswordField.setOnAction(e -> handleLogin());
    }

    @FXML private void togglePasswordVisibility() {
        pwdVisible = !pwdVisible;
        visiblePasswordField.setVisible(pwdVisible);  visiblePasswordField.setManaged(pwdVisible);
        passwordField.setVisible(!pwdVisible);         passwordField.setManaged(!pwdVisible);
        togglePasswordButton.setText(pwdVisible ? "🙈" : "👁️");
    }

    @FXML private void handleLogin() {
        hideError();
        String email = emailField.getText().trim().toLowerCase();
        String pwd   = pwdVisible ? visiblePasswordField.getText() : passwordField.getText();

        if (!EMAIL.matcher(email).matches()) { setInvalid(emailField); showError("Email invalide."); return; }
        if (pwd.isEmpty()) { setInvalid(passwordField); showError("Mot de passe requis."); return; }

        try {
            User user = null;

            // ✅ Vérification admin hardcodé EN PREMIER
            if (email.equals(ADMIN_EMAIL) && pwd.equals(ADMIN_PASSWORD)) {
                // Créer un User admin fictif sans passer par la BD
                models.Role adminRole = new models.Role();
                adminRole.setId_role(1);
                adminRole.setNomRole("admin");

                user = new User();
                user.setId(0);
                user.setNom("Admin");
                user.setPrenom("LocalTrade");
                user.setEmail(ADMIN_EMAIL);
                user.setMotDePasse(ADMIN_PASSWORD);
                user.setRole(adminRole);
            } else {
                // Authentification normale via BD pour les autres utilisateurs
                user = userService.authenticate(email, pwd);
            }

            if (user == null) {
                showError("Email ou mot de passe incorrect.");
                setInvalid(emailField); setInvalid(passwordField); setInvalid(visiblePasswordField);
                return;
            }

            // ✅ Session AVANT loader.load() — OBLIGATOIRE
            SessionManager.getInstance().setCurrentUser(user);

            // ✅ Résolution layout — utilise SessionManager qui gère fournisseur/fournisseurs
            String layout = SessionManager.getInstance().isRegularUser()
                    ? "/UserDashboard.fxml"
                    : "/MainLayout.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(layout));
            Parent root = loader.load();

            Object ctrl = loader.getController();
            if (ctrl instanceof MainLayoutController mlc)
                mlc.setCurrentUser(user);
            else if (ctrl instanceof UserDashboardController udc)
                udc.setCurrentUser(user);

            var scene = emailField.getScene();
            scene.getWindow().setWidth(1280);
            scene.getWindow().setHeight(780);
            ((javafx.stage.Stage) scene.getWindow()).centerOnScreen();
            scene.setRoot(root);

        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML private void handleGoToRegister() {
        try {
            emailField.getScene().setRoot(
                    new FXMLLoader(getClass().getResource("/register.fxml")).load()
            );
        } catch (IOException e) { showError("Impossible de charger la page."); }
    }

    private void showError(String msg)  { errorLabel.setText("⚠️  " + msg); errorLabel.setVisible(true); errorLabel.setManaged(true); }
    private void hideError()            { errorLabel.setVisible(false); errorLabel.setManaged(false); }
    private void setValid(Control f)    { f.setStyle("-fx-border-color: #10b981; -fx-border-width: 2px;"); }
    private void setInvalid(Control f)  { f.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px;"); }
    private void resetStyle(Control f)  { f.setStyle(""); }

    // ══════════════════════════════════════════════════════════════════════════
    // MOT DE PASSE OUBLIÉ — Validation email obligatoire + Navigation
    // ══════════════════════════════════════════════════════════════════════════
    @FXML private void handleForgotPassword() {
        String email = emailField.getText().trim();

        // ✅ Email obligatoire avant navigation
        if (email.isEmpty()) {
            setInvalid(emailField);
            showError("Veuillez entrer votre email avant de continuer.");
            return;
        }
        if (!EMAIL.matcher(email).matches()) {
            setInvalid(emailField);
            showError("Email invalide. Ex: user@gmail.com");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/forgot_password.fxml"));
            Parent root = loader.load();

            // ✅ Pré-remplir l'email dans ForgotPasswordController
            ForgotPasswordController ctrl = loader.getController();
            ctrl.prefillEmail(email);

            var scene = emailField.getScene();
            scene.getWindow().setWidth(600);
            scene.getWindow().setHeight(780);
            ((Stage) scene.getWindow()).centerOnScreen();
            scene.setRoot(root);
        } catch (Exception e) {
            showError("Impossible de charger la page : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showRecoveryChoiceDialog() {
        Dialog<String> choiceDialog = new Dialog<>();
        choiceDialog.setTitle("Récupération de compte");
        choiceDialog.setHeaderText(null);

        DialogPane pane = choiceDialog.getDialogPane();
        pane.setStyle("-fx-background-color: #0d1b2e; -fx-font-size: 13px; -fx-pref-width: 400px;");

        VBox content = new VBox(20);
        content.setStyle("-fx-padding: 24 24 10 24;");

        Label title = new Label("🔐  Comment souhaitez-vous récupérer votre compte ?");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");
        title.setWrapText(true);

        // Bouton Email
        Button emailBtn = new Button("📧  Par Email");
        emailBtn.setStyle("-fx-background-color: #1e3a5f; -fx-text-fill: white; -fx-font-size: 14px; " +
                "-fx-font-weight: bold; -fx-background-radius: 12px; -fx-padding: 14 20; " +
                "-fx-cursor: hand; -fx-pref-width: 300px;");
        emailBtn.setOnAction(e -> {
            choiceDialog.close();
            showEmailInputDialog();
        });

        // Bouton SMS
        Button smsBtn = new Button("📱  Par SMS");
        smsBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 14px; " +
                "-fx-font-weight: bold; -fx-background-radius: 12px; -fx-padding: 14 20; " +
                "-fx-cursor: hand; -fx-pref-width: 300px;");
        smsBtn.setOnAction(e -> {
            choiceDialog.close();
            showPhoneInputDialog();
        });

        // Bouton Annuler
        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4a6a8a; -fx-font-size: 13px; " +
                "-fx-cursor: hand; -fx-underline: true;");
        cancelBtn.setOnAction(e -> choiceDialog.close());

        content.getChildren().addAll(title, emailBtn, smsBtn, cancelBtn);
        pane.setContent(content);

        choiceDialog.showAndWait();
    }

    private void showEmailInputDialog() {
        Dialog<String> emailDialog = new Dialog<>();
        emailDialog.setTitle("Récupération par Email");
        emailDialog.setHeaderText(null);

        DialogPane pane1 = emailDialog.getDialogPane();
        pane1.setStyle("-fx-background-color: #0d1b2e; -fx-font-size: 13px;");

        ButtonType nextBtn = new ButtonType("Vérifier →", ButtonBar.ButtonData.OK_DONE);
        pane1.getButtonTypes().addAll(nextBtn, ButtonType.CANCEL);

        javafx.scene.Node okNode = pane1.lookupButton(nextBtn);
        okNode.setStyle("-fx-background-color: #f97316; -fx-text-fill: white;" +
                "-fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 8 20;");

        VBox content1 = new VBox(16);
        content1.setStyle("-fx-padding: 20 24 10 24;");

        Label title1 = new Label("📧  Réinitialisation par Email");
        title1.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label sub1 = new Label("Entrez votre adresse email pour recevoir un code de réinitialisation.");
        sub1.setWrapText(true);
        sub1.setStyle("-fx-font-size: 12px; -fx-text-fill: #4a6a8a;");

        TextField tfEmail = new TextField();
        tfEmail.setPromptText("vous@gmail.com");
        tfEmail.setStyle("-fx-pref-height: 44px; -fx-background-color: #0a1628;" +
                "-fx-border-color: #1a2d47; -fx-border-width: 1.5px;" +
                "-fx-border-radius: 10px; -fx-background-radius: 10px;" +
                "-fx-font-size: 13px; -fx-text-fill: white; -fx-prompt-text-fill: #1e3050;" +
                "-fx-padding: 10 14;");

        content1.getChildren().addAll(title1, sub1, tfEmail);
        pane1.setContent(content1);

        emailDialog.setResultConverter(btn -> btn == nextBtn ? tfEmail.getText().trim() : null);
        java.util.Optional<String> emailResult = emailDialog.showAndWait();
        if (emailResult.isEmpty() || emailResult.get().isBlank()) return;

        String email = emailResult.get().toLowerCase();
        if (!EMAIL.matcher(email).matches()) {
            showError("Email invalide.");
            return;
        }

        processEmailRecovery(email);
    }

    private void showPhoneInputDialog() {
        Dialog<String> phoneDialog = new Dialog<>();
        phoneDialog.setTitle("Récupération par SMS");
        phoneDialog.setHeaderText(null);

        DialogPane pane = phoneDialog.getDialogPane();
        pane.setStyle("-fx-background-color: #0d1b2e; -fx-font-size: 13px;");

        ButtonType nextBtn = new ButtonType("Envoyer le code →", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(nextBtn, ButtonType.CANCEL);

        javafx.scene.Node okNode = pane.lookupButton(nextBtn);
        okNode.setStyle("-fx-background-color: #10b981; -fx-text-fill: white;" +
                "-fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 8 20;");

        VBox content = new VBox(16);
        content.setStyle("-fx-padding: 20 24 10 24;");

        Label title = new Label("📱  Réinitialisation par SMS");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label sub = new Label("Entrez votre numéro de téléphone pour recevoir un code par SMS.");
        sub.setWrapText(true);
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #4a6a8a;");

        TextField tfPhone = new TextField();
        tfPhone.setPromptText("+33612345678 ou 0612345678");
        tfPhone.setStyle("-fx-pref-height: 44px; -fx-background-color: #0a1628;" +
                "-fx-border-color: #1a2d47; -fx-border-width: 1.5px;" +
                "-fx-border-radius: 10px; -fx-background-radius: 10px;" +
                "-fx-font-size: 13px; -fx-text-fill: white; -fx-prompt-text-fill: #1e3050;" +
                "-fx-padding: 10 14;");

        Label hint = new Label("Format international recommandé : +33XXXXXXXXX");
        hint.setStyle("-fx-font-size: 10px; -fx-text-fill: #4a6a8a;");

        content.getChildren().addAll(title, sub, tfPhone, hint);
        pane.setContent(content);

        phoneDialog.setResultConverter(btn -> btn == nextBtn ? tfPhone.getText().trim() : null);
        java.util.Optional<String> phoneResult = phoneDialog.showAndWait();
        if (phoneResult.isEmpty() || phoneResult.get().isBlank()) return;

        String phone = phoneResult.get();
        processSmsRecovery(phone);
    }

    private void processEmailRecovery(String email) {
        try {
            User userFound = userService.findByEmail(email);

            if (userFound == null) {
                showError("Aucun compte associé à cet email.");
                return;
            }

            String codeGenere = String.format("%06d", (int)(Math.random() * 1000000));

            // Afficher le code dans la console pour le développement
            System.out.println("📧 CODE DE TEST pour " + email + " : " + codeGenere);

            // Tentative d'envoi par email
            sendCodeByEmail(userFound, codeGenere);

        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processSmsRecovery(String phone) {
        try {

            String cleanPhone = phone.replaceAll("[^0-9+]", "");

            User userFound = userService.findByPhone(cleanPhone);

            if (userFound == null) {
                showError("Aucun compte associé à ce numéro de téléphone.");
                return;
            }

            String codeGenere = String.format("%06d", (int)(Math.random() * 1000000));

            System.out.println("📱 CODE SMS pour " + cleanPhone + " : " + codeGenere);

            // 🔥 ENVOI SMS RÉEL
            sendCodeBySms(userFound, cleanPhone, codeGenere);

        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private User findUserByPhone(String phone) throws Exception {
        // À implémenter dans UserService
        // Pour l'instant, on simule une recherche
        return userService.getAll().stream()
                .filter(u -> u.getTelephone() != null &&
                        u.getTelephone().replaceAll("[^0-9+]", "")
                                .equals(phone.replaceAll("[^0-9+]", "")))
                .findFirst()
                .orElse(null);
    }

    private void sendCodeByEmail(User user, String codeGenere) {
        // Mode développement - afficher simplement le code
        Platform.runLater(() -> {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("🔐 Mode Développement");
            info.setHeaderText("Code de réinitialisation");
            info.setContentText(
                    "Email: " + user.getEmail() + "\n\n" +
                            "Code: " + codeGenere + "\n\n" +
                            "(Configurez EmailService pour l'envoi réel)"
            );
            info.showAndWait();
            showCodeInputDialog(user, codeGenere);
        });
    }

    private void sendCodeBySms(User user, String phone, String codeGenere) {

        Alert sending = new Alert(Alert.AlertType.INFORMATION);
        sending.setTitle("Envoi du SMS...");
        sending.setHeaderText(null);
        sending.setContentText("📱  Envoi du code par SMS à " + phone + "...");
        sending.getButtonTypes().clear();

        Thread smsThread = new Thread(() -> {
            try {

                SmsService.sendResetCode(phone, codeGenere);

                Platform.runLater(() -> {
                    sending.close();
                    showSuccessDialog("SMS envoyé !",
                            "✅ Code envoyé à : " + phone);
                    showCodeInputDialog(user, codeGenere);
                });

            } catch (Exception ex) {

                Platform.runLater(() -> {
                    sending.close();
                    showErrorDialog("Erreur SMS",
                            "❌ Impossible d'envoyer le SMS.\n\n" +
                                    "Vérifiez Twilio SID / TOKEN.\n\n" +
                                    "Code de test : " + codeGenere);

                    showCodeInputDialog(user, codeGenere);
                });

                ex.printStackTrace();
            }
        });

        smsThread.setDaemon(true);
        smsThread.start();
        sending.showAndWait();
    }

    private void showCodeInputDialog(User user, String codeGenere) {
        Dialog<String> codeDialog = new Dialog<>();
        codeDialog.setTitle("Vérification");
        codeDialog.setHeaderText(null);

        DialogPane pane2 = codeDialog.getDialogPane();
        pane2.setStyle("-fx-background-color: #0d1b2e; -fx-font-size: 13px;");

        ButtonType verifyBtn = new ButtonType("Vérifier ✓", ButtonBar.ButtonData.OK_DONE);
        pane2.getButtonTypes().addAll(verifyBtn, ButtonType.CANCEL);
        pane2.lookupButton(verifyBtn).setStyle(
                "-fx-background-color: #f97316; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 8 20;");

        VBox content2 = new VBox(14);
        content2.setStyle("-fx-padding: 20 24 10 24;");

        Label title2 = new Label("📩  Entrez le code reçu");
        title2.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label sub2 = new Label("Code envoyé à : " + (user.getEmail() != null ? user.getEmail() : "votre téléphone"));
        sub2.setStyle("-fx-font-size: 12px; -fx-text-fill: #4a6a8a;");

        // 6 champs séparés pour le code
        HBox codeBoxes = new HBox(8);
        codeBoxes.setAlignment(Pos.CENTER);
        String digitStyle = "-fx-pref-width:48px; -fx-pref-height:56px;" +
                "-fx-background-color:#0a1628; -fx-border-color:#1a2d47;" +
                "-fx-border-width:2px; -fx-border-radius:10px; -fx-background-radius:10px;" +
                "-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:white;" +
                "-fx-alignment:center;";
        TextField[] digits = new TextField[6];
        for (int i = 0; i < 6; i++) {
            digits[i] = new TextField();
            digits[i].setStyle(digitStyle);
            digits[i].setTextFormatter(new TextFormatter<>(change ->
                    change.getControlNewText().length() <= 1 ? change : null));
            final int idx = i;
            digits[i].textProperty().addListener((o, ov, nv) -> {
                if (nv.length() > 1) digits[idx].setText(nv.substring(nv.length()-1));
                if (!nv.isEmpty() && idx < 5) digits[idx + 1].requestFocus();
                digits[idx].setStyle(digitStyle + (nv.isEmpty() ? "" :
                        "-fx-border-color:#f97316;"));
            });
            codeBoxes.getChildren().add(digits[i]);
        }

        Label codeErr = new Label("");
        codeErr.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");

        // Renvoi code
        HBox resendBox = new HBox(10);
        resendBox.setAlignment(Pos.CENTER);
        Button resendBtn = new Button("Renvoyer le code");
        resendBtn.setStyle("-fx-background-color: #1e3a5f; -fx-text-fill: white;" +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 8px;" +
                "-fx-padding: 8 16; -fx-cursor: hand;");

        content2.getChildren().addAll(title2, sub2, codeBoxes, codeErr, resendBox);
        pane2.setContent(content2);

        codeDialog.setResultConverter(btn -> {
            if (btn == verifyBtn) {
                StringBuilder sb = new StringBuilder();
                for (TextField d : digits) sb.append(d.getText().trim());
                return sb.toString();
            }
            return null;
        });

        resendBtn.setOnAction(e -> {
            try {
                String phone = user.getTelephone();
                if (phone != null && !phone.isEmpty()) {
                    sendCodeBySms(user, phone, codeGenere);
                } else {
                    sendCodeByEmail(user, codeGenere);
                }
            } catch (Exception ex) {
                codeErr.setText("❌ Erreur : " + ex.getMessage());
            }
            codeDialog.close();
        });

        java.util.Optional<String> codeResult = codeDialog.showAndWait();
        if (codeResult.isEmpty() || codeResult.get().isBlank()) return;

        if (!codeResult.get().equals(codeGenere)) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Code incorrect");
            err.setHeaderText(null);
            err.setContentText("❌ Le code saisi est incorrect.");
            err.getDialogPane().setStyle("-fx-font-size: 13px;");
            err.showAndWait();
            return;
        }

        showNewPasswordDialog(user);
    }

    private void showNewPasswordDialog(User user) {
        Dialog<String> pwdDialog = new Dialog<>();
        pwdDialog.setTitle("Nouveau mot de passe");
        pwdDialog.setHeaderText(null);

        DialogPane pane3 = pwdDialog.getDialogPane();
        pane3.setStyle("-fx-background-color: #0d1b2e; -fx-font-size: 13px;");

        ButtonType saveBtn = new ButtonType("💾 Enregistrer", ButtonBar.ButtonData.OK_DONE);
        pane3.getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        pane3.lookupButton(saveBtn).setStyle(
                "-fx-background-color: #10b981; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 8 20;");

        String fieldStyle = "-fx-pref-height: 44px; -fx-background-color: #0a1628;" +
                "-fx-border-color: #1a2d47; -fx-border-width: 1.5px;" +
                "-fx-border-radius: 10px; -fx-background-radius: 10px;" +
                "-fx-font-size: 13px; -fx-text-fill: white; -fx-prompt-text-fill: #1e3050;" +
                "-fx-padding: 10 14;";

        VBox content3 = new VBox(14);
        content3.setStyle("-fx-padding: 20 24 10 24;");

        Label title3 = new Label("🔑  Créez votre nouveau mot de passe");
        title3.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label rules = new Label("• Minimum 8 caractères  • 1 majuscule  • 1 chiffre");
        rules.setStyle("-fx-font-size: 11px; -fx-text-fill: #4a6a8a;");

        PasswordField tfNew = new PasswordField();
        tfNew.setPromptText("Nouveau mot de passe");
        tfNew.setStyle(fieldStyle);

        PasswordField tfConfirm = new PasswordField();
        tfConfirm.setPromptText("Confirmer le mot de passe");
        tfConfirm.setStyle(fieldStyle);

        Label pwdErr = new Label("");
        pwdErr.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");

        content3.getChildren().addAll(title3, rules, tfNew, tfConfirm, pwdErr);
        pane3.setContent(content3);

        pwdDialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                String pwd = tfNew.getText();
                String conf = tfConfirm.getText();
                boolean pwdHasUpper = pwd.matches(".*[A-Z].*");
                boolean pwdHasDigit = pwd.matches(".*\\d.*");
                if (pwd.length() < 8 || !pwdHasUpper || !pwdHasDigit) {
                    pwdErr.setText("⚠ Le mot de passe ne respecte pas les règles.");
                    return null;
                }
                if (!pwd.equals(conf)) {
                    pwdErr.setText("⚠ Les mots de passe ne correspondent pas.");
                    return null;
                }
                return pwd;
            }
            return null;
        });

        pwdDialog.showAndWait().ifPresent(newPwd -> {
            try {
                user.setMotDePasse(newPwd);
                userService.update(user);
                showSuccessDialog("Succès !",
                        "✅ Mot de passe mis à jour avec succès !\nVous pouvez maintenant vous connecter.");
            } catch (Exception e) {
                showError("Erreur lors de la mise à jour : " + e.getMessage());
            }
        });
    }

    private void showSuccessDialog(String title, String content) {
        Alert ok = new Alert(Alert.AlertType.INFORMATION);
        ok.setTitle(title);
        ok.setHeaderText(null);
        ok.setContentText(content);
        ok.getDialogPane().setStyle("-fx-font-size: 13px;");
        ok.showAndWait();
    }

    private void showErrorDialog(String title, String content) {
        Alert err = new Alert(Alert.AlertType.ERROR);
        err.setTitle(title);
        err.setHeaderText(null);
        err.setContentText(content);
        err.getDialogPane().setStyle("-fx-font-size: 13px;");
        err.showAndWait();
    }

}