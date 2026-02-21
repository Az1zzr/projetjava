package Controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import models.Role;
import models.User;
import services.RoleService;
import services.UserService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Accessible uniquement par l'Administrateur.
 * Affiche les Fournisseurs et Entrepreneurs (pas les admins).
 */
public class AfficherUser {

    @FXML private TableView<User>           userTable;
    @FXML private TableColumn<User,Integer> colId;
    @FXML private TableColumn<User,String>  colNomComplet;
    @FXML private TableColumn<User,String>  colEmail;
    @FXML private TableColumn<User,String>  colDDN;
    @FXML private TableColumn<User,String>  colRole;
    @FXML private TableColumn<User,String>  colMotDePasse;
    @FXML private TableColumn<User,Void>    colActions;
    @FXML private TextField                 searchField;
    @FXML private Label                     countLabel;

    private final ObservableList<User> allUsers      = FXCollections.observableArrayList();
    private final ObservableList<User> filteredUsers = FXCollections.observableArrayList();
    private final UserService userService = new UserService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML public void initialize() {
        setupColumns();
        setupActionsColumn();
        setupSearch();
        userTable.setItems(filteredUsers);
        loadUsers();
    }

    // ─── Colonnes ────────────────────────────────────────────────────────────

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setStyle("-fx-alignment: CENTER;");

