package Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import models.Role;
import services.RoleService;

/**
 * Controller Gestion Rôles — intégré dans MainLayout.
 * Bouton "Retour" standalone supprimé (navigation via sidebar).
 */
public class AjouterRole {

    @FXML private TextField        nomRoleField;
    @FXML private TableView<Role>  roleTable;
    @FXML private TableColumn<Role,Integer> colId;
    @FXML private TableColumn<Role,String>  colNom;
    @FXML private TableColumn<Role,Void>    colActions;
    @FXML private Label            countLabel;

    private final RoleService service = new RoleService();
    private final ObservableList<Role> roles = FXCollections.observableArrayList();

    @FXML public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id_role"));
        colId.setStyle("-fx-alignment: CENTER; -fx-font-size: 13px;");
        colNom.setCellValueFactory(new PropertyValueFactory<>("nomRole"));
        colNom.setStyle("-fx-font-size: 13px; -fx-padding: 8;");

        setupActionsColumn();
        setupValidation();
        loadRoles();
    }

    // ─── Colonne actions ─────────────────────────────────────────────────────
    private void setupActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button modBtn = new Button("✏️ Modifier");
            private final Button delBtn = new Button("🗑️ Supprimer");
            private final HBox box = new HBox(8, modBtn, delBtn);
            {
                box.setAlignment(Pos.CENTER);
                modBtn.setStyle(
                        "-fx-background-color: #1e3a5f; -fx-text-fill: white; " +
                                "-fx-font-size: 11px; -fx-font-weight: bold; " +
                                "-fx-background-radius: 8px; -fx-padding: 6 14; -fx-cursor: hand;");
                delBtn.setStyle(
                        "-fx-background-color: #dc2626; -fx-text-fill: white; " +
                                "-fx-font-size: 11px; -fx-font-weight: bold; " +
                                "-fx-background-radius: 8px; -fx-padding: 6 14; -fx-cursor: hand;");
                modBtn.setOnAction(e -> modifyRole(getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(e -> deleteRole(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    // ─── Ajouter ─────────────────────────────────────────────────────────────
    @FXML public void ajouterRole() {
        String nom = nomRoleField.getText().trim();
        if (nom.isEmpty()) { showAlert(Alert.AlertType.ERROR, "Erreur", "⚠️ Le nom du rôle est obligatoire !"); return; }
        if (nom.length() < 3) { showAlert(Alert.AlertType.ERROR, "Erreur", "⚠️ Minimum 3 caractères !"); return; }
        if (roles.stream().anyMatch(r -> r.getNomRole().equalsIgnoreCase(nom))) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "⚠️ Ce rôle existe déjà !"); return;
        }
        try {
            Role r = new Role(); r.setNomRole(nom);
            service.add(r);
            loadRoles();
            nomRoleField.clear(); nomRoleField.setStyle("");
            showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Rôle '" + nom + "' ajouté !");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "❌ " + e.getMessage());
        }
    }

    // ─── Modifier ────────────────────────────────────────────────────────────
    private void modifyRole(Role role) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Modifier le rôle");
        dialog.setHeaderText("✏️  Modifier : " + role.getNomRole());
        ButtonType save = new ButtonType("💾 Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        TextField tf = new TextField(role.getNomRole());
        tf.setStyle("-fx-pref-width: 300px; -fx-pref-height: 40px; -fx-font-size: 14px; " +
                "-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; " +
                "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10);
        content.getChildren().addAll(new Label("Nouveau nom :"), tf);
        content.setPadding(new javafx.geometry.Insets(16));
        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(btn -> btn == save ? tf.getText().trim() : null);

        dialog.showAndWait().ifPresent(newName -> {
            if (newName.length() < 3) { showAlert(Alert.AlertType.ERROR, "Erreur", "⚠️ Minimum 3 caractères !"); return; }
            try {
                role.setNomRole(newName); service.update(role);
                loadRoles();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Rôle modifié !");
            } catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Erreur", "❌ " + e.getMessage()); }
        });
    }

    // ─── Supprimer ───────────────────────────────────────────────────────────
    private void deleteRole(Role role) {
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
        conf.setTitle("Confirmation"); conf.setHeaderText("Supprimer le rôle");
        conf.setContentText("Supprimer le rôle « " + role.getNomRole() + " » ?");
        conf.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.delete(role); roles.remove(role); updateCount();
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Rôle supprimé !");
                } catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Erreur", "❌ " + e.getMessage()); }
            }
        });
    }

    // ─── Depuis boutons barre ─────────────────────────────────────────────────
    @FXML private void handleRefresh() { loadRoles(); }

    @FXML private void handleModify() {
        Role sel = roleTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert(Alert.AlertType.WARNING, "Sélection", "⚠️ Sélectionnez un rôle à modifier."); return; }
        modifyRole(sel);
    }

    @FXML private void handleDelete() {
        Role sel = roleTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert(Alert.AlertType.WARNING, "Sélection", "⚠️ Sélectionnez un rôle à supprimer."); return; }
        deleteRole(sel);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private void loadRoles() {
        try {
            roles.clear(); roles.addAll(service.getAll());
            roleTable.setItems(roles); updateCount();
        } catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Chargement : " + e.getMessage()); }
    }

    private void updateCount() {
        if (countLabel != null) countLabel.setText(roles.size() + " rôle(s)");
    }

    private void setupValidation() {
        nomRoleField.textProperty().addListener((o, ov, nv) -> {
            if (nv.trim().isEmpty()) nomRoleField.setStyle("");
            else if (nv.trim().length() < 3) nomRoleField.setStyle("-fx-border-color: #f97316; -fx-border-width: 2px; -fx-border-radius: 10px;");
            else nomRoleField.setStyle("-fx-border-color: #10b981; -fx-border-width: 2px; -fx-border-radius: 10px;");
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
        a.getDialogPane().setStyle("-fx-font-size: 13px;"); a.showAndWait();
    }
}
