package Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import models.User;
import utils.SessionManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class UserDashboardController {

    @FXML private StackPane contentArea;
    @FXML private Label     lblPageTitle;
    @FXML private Label     lblUserName;
    @FXML private Label     lblUserRole;
    @FXML private Label     lblAvatarInitial;
    @FXML private Label     lblTopAvatar;
    @FXML private Label     lblRoleBadge;

    @FXML private Button btnAccueil;
    @FXML private Button btnProduits;
    @FXML private Button btnCommandes;
    @FXML private Button btnLivraisons;
    @FXML private Button btnFeedback;
    @FXML private Button btnPublications;
    @FXML private javafx.scene.image.ImageView imgTopAvatar;

    private Button activeButton;
    private final Map<String, Node> cache = new HashMap<>();

    private static final String BTN_ACTIVE =
            "-fx-background-color: #f97316; -fx-text-fill: white; -fx-font-size: 13px; " +
                    "-fx-font-weight: bold; -fx-alignment: CENTER_LEFT; -fx-padding: 12 16; " +
                    "-fx-background-radius: 10px; -fx-cursor: hand;";

    private static final String BTN_INACTIVE =
            "-fx-background-color: transparent; -fx-text-fill: #7a9ab8; -fx-font-size: 13px; " +
                    "-fx-alignment: CENTER_LEFT; -fx-padding: 12 16; -fx-background-radius: 10px; -fx-cursor: hand;";

    @FXML
    public void initialize() {
        activeButton = btnAccueil;
        // ✅ Session déjà remplie avant loader.load() dans LoginController
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) applyUser(user);
        showAccueil();
    }

    public void setCurrentUser(User user) {
        if (user == null) return;
        applyUser(user);
    }

    private void applyUser(User user) {
        String prenom = user.getPrenom() != null ? user.getPrenom().trim() : "";
        String nom    = user.getNom()    != null ? user.getNom().trim()    : "";
        String email  = user.getEmail()  != null ? user.getEmail()         : "";
        String role   = user.getRole()   != null ? user.getRole().getNomRole() : "";

        String displayName = !prenom.isEmpty() ? prenom + " " + nom
                : (!nom.isEmpty()   ? nom
                : email.split("@")[0]);

        if (lblUserName != null) lblUserName.setText(displayName.trim());
        if (lblUserRole != null) lblUserRole.setText(role);

        String init = displayName.trim().isEmpty() ? "U"
                : String.valueOf(displayName.trim().charAt(0)).toUpperCase();
        if (lblAvatarInitial != null) lblAvatarInitial.setText(init);
        // Top avatar photo
        if (imgTopAvatar != null && user.getPhotoPath() != null && !user.getPhotoPath().isBlank()) {
            java.io.File f = new java.io.File(user.getPhotoPath());
            if (f.exists()) {
                try {
                    imgTopAvatar.setImage(new javafx.scene.image.Image(f.toURI().toString()));
                    imgTopAvatar.setVisible(true); imgTopAvatar.setManaged(true);
                    if (lblTopAvatar != null) { lblTopAvatar.setVisible(false); lblTopAvatar.setManaged(false); }
                } catch (Exception ignored) {}
            }
        }
        if (lblTopAvatar     != null) lblTopAvatar.setText(init);

        // ✅ Badge couleur — startsWith pour gérer singulier ET pluriel
        if (lblRoleBadge != null) {
            lblRoleBadge.setText("● " + role.toUpperCase());
            String roleLower = role.trim().toLowerCase();
            String color = roleLower.startsWith("fournisseur")
                    ? "-fx-background-color: rgba(249,115,22,0.15); -fx-text-fill: #f97316;"
                    : "-fx-background-color: rgba(16,185,129,0.12); -fx-text-fill: #10b981;";
            lblRoleBadge.setStyle(color +
                    " -fx-font-size: 9px; -fx-font-weight: bold; " +
                    "-fx-background-radius: 20px; -fx-padding: 3 10;");
        }
    }

    @FXML public void showAccueil()    { loadPage("/AccueilUser.fxml", "Mon Compte", btnAccueil); }
    @FXML public void showProduits()   { loadPage("/Produit.fxml",     "Produits",   btnProduits); }
    @FXML public void showCommandes()  { loadPage("/Commande.fxml",    "Commandes",  btnCommandes); }
    @FXML public void showLivraisons() { loadPage("/Livraison.fxml",   "Livraisons", btnLivraisons); }
    @FXML public void showFeedback()       { loadPage("/Feedback.fxml",      "Feedback",      btnFeedback); }
    @FXML public void showPublications()   { loadPage("/Publications.fxml",  "Publications",  btnPublications); }

    private void loadPage(String path, String title, Button btn) {
        if (lblPageTitle != null) lblPageTitle.setText(title);
        if (activeButton != null) { activeButton.setStyle(BTN_INACTIVE); activeButton.setMaxWidth(Double.MAX_VALUE); }
        btn.setStyle(BTN_ACTIVE); btn.setMaxWidth(Double.MAX_VALUE);
        activeButton = btn;
        try {
            // AccueilUser rechargé à chaque fois (données fraîches depuis session)
            Node page = path.equals("/AccueilUser.fxml")
                    ? new FXMLLoader(getClass().getResource(path)).load()
                    : cache.computeIfAbsent(path, p -> {
                try { return new FXMLLoader(getClass().getResource(p)).load(); }
                catch (IOException e) { return null; }
            });
            if (page != null) contentArea.getChildren().setAll(page);
            else showPlaceholder(title);
        } catch (Exception e) { showPlaceholder(title); }
    }

    private void showPlaceholder(String name) {
        VBox box = new VBox(16); box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 16px;");
        Label icon = new Label("🚧"); icon.setStyle("-fx-font-size: 52px;");
        Label lbl  = new Label("Module «" + name + "» en développement");
        lbl.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label sub  = new Label("Disponible prochainement.");
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
        box.getChildren().addAll(icon, lbl, sub);
        contentArea.getChildren().setAll(box);
    }

    /** Click sur l'avatar en haut à droite → Mon Compte */
    @FXML private void handleTopAvatarClick() {
        showAccueil();
    }

    @FXML private void handleLogout() {
        SessionManager.getInstance().logout();
        cache.clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            javafx.scene.Parent root = loader.load();
            var scene = contentArea.getScene();
            scene.getWindow().setWidth(1200); scene.getWindow().setHeight(700);
            ((javafx.stage.Stage) scene.getWindow()).centerOnScreen();
            scene.setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }
}