        colNomComplet.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNomComplet()));

        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        colDDN.setCellValueFactory(c -> {
            LocalDate d = c.getValue().getDateNaissance();
            return new SimpleStringProperty(d != null ? d.format(FMT) : "—");
        });
        colDDN.setStyle("-fx-alignment: CENTER;");

        // Rôle avec badge coloré
        colRole.setCellValueFactory(c -> {
            Role r = c.getValue().getRole();
            return new SimpleStringProperty(r != null ? r.getNomRole() : "—");
        });
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item);
                String style = switch (item.toLowerCase()) {
                    case "fournisseur"  -> "-fx-background-color: #fff7ed; -fx-text-fill: #c2410c;";
                    case "entrepreneur" -> "-fx-background-color: #f0fdf4; -fx-text-fill: #166534;";
                    default             -> "-fx-background-color: #f1f5f9; -fx-text-fill: #475569;";
                };
                badge.setStyle(style + " -fx-font-size: 11px; -fx-font-weight: bold; " +
                        "-fx-background-radius: 20px; -fx-padding: 4 12;");
                setGraphic(badge); setAlignment(Pos.CENTER);
            }
        });
        colRole.setStyle("-fx-alignment: CENTER;");

        // Mot de passe masqué avec toggle
        colMotDePasse.setCellFactory(col -> new TableCell<>() {
            private final Label lbl = new Label("••••••••");
            private final Button btn = new Button("👁️");
            private boolean visible = false;
            {
                lbl.setStyle("-fx-font-family: monospace; -fx-font-size: 13px;");
                btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 2 6;");
                btn.setOnAction(e -> {
                    visible = !visible;
                    User u = getTableView().getItems().get(getIndex());
                    lbl.setText(visible ? u.getMotDePasse() : "••••••••");
                    btn.setText(visible ? "🙈" : "👁️");
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                visible = false; lbl.setText("••••••••"); btn.setText("👁️");
                HBox hb = new HBox(6, lbl, btn); hb.setAlignment(Pos.CENTER_LEFT);
                setGraphic(hb);
            }
        });
    }

    // ─── Actions boutons dans la ligne ───────────────────────────────────────

    private void setupActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("✏️ Modifier");
            private final Button delBtn  = new Button("🗑️ Supprimer");
            private final HBox   box     = new HBox(8, editBtn, delBtn);
            {
                box.setAlignment(Pos.CENTER);
                editBtn.setStyle(
                        "-fx-background-color: #1e3a5f; -fx-text-fill: white; " +
                                "-fx-font-size: 11px; -fx-font-weight: bold; " +
                                "-fx-background-radius: 8px; -fx-padding: 6 12; -fx-cursor: hand;");
                delBtn.setStyle(
                        "-fx-background-color: #dc2626; -fx-text-fill: white; " +
                                "-fx-font-size: 11px; -fx-font-weight: bold; " +
                                "-fx-background-radius: 8px; -fx-padding: 6 12; -fx-cursor: hand;");

                editBtn.setOnAction(e -> handleModifyRow(getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(e  -> handleDeleteRow(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    // ─── Chargement ──────────────────────────────────────────────────────────

    private void loadUsers() {
        try {
            // ✅ Admin voit uniquement les Fournisseurs et Entrepreneurs
            allUsers.setAll(userService.getNonAdmins());
            filteredUsers.setAll(allUsers);
            updateCount();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Chargement échoué : " + e.getMessage());
        }
    }

    @FXML private void handleRefresh() { loadUsers(); }

    // ─── Recherche ───────────────────────────────────────────────────────────

    private void setupSearch() {
        if (searchField == null) return;
        searchField.textProperty().addListener((o, ov, nv) -> {
            if (nv == null || nv.isBlank()) { filteredUsers.setAll(allUsers); }
            else {
                String q = nv.toLowerCase().trim();
                filteredUsers.setAll(allUsers.stream().filter(u ->
                        u.getNomComplet().toLowerCase().contains(q) ||
                                u.getEmail().toLowerCase().contains(q) ||
                                (u.getRole() != null && u.getRole().getNomRole().toLowerCase().contains(q))
                ).toList());
            }
            updateCount();
        });
    }

    // ─── Modifier ────────────────────────────────────────────────────────────

    private void handleModifyRow(User user) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Modifier l'utilisateur");
        dialog.setHeaderText("✏️  Modifier : " + user.getNomComplet());

        ButtonType save = new ButtonType("💾 Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-font-size: 13px;");

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(12); grid.setVgap(14);
        grid.setPadding(new javafx.geometry.Insets(20, 40, 20, 20));

        String fieldStyle = "-fx-pref-width: 280px; -fx-pref-height: 38px; -fx-font-size: 13px; " +
                "-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; " +
                "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px;";

        TextField  tfNom    = new TextField(user.getNom());    tfNom.setStyle(fieldStyle);
        TextField  tfPrenom = new TextField(user.getPrenom()); tfPrenom.setStyle(fieldStyle);
        TextField  tfEmail  = new TextField(user.getEmail());  tfEmail.setStyle(fieldStyle);
        PasswordField tfPwd = new PasswordField();             tfPwd.setStyle(fieldStyle);
        tfPwd.setText(user.getMotDePasse());

        ChoiceBox<Role> cbRole = new ChoiceBox<>();
        try { cbRole.getItems().addAll(new RoleService().getAll()); cbRole.setValue(user.getRole()); }
        catch (Exception ignored) {}

        grid.add(new Label("Nom :"),    0, 0); grid.add(tfNom,    1, 0);
        grid.add(new Label("Prénom :"), 0, 1); grid.add(tfPrenom, 1, 1);
        grid.add(new Label("Email :"),  0, 2); grid.add(tfEmail,  1, 2);
        grid.add(new Label("Mot de passe :"), 0, 3); grid.add(tfPwd, 1, 3);
        grid.add(new Label("Rôle :"),   0, 4); grid.add(cbRole,   1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn == save) {
                user.setNom(tfNom.getText().trim());
                user.setPrenom(tfPrenom.getText().trim());
                user.setEmail(tfEmail.getText().trim().toLowerCase());
                if (!tfPwd.getText().isEmpty()) user.setMotDePasse(tfPwd.getText());
                user.setRole(cbRole.getValue());
                return user;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(u -> {
            try {
                userService.update(u);
                userTable.refresh();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Utilisateur modifié !");
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "❌ " + ex.getMessage());
            }
        });
    }

    // ─── Supprimer ───────────────────────────────────────────────────────────

    private void handleDeleteRow(User user) {
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
        conf.setTitle("Confirmation");
        conf.setHeaderText("Supprimer l'utilisateur");
        conf.setContentText("Supprimer « " + user.getNomComplet() + " » ?");
        conf.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    userService.delete(user);
                    allUsers.remove(user); filteredUsers.remove(user);
                    updateCount();
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Utilisateur supprimé !");
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "❌ " + ex.getMessage());
                }
            }
        });
    }

    private void updateCount() {
        if (countLabel != null)
            countLabel.setText(filteredUsers.size() + " utilisateur(s)");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
        a.getDialogPane().setStyle("-fx-font-size: 13px;");
        a.showAndWait();
    }
}
