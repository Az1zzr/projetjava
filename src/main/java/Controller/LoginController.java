package Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import models.User;
import services.UserService;
import utils.SessionManager;

import java.io.IOException;
import java.util.regex.Pattern;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField     visiblePasswordField;
    @FXML private Button        togglePasswordButton;
    @FXML private Label         errorLabel;

    private final UserService userService = new UserService();
    private boolean pwdVisible = false;

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

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
            User user = userService.authenticate(email, pwd);
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
}