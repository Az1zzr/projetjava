package Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.stage.FileChooser;
import models.User;
import services.UserService;
import utils.SessionManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.regex.Pattern;

public class AccueilUserController {

    @FXML private Label     lblBienvenue;
    @FXML private Label     lblEmail;
    @FXML private Label     lblEmailInfo;
    @FXML private Label     lblNomComplet;
    @FXML private Label     lblDDN;
    @FXML private Label     lblRoleBadge;
    @FXML private Label     lblRoleInfo;
    @FXML private Label     lblAvatarGrand;
    @FXML private Label     lblDate;
    @FXML private ImageView imgProfil;          // photo de profil (si présente)
    @FXML private StackPane avatarPane;         // conteneur de l'avatar

    private final UserService userService = new UserService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Dossier local de stockage des photos
    private static final String PHOTOS_DIR = "photos_profil";

    @FXML
    public void initialize() {
        // Créer le dossier photos si inexistant
        new File(PHOTOS_DIR).mkdirs();
        fillFromSession();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Remplissage de l'interface depuis la session
    // ══════════════════════════════════════════════════════════════════════════
    private void fillFromSession() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        String prenom = user.getPrenom() != null ? user.getPrenom().trim() : "";
        String nom    = user.getNom()    != null ? user.getNom().trim()    : "";
        String email  = user.getEmail()  != null ? user.getEmail()         : "—";
        String role   = user.getRole()   != null ? user.getRole().getNomRole() : "";

        String displayName = !prenom.isEmpty() ? prenom + " " + nom
                : (!nom.isEmpty()   ? nom : email.split("@")[0]);

        lblBienvenue.setText("Mon Compte 👤");
        lblEmail.setText(email);
        if (lblNomComplet != null) lblNomComplet.setText(displayName.trim().isEmpty() ? "—" : displayName.trim());
        if (lblEmailInfo  != null) lblEmailInfo.setText(email);
        if (lblRoleInfo   != null) lblRoleInfo.setText(role.isEmpty() ? "—" : role);
        if (lblDDN        != null) lblDDN.setText(user.getDateNaissance() != null
                ? user.getDateNaissance().format(FMT) : "—");

        // Badge rôle
        if (lblRoleBadge != null) {
            lblRoleBadge.setText("● " + role.toUpperCase());
            String roleLower = role.trim().toLowerCase();
            String badgeStyle = roleLower.startsWith("fournisseur")
                    ? "-fx-text-fill: #c2410c; -fx-background-color: rgba(249,115,22,0.15);"
                    : "-fx-text-fill: #166534; -fx-background-color: rgba(16,185,129,0.12);";
            lblRoleBadge.setStyle(badgeStyle +
                    " -fx-font-size: 10px; -fx-font-weight: bold;" +
                    " -fx-background-radius: 20px; -fx-padding: 3 12;");
        }

        // Date du jour
        if (lblDate != null) {
            LocalDate today = LocalDate.now();
            lblDate.setText(
                    today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH) + " " +
                            today.getDayOfMonth() + " " +
                            today.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH) + " " +
                            today.getYear());
        }

        // ✅ Photo de profil ou initiale
        loadPhoto(user, displayName);
    }

    /** Charge la photo si disponible, sinon affiche l'initiale */
    private void loadPhoto(User user, String displayName) {
        String photoPath = user.getPhotoPath();

        if (photoPath != null && !photoPath.isBlank()) {
            File f = new File(photoPath);
            if (f.exists()) {
                try {
                    if (imgProfil != null) {
                        imgProfil.setImage(new Image(f.toURI().toString()));
                        imgProfil.setVisible(true);
                        imgProfil.setManaged(true);
                    }
                    if (lblAvatarGrand != null) {
                        lblAvatarGrand.setVisible(false);
                        lblAvatarGrand.setManaged(false);
                    }
                    // Style cercle pour la photo
                    if (avatarPane != null) {
                        avatarPane.setStyle(
                                "-fx-background-color: transparent; -fx-background-radius: 50%;");
                    }
                    return;
                } catch (Exception ignored) {}
            }
        }

        // Fallback : initiale
        if (imgProfil != null) { imgProfil.setVisible(false); imgProfil.setManaged(false); }
        if (lblAvatarGrand != null) {
            String init = displayName.trim().isEmpty() ? "U"
                    : String.valueOf(displayName.trim().charAt(0)).toUpperCase();
            lblAvatarGrand.setText(init);
            lblAvatarGrand.setVisible(true);
            lblAvatarGrand.setManaged(true);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ✅ CHANGER LA PHOTO DE PROFIL
    // ══════════════════════════════════════════════════════════════════════════
    @FXML private void handleChangerPhoto() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        File selected = chooser.showOpenDialog(lblBienvenue.getScene().getWindow());
        if (selected == null) return;

        try {
            // Copier le fichier dans le dossier local
            String ext      = selected.getName().substring(selected.getName().lastIndexOf('.'));
            String fileName = "user_" + user.getId() + "_" + System.currentTimeMillis() + ext;
            Path dest       = Paths.get(PHOTOS_DIR, fileName);
            Files.copy(selected.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

            // Mettre à jour l'utilisateur
            String photoPath = dest.toString();
            user.setPhotoPath(photoPath);
            userService.update(user);
            SessionManager.getInstance().setCurrentUser(user);

            // Rafraîchir l'affichage
            fillFromSession();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Impossible de changer la photo : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ✅ MODIFIER MON COMPTE
    // ══════════════════════════════════════════════════════════════════════════
    @FXML private void handleModifier() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        // Vérification ID valide
        if (user.getId() == 0) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "❌ ID utilisateur invalide. Déconnectez-vous et reconnectez-vous.");
            return;
        }

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Modifier mon compte");
        dialog.setHeaderText("✏️  Modifier mes informations");
        dialog.getDialogPane().setStyle("-fx-font-size: 13px; -fx-background-color: #f8fafc;");
        dialog.getDialogPane().setPrefWidth(460);

        ButtonType saveBtn = new ButtonType("💾 Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        String fieldStyle =
                "-fx-pref-height: 42px; -fx-font-size: 13px;" +
                        "-fx-background-color: white; -fx-border-color: #e2e8f0;" +
                        "-fx-border-width: 1.5px; -fx-border-radius: 10px; -fx-background-radius: 10px;" +
                        "-fx-padding: 8 14;";
        String labelStyle = "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #374151;";

        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(14);
        grid.setPadding(new Insets(20, 30, 10, 20));

        TextField     tfNom    = new TextField(user.getNom()    != null ? user.getNom()    : "");
        TextField     tfPrenom = new TextField(user.getPrenom() != null ? user.getPrenom() : "");
        TextField     tfEmail  = new TextField(user.getEmail()  != null ? user.getEmail()  : "");
        DatePicker    dpDDN    = new DatePicker(user.getDateNaissance());
        PasswordField pfPwd    = new PasswordField();
        PasswordField pfConfirm = new PasswordField();

        for (TextField tf : new TextField[]{tfNom, tfPrenom, tfEmail}) tf.setStyle(fieldStyle);
        pfPwd.setStyle(fieldStyle);     pfPwd.setPromptText("Laisser vide = pas de changement");
        pfConfirm.setStyle(fieldStyle); pfConfirm.setPromptText("Confirmer le nouveau mot de passe");
        dpDDN.setStyle("-fx-pref-height: 42px; -fx-pref-width: 280px; -fx-font-size: 13px;");
        tfNom.setPrefWidth(280); tfPrenom.setPrefWidth(280); tfEmail.setPrefWidth(280);

        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11px; " +
                "-fx-background-color: #fef2f2; -fx-background-radius: 8px; -fx-padding: 8 12;");
        errorLbl.setWrapText(true); errorLbl.setMaxWidth(400);
        errorLbl.setVisible(false); errorLbl.setManaged(false);

        grid.add(new Label("Nom :")              {{ setStyle(labelStyle); }}, 0, 0); grid.add(tfNom,     1, 0);
        grid.add(new Label("Prénom :")           {{ setStyle(labelStyle); }}, 0, 1); grid.add(tfPrenom,  1, 1);
        grid.add(new Label("Email :")            {{ setStyle(labelStyle); }}, 0, 2); grid.add(tfEmail,   1, 2);
        grid.add(new Label("Date naissance :")   {{ setStyle(labelStyle); }}, 0, 3); grid.add(dpDDN,     1, 3);
        grid.add(new Label("Nouveau mot de passe :") {{ setStyle(labelStyle); }}, 0, 4); grid.add(pfPwd, 1, 4);
        grid.add(new Label("Confirmer :")        {{ setStyle(labelStyle); }}, 0, 5); grid.add(pfConfirm, 1, 5);
        grid.add(errorLbl, 0, 6, 2, 1);

        dialog.getDialogPane().setContent(grid);

        // Bloquer si erreurs de validation
        dialog.getDialogPane().lookupButton(saveBtn).addEventFilter(
                javafx.event.ActionEvent.ACTION, event -> {
                    String em  = tfEmail.getText().trim().toLowerCase();
                    String pwd = pfPwd.getText();
                    StringBuilder err = new StringBuilder();

                    if (tfNom.getText().trim().length() < 2)     err.append("• Nom : minimum 2 caractères.\n");
                    if (tfPrenom.getText().trim().length() < 2)  err.append("• Prénom : minimum 2 caractères.\n");
                    if (!EMAIL.matcher(em).matches())            err.append("• Email invalide.\n");
                    if (!pwd.isEmpty()) {
                        if (pwd.length() < 8)                    err.append("• Mot de passe : minimum 8 caractères.\n");
                        if (!pwd.matches(".*[A-Z].*"))           err.append("• Mot de passe : 1 majuscule requise.\n");
                        if (!pwd.matches(".*\\d.*"))             err.append("• Mot de passe : 1 chiffre requis.\n");
                        if (!pwd.equals(pfConfirm.getText()))    err.append("• Les mots de passe ne correspondent pas.\n");
                    }

                    if (err.length() > 0) {
                        errorLbl.setText(err.toString().trim());
                        errorLbl.setVisible(true); errorLbl.setManaged(true);
                        event.consume();
                    }
                }
        );

        dialog.setResultConverter(btn -> btn == saveBtn);

        dialog.showAndWait().ifPresent(ok -> {
            if (!ok) return;
            try {
                // ✅ Mettre à jour l'objet user avec les nouvelles valeurs
                user.setNom(tfNom.getText().trim());
                user.setPrenom(tfPrenom.getText().trim());
                user.setEmail(tfEmail.getText().trim().toLowerCase());
                user.setDateNaissance(dpDDN.getValue());
                if (!pfPwd.getText().isEmpty())
                    user.setMotDePasse(pfPwd.getText());

                // ✅ Sauvegarder en base
                userService.update(user);

                // ✅ Mettre à jour la session
                SessionManager.getInstance().setCurrentUser(user);

                // ✅ Rafraîchir l'affichage
                fillFromSession();

                showInfo("✅ Compte mis à jour avec succès !");

            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "❌ " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ✅ SUPPRIMER MON COMPTE
    // ══════════════════════════════════════════════════════════════════════════
    @FXML private void handleSupprimer() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
        conf.setTitle("Supprimer mon compte");
        conf.setHeaderText("⚠️  Suppression définitive");
        conf.setContentText(
                "Vous allez supprimer définitivement :\n\n" +
                        "• " + user.getNomComplet() + "\n" +
                        "• " + user.getEmail() + "\n\n" +
                        "Cette action est IRRÉVERSIBLE. Confirmer ?"
        );
        conf.getDialogPane().setStyle("-fx-font-size: 13px;");

        ButtonType supprimerBtn = new ButtonType("🗑️  Oui, supprimer", ButtonBar.ButtonData.OK_DONE);
        conf.getButtonTypes().setAll(supprimerBtn, ButtonType.CANCEL);

        conf.showAndWait().ifPresent(btn -> {
            if (btn == supprimerBtn) {
                try {
                    userService.delete(user);
                    SessionManager.getInstance().logout();

                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
                    javafx.scene.Parent root = loader.load();
                    var scene = lblBienvenue.getScene();
                    scene.getWindow().setWidth(1200);
                    scene.getWindow().setHeight(700);
                    ((javafx.stage.Stage) scene.getWindow()).centerOnScreen();
                    scene.setRoot(root);

                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "❌ " + ex.getMessage());
                }
            }
        });
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Succès"); a.setHeaderText(null); a.setContentText(msg);
        a.getDialogPane().setStyle("-fx-font-size: 13px;"); a.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
        a.getDialogPane().setStyle("-fx-font-size: 13px;"); a.showAndWait();
    }
}