package Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import models.Role;
import models.User;
import services.RoleService;
import services.UserService;
import utils.SessionManager;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

public class RegisterController {

    @FXML private TextField       nomField;
    @FXML private TextField       prenomField;
    @FXML private TextField       emailField;
    @FXML private PasswordField   passwordField;
    @FXML private TextField       visiblePasswordField;
    @FXML private Button          togglePasswordButton;
    @FXML private PasswordField   confirmPasswordField;
    @FXML private TextField       visibleConfirmField;
    @FXML private Button          toggleConfirmButton;
    @FXML private DatePicker      dateNaissancePicker;
    @FXML private ChoiceBox<Role> roleChoice;
    @FXML private Label           errorLabel;
    @FXML private Label           successLabel;

    private final UserService userService = new UserService();
    private final RoleService roleService = new RoleService();

    private boolean isPwdVisible     = false;
    private boolean isConfirmVisible = false;

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @FXML
    public void initialize() {
        // Charger Fournisseur + Entrepreneur uniquement
        try {
            List<Role> roles = roleService.getAll().stream()
                    .filter(r -> !r.getNomRole().equalsIgnoreCase("Administrateur"))
                    .toList();
            roleChoice.getItems().addAll(roles);
            if (!roles.isEmpty()) roleChoice.setValue(roles.get(0));
        } catch (Exception e) { e.printStackTrace(); }

        // DatePicker : min 16 ans
        dateNaissancePicker.setDayCellFactory(picker -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now().minusYears(16)));
            }
        });

        // Sync mots de passe
        passwordField.textProperty().addListener((o, ov, nv) -> visiblePasswordField.setText(nv));
        visiblePasswordField.textProperty().addListener((o, ov, nv) -> passwordField.setText(nv));
        confirmPasswordField.textProperty().addListener((o, ov, nv) -> visibleConfirmField.setText(nv));
        visibleConfirmField.textProperty().addListener((o, ov, nv) -> confirmPasswordField.setText(nv));

        errorLabel.setVisible(false);   errorLabel.setManaged(false);
        successLabel.setVisible(false); successLabel.setManaged(false);

        // Validation temps réel
        nomField.textProperty().addListener((o, ov, nv) -> {
            if (!nv.isEmpty()) { if (nv.trim().length() >= 2) setValid(nomField); else setInvalid(nomField); }
            else resetStyle(nomField);
        });
        prenomField.textProperty().addListener((o, ov, nv) -> {
            if (!nv.isEmpty()) { if (nv.trim().length() >= 2) setValid(prenomField); else setInvalid(prenomField); }
            else resetStyle(prenomField);
        });
        emailField.textProperty().addListener((o, ov, nv) -> {
            if (!nv.isEmpty()) {
                if (EMAIL.matcher(nv.trim()).matches()) setValid(emailField);
                else setInvalid(emailField);
            } else resetStyle(emailField);
        });
        passwordField.textProperty().addListener((o, ov, nv) -> {
            boolean ok = nv.length() >= 8 && nv.matches(".*[A-Z].*") && nv.matches(".*\\d.*");
            if (!nv.isEmpty()) {
                if (ok) { setValid(passwordField); setValid(visiblePasswordField); }
                else    { setInvalid(passwordField); setInvalid(visiblePasswordField); }
            } else { resetStyle(passwordField); resetStyle(visiblePasswordField); }
        });
    }

    @FXML private void togglePassword() {
        isPwdVisible = !isPwdVisible;
        passwordField.setVisible(!isPwdVisible);       passwordField.setManaged(!isPwdVisible);
        visiblePasswordField.setVisible(isPwdVisible); visiblePasswordField.setManaged(isPwdVisible);
        togglePasswordButton.setText(isPwdVisible ? "🙈" : "👁️");
    }

    @FXML private void toggleConfirmPassword() {
        isConfirmVisible = !isConfirmVisible;
        confirmPasswordField.setVisible(!isConfirmVisible); confirmPasswordField.setManaged(!isConfirmVisible);
        visibleConfirmField.setVisible(isConfirmVisible);   visibleConfirmField.setManaged(isConfirmVisible);
        toggleConfirmButton.setText(isConfirmVisible ? "🙈" : "👁️");
    }

    @FXML private void handleRegister() {
        hideMessages();

        String nom     = nomField.getText().trim();
        String prenom  = prenomField.getText().trim();
        String email   = emailField.getText().trim().toLowerCase();
        String pwd     = isPwdVisible ? visiblePasswordField.getText() : passwordField.getText();
        String confirm = isConfirmVisible ? visibleConfirmField.getText() : confirmPasswordField.getText();
        LocalDate ddn  = dateNaissancePicker.getValue();
        Role role      = roleChoice.getValue();

        StringBuilder errors = new StringBuilder();

        if (nom.length() < 2)    { setInvalid(nomField);    errors.append("• Nom : minimum 2 caractères.\n"); }    else setValid(nomField);
        if (prenom.length() < 2) { setInvalid(prenomField); errors.append("• Prénom : minimum 2 caractères.\n"); } else setValid(prenomField);

        if (email.isEmpty()) {
            setInvalid(emailField); errors.append("• Email obligatoire.\n");
        } else if (!EMAIL.matcher(email).matches()) {
            setInvalid(emailField); errors.append("• Email invalide (ex: user@gmail.com).\n");
        } else if (emailExists(email)) {
            setInvalid(emailField); errors.append("• Email déjà utilisé.\n");
        } else setValid(emailField);

        if (pwd.isEmpty()) {
            setInvalid(passwordField); setInvalid(visiblePasswordField); errors.append("• Mot de passe obligatoire.\n");
        } else if (pwd.length() < 8) {
            setInvalid(passwordField); setInvalid(visiblePasswordField); errors.append("• Minimum 8 caractères.\n");
        } else if (!pwd.matches(".*[A-Z].*")) {
            setInvalid(passwordField); setInvalid(visiblePasswordField); errors.append("• Au moins 1 majuscule requise.\n");
        } else if (!pwd.matches(".*\\d.*")) {
            setInvalid(passwordField); setInvalid(visiblePasswordField); errors.append("• Au moins 1 chiffre requis.\n");
        } else { setValid(passwordField); setValid(visiblePasswordField); }

        if (!pwd.equals(confirm)) {
            setInvalid(confirmPasswordField); setInvalid(visibleConfirmField);
            errors.append("• Les mots de passe ne correspondent pas.\n");
        } else if (!confirm.isEmpty()) {
            setValid(confirmPasswordField); setValid(visibleConfirmField);
        }

        if (ddn == null) {
            errors.append("• Date de naissance obligatoire.\n");
        } else if (ddn.isAfter(LocalDate.now().minusYears(16))) {
            errors.append("• Vous devez avoir au moins 16 ans.\n");
        }

        if (role == null) errors.append("• Veuillez sélectionner un rôle.\n");

        if (errors.length() > 0) { showError(errors.toString().trim()); return; }

        try {
            User u = new User();
            u.setNom(nom);
            u.setPrenom(prenom);
            u.setEmail(email);
            u.setMotDePasse(pwd);
            u.setDateNaissance(ddn);
            u.setRole(role);
            userService.add(u);

            // ✅ Sauvegarder en session
            SessionManager.getInstance().setCurrentUser(u);

            // ✅ Redirect automatique vers la bonne page selon le rôle
            redirectAfterRegister(u);

        } catch (Exception e) {
            showError("❌ " + e.getMessage());
        }
    }

    /** Redirige vers le bon layout immédiatement après l'inscription */
    private void redirectAfterRegister(User user) {
        try {
            // ✅ Utilise SessionManager qui gère "fournisseur" ET "fournisseurs"
            String fxmlPath = SessionManager.getInstance().isRegularUser()
                    ? "/UserDashboard.fxml"
                    : "/MainLayout.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Passer l'utilisateur au controller
            Object ctrl = loader.getController();
            if (ctrl instanceof Controller.MainLayoutController mlc)       mlc.setCurrentUser(user);
            else if (ctrl instanceof Controller.UserDashboardController udc) udc.setCurrentUser(user);

            var scene = emailField.getScene();
            scene.getWindow().setWidth(1280);
            scene.getWindow().setHeight(780);
            ((javafx.stage.Stage) scene.getWindow()).centerOnScreen();
            scene.setRoot(root);

        } catch (IOException e) {
            // Si la redirection échoue, afficher succès et laisser se connecter manuellement
            showSuccess("✅ Compte créé ! Cliquez ici pour vous connecter.");
            e.printStackTrace();
        }
    }

    @FXML private void handleGoToLogin() {
        try {
            emailField.getScene().setRoot(
                    new FXMLLoader(getClass().getResource("/login.fxml")).load()
            );
        } catch (IOException e) { showError("Impossible de charger la page."); }
    }

    private boolean emailExists(String email) {
        try { return userService.getAll().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email)); }
        catch (Exception e) { return false; }
    }

    private void clearFields() {
        nomField.clear(); prenomField.clear(); emailField.clear();
        passwordField.clear(); confirmPasswordField.clear();
        dateNaissancePicker.setValue(null);
        resetStyle(nomField); resetStyle(prenomField); resetStyle(emailField);
        resetStyle(passwordField); resetStyle(visiblePasswordField);
        resetStyle(confirmPasswordField); resetStyle(visibleConfirmField);
        if (!roleChoice.getItems().isEmpty()) roleChoice.setValue(roleChoice.getItems().get(0));
    }

    private void hideMessages() {
        errorLabel.setVisible(false);   errorLabel.setManaged(false);
        successLabel.setVisible(false); successLabel.setManaged(false);
    }
    private void showError(String m) {
        errorLabel.setText(m);   errorLabel.setVisible(true);   errorLabel.setManaged(true);
        successLabel.setVisible(false); successLabel.setManaged(false);
    }
    private void showSuccess(String m) {
        successLabel.setText(m); successLabel.setVisible(true);  successLabel.setManaged(true);
        errorLabel.setVisible(false);   errorLabel.setManaged(false);
    }

    private void setValid(Control f)   { f.setStyle("-fx-border-color: #10b981; -fx-border-width: 2px;"); }
    private void setInvalid(Control f) { f.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px;"); }
    private void resetStyle(Control f) { f.setStyle(""); }
}