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

/**
 * Layout ADMIN uniquement.
 * Donne accès à tout : utilisateurs, rôles, produits, commandes, livraisons, feedback.
 */
public class MainLayoutController {

    @FXML private StackPane contentArea;
    @FXML private Label     lblPageTitle;
    @FXML private Label     lblUserName;
    @FXML private Label     lblUserRole;
    @FXML private Label     lblAvatarInitial;
    @FXML private Label     lblTopAvatar;
    @FXML private Label     lblAdminBadge;

    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;
    @FXML private Button btnRoles;
    @FXML private Button btnProduits;
    @FXML private Button btnFournisseurs;
    @FXML private Button btnCommandes;
    @FXML private Button btnLivraisons;
    @FXML private Button btnFeedback;

    private Button activeButton;
    private final Map<String, Node> cache = new HashMap<>();

    private static final String BTN_ACTIVE =
            "-fx-background-color: #f97316; -fx-text-fill: white; -fx-font-size: 13px; " +
                    "-fx-font-weight: bold; -fx-alignment: CENTER_LEFT; -fx-padding: 12 16; " +
                    "-fx-background-radius: 10px; -fx-cursor: hand;";

    private static final String BTN_INACTIVE =
            "-fx-background-color: transparent; -fx-text-fill: #7a9ab8; -fx-font-size: 13px; " +
                    "-fx-alignment: CENTER_LEFT; -fx-padding: 12 16; -fx-background-radius: 10px; -fx-cursor: hand;";

    @FXML public void initialize() {
        activeButton = btnDashboard;
        loadPage("/Dashboard.fxml", "Dashboard", btnDashboard);
    }

    public void setCurrentUser(User user) {
        if (user == null) return;
        String prenom = user.getPrenom() != null ? user.getPrenom() : "";
        String nom    = user.getNom()    != null ? user.getNom()    : "";
        String role   = user.getRole()   != null ? user.getRole().getNomRole() : "Admin";

        lblUserName.setText(prenom + " " + nom);
        lblUserRole.setText(role);

        String init = !prenom.isEmpty() ? String.valueOf(prenom.charAt(0)).toUpperCase()
                : (!nom.isEmpty() ? String.valueOf(nom.charAt(0)).toUpperCase() : "A");
        if (lblAvatarInitial != null) lblAvatarInitial.setText(init);
        if (lblTopAvatar     != null) lblTopAvatar.setText(init);

        // Badge ADMIN visible
        if (lblAdminBadge != null) lblAdminBadge.setVisible(true);
    }

    @FXML public void showDashboard()    { loadPage("/Dashboard.fxml",   "Dashboard",    btnDashboard); }
    @FXML public void showUsers()        { loadPage("/UserTable.fxml",   "Utilisateurs", btnUsers); }
    @FXML public void showRoles()        { loadPage("/ROLE.fxml",        "Rôles",        btnRoles); }
    @FXML public void showProduits()     { loadPage("/Produit.fxml",     "Produits",     btnProduits); }
    @FXML public void showFournisseurs() { loadPage("/Fournisseur.fxml", "Fournisseurs", btnFournisseurs); }
    @FXML public void showCommandes()    { loadPage("/Commande.fxml",    "Commandes",    btnCommandes); }
    @FXML public void showLivraisons()   { loadPage("/Livraison.fxml",   "Livraisons",   btnLivraisons); }
    @FXML public void showFeedback()     { loadPage("/Feedback.fxml",    "Feedback",     btnFeedback); }

    private void loadPage(String path, String title, Button btn) {
        lblPageTitle.setText(title);
        if (activeButton != null) { activeButton.setStyle(BTN_INACTIVE); activeButton.setMaxWidth(Double.MAX_VALUE); }
        btn.setStyle(BTN_ACTIVE); btn.setMaxWidth(Double.MAX_VALUE);
        activeButton = btn;
        try {
            Node page = cache.computeIfAbsent(path, p -> {
                try { return new FXMLLoader(getClass().getResource(p)).load(); }
                catch (IOException e) { return null; }
            });
            if (page != null) contentArea.getChildren().setAll(page);
            else showPlaceholder(title);
        } catch (Exception e) { showPlaceholder(title); }
    }

    private void showPlaceholder(String name) {
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 16px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 10, 0, 0, 3);");
        Label icon  = new Label("🚧"); icon.setStyle("-fx-font-size: 52px;");
        Label title = new Label("Module «" + name + "» en développement");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label sub   = new Label("Cette section sera disponible prochainement.");
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
        Region bar  = new Region();
        bar.setStyle("-fx-background-color: #f97316; -fx-pref-height: 4px; -fx-pref-width: 60px; -fx-background-radius: 2px;");
        box.getChildren().addAll(icon, title, sub, bar);
        contentArea.getChildren().setAll(box);
    }

    @FXML private void handleLogout() {
        SessionManager.getInstance().logout();
        navigateTo("/login.fxml", 1200, 700);
    }

    private void navigateTo(String path, double w, double h) {
        try {
            cache.clear();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            javafx.scene.Parent root = loader.load();
            var scene = contentArea.getScene();
            scene.getWindow().setWidth(w); scene.getWindow().setHeight(h);
            ((javafx.stage.Stage) scene.getWindow()).centerOnScreen();
            scene.setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }
}
