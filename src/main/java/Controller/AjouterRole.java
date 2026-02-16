package Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import models.Role;
import services.RoleService;

import java.io.IOException;

public class AjouterRole {

    @FXML
    private TextField nomRoleField;

    @FXML
    private TableView<Role> roleTable;

    @FXML
    private TableColumn<Role, Integer> colId;

    @FXML
    private TableColumn<Role, String> colNom;

    @FXML
    private TableColumn<Role, Void> colActions;

    @FXML
    private Label countLabel;

    private RoleService service = new RoleService();
    private ObservableList<Role> roles = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Configuration des colonnes
        colId.setCellValueFactory(new PropertyValueFactory<>("id_role"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nomRole"));

        // Style des colonnes
        colId.setStyle("-fx-alignment: CENTER; -fx-font-size: 13px;");
        colNom.setStyle("-fx-font-size: 14px; -fx-padding: 10;");

        // Colonne Actions avec boutons
        setupActionsColumn();

        // Charger les rôles
        loadRoles();

        // Validation en temps réel
        setupValidation();
    }

    // Configuration de la colonne Actions avec boutons Modifier et Supprimer
    private void setupActionsColumn() {
        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button modifyBtn = new Button("✏️ Modifier");
            private final Button deleteBtn = new Button("🗑️");
            private final HBox container = new HBox(8);

            {
                // Style du bouton Modifier (Orange)
                modifyBtn.setStyle(
                        "-fx-background-color: linear-gradient(to right, #f97316, #fb923c); " +
                                "-fx-text-fill: white; " +
                                "-fx-font-family: 'Poppins', sans-serif; " +
                                "-fx-font-weight: bold; " +
                                "-fx-font-size: 11px; " +
                                "-fx-padding: 6 14 6 14; " +
                                "-fx-background-radius: 8px; " +
                                "-fx-cursor: hand;"
                );

                // Style du bouton Supprimer (Rouge)
                deleteBtn.setStyle(
                        "-fx-background-color: linear-gradient(to right, #dc2626, #ef4444); " +
                                "-fx-text-fill: white; " +
                                "-fx-font-size: 13px; " +
                                "-fx-padding: 6 10 6 10; " +
                                "-fx-background-radius: 8px; " +
                                "-fx-cursor: hand;"
                );

                // Actions
                modifyBtn.setOnAction(event -> {
                    Role role = getTableView().getItems().get(getIndex());
                    modifyRole(role);
                });

                deleteBtn.setOnAction(event -> {
                    Role role = getTableView().getItems().get(getIndex());
                    deleteRole(role);
                });

                container.setAlignment(Pos.CENTER);
                container.getChildren().addAll(modifyBtn, deleteBtn);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });
    }

    // Ajouter un rôle
    @FXML
    public void ajouterRole() {
        String nomRole = nomRoleField.getText().trim();

        // Validation
        if (nomRole.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur de validation",
                    "⚠️ Le nom du rôle est obligatoire !");
            nomRoleField.setStyle("-fx-border-color: #dc2626; -fx-border-width: 2px;");
            return;
        }

        if (nomRole.length() < 3) {
            showAlert(Alert.AlertType.ERROR, "Erreur de validation",
                    "⚠️ Le nom du rôle doit contenir au moins 3 caractères !");
            nomRoleField.setStyle("-fx-border-color: #dc2626; -fx-border-width: 2px;");
            return;
        }

        // Vérifier les doublons
        boolean exists = roles.stream()
                .anyMatch(r -> r.getNomRole().equalsIgnoreCase(nomRole));

        if (exists) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "⚠️ Ce rôle existe déjà !");
            nomRoleField.setStyle("-fx-border-color: #dc2626; -fx-border-width: 2px;");
            return;
        }

        try {
            Role r = new Role();
            r.setNomRole(nomRole);
            service.add(r);

            loadRoles();
            nomRoleField.clear();
            nomRoleField.setStyle("");

            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    "✅ Rôle '" + nomRole + "' ajouté avec succès !");

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "❌ Erreur lors de l'ajout : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Modifier un rôle
    private void modifyRole(Role role) {
        // Créer une boîte de dialogue
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Modifier le rôle");
        dialog.setHeaderText("Modifier : " + role.getNomRole());

        // Boutons
        ButtonType saveButtonType = new ButtonType("💾 Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Champ de texte
        TextField textField = new TextField(role.getNomRole());
        textField.setPromptText("Nom du rôle");
        textField.setStyle(
                "-fx-pref-width: 300px; " +
                        "-fx-pref-height: 40px; " +
                        "-fx-font-family: 'Roboto', sans-serif; " +
                        "-fx-font-size: 14px;"
        );

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10);
        content.getChildren().add(new Label("Nouveau nom :"));
        content.getChildren().add(textField);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return textField.getText().trim();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newName -> {
            if (newName.isEmpty() || newName.length() < 3) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "⚠️ Le nom doit contenir au moins 3 caractères !");
                return;
            }

            try {
                role.setNomRole(newName);
                service.update(role);
                loadRoles();
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "✅ Rôle modifié avec succès !");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "❌ Erreur lors de la modification : " + e.getMessage());
            }
        });
    }

    // Supprimer un rôle
    private void deleteRole(Role role) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer le rôle");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer le rôle '" +
                role.getNomRole() + "' ?");

        if (confirmation.showAndWait().get() == ButtonType.OK) {
            try {
                service.delete(role);
                roles.remove(role);
                updateCountLabel();
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "✅ Rôle supprimé avec succès !");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "❌ Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    // Charger les rôles depuis la BD
    private void loadRoles() {
        try {
            roles.clear();
            roles.addAll(service.getAll());
            roleTable.setItems(roles);
            updateCountLabel();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "❌ Erreur lors du chargement : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Actualiser la liste
    @FXML
    private void handleRefresh() {
        loadRoles();
        showAlert(Alert.AlertType.INFORMATION, "Actualisation",
                "✅ Liste mise à jour avec succès !");
    }

    // Modifier (depuis le bouton principal)
    @FXML
    private void handleModify() {
        Role selectedRole = roleTable.getSelectionModel().getSelectedItem();

        if (selectedRole == null) {
            showAlert(Alert.AlertType.WARNING, "Aucune sélection",
                    "⚠️ Veuillez sélectionner un rôle à modifier.");
            return;
        }

        modifyRole(selectedRole);
    }

    // Supprimer (depuis le bouton principal)
    @FXML
    private void handleDelete() {
        Role selectedRole = roleTable.getSelectionModel().getSelectedItem();

        if (selectedRole == null) {
            showAlert(Alert.AlertType.WARNING, "Aucune sélection",
                    "⚠️ Veuillez sélectionner un rôle à supprimer.");
            return;
        }

        deleteRole(selectedRole);
    }

    // Retourner à la page d'ajout d'utilisateur
    @FXML
    private void handleBackToUsers() {
        try {
            // Charger la page d'ajout d'utilisateur
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user.fxml"));
            Parent root = loader.load();

            // Obtenir la scène actuelle
            Scene scene = nomRoleField.getScene();

            // Changer la vue de la scène actuelle
            scene.setRoot(root);

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de retourner à la gestion des utilisateurs : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Mettre à jour le compteur
    private void updateCountLabel() {
        if (countLabel != null) {
            countLabel.setText(roles.size() + " rôle(s)");
        }
    }

    // Validation en temps réel
    private void setupValidation() {
        nomRoleField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.trim().isEmpty()) {
                nomRoleField.setStyle("");
            } else if (newValue.trim().length() < 3) {
                nomRoleField.setStyle("-fx-border-color: #f97316; -fx-border-width: 2px;");
            } else {
                nomRoleField.setStyle("-fx-border-color: #10b981; -fx-border-width: 2px;");
            }
        });
    }

    // Afficher une alerte
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(
                "-fx-font-family: 'Roboto', 'Open Sans', sans-serif; " +
                        "-fx-font-size: 14px;"
        );

        alert.showAndWait();
    }
}
