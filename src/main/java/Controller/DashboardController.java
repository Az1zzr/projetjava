package Controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import models.User;
import services.UserService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class DashboardController {

    @FXML private Label lblTotalUsers;
    @FXML private Label lblTotalProduits;
    @FXML private Label lblTotalCommandes;
    @FXML private Label lblTotalFeedbacks;
    @FXML private Label lblDate;

    @FXML private TableView<User>           recentUsersTable;
    @FXML private TableColumn<User,Integer> colId;
    @FXML private TableColumn<User,String>  colNomComplet;
    @FXML private TableColumn<User,String>  colEmail;
    @FXML private TableColumn<User,String>  colRole;
    @FXML private TableColumn<User,String>  colDDN;

    private final UserService userService = new UserService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML public void initialize() {
        LocalDate today = LocalDate.now();
        String dayName   = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        String monthName = today.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        lblDate.setText(dayName + " " + today.getDayOfMonth() + " " + monthName + " " + today.getYear());
        setupTable();
        loadStats();
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setStyle("-fx-alignment: CENTER;");

        colNomComplet.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNomComplet()));

        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        // Rôle avec badge
        colRole.setCellValueFactory(c -> {
            models.Role r = c.getValue().getRole();
            return new SimpleStringProperty(r != null ? r.getNomRole() : "—");
        });
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(item);
                String style = switch (item.toLowerCase()) {
                    case "administrateur", "admin" -> "-fx-background-color: #eff6ff; -fx-text-fill: #1e3a5f;";
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

        colDDN.setCellValueFactory(c -> {
            LocalDate d = c.getValue().getDateNaissance();
            return new SimpleStringProperty(d != null ? d.format(FMT) : "—");
        });
        colDDN.setStyle("-fx-alignment: CENTER;");

        recentUsersTable.setStyle(
                "-fx-background-color: transparent; -fx-border-color: transparent; " +
                        "-fx-table-cell-border-color: #f1f5f9; -fx-font-size: 13px;");
        recentUsersTable.setFixedCellSize(50);
        recentUsersTable.setPrefHeight(50 * 5 + 40);
    }

    private void loadStats() {
        try {
            // Dashboard admin affiche les NON-admins (fournisseurs + entrepreneurs)
            List<User> users = userService.getNonAdmins();
            lblTotalUsers.setText(String.valueOf(users.size()));
            int size = users.size();
            List<User> recent = size > 5 ? users.subList(size - 5, size) : users;
            recentUsersTable.setItems(FXCollections.observableArrayList(recent));
        } catch (Exception e) {
            lblTotalUsers.setText("Err");
            e.printStackTrace();
        }
        lblTotalProduits.setText("—");
        lblTotalCommandes.setText("—");
        lblTotalFeedbacks.setText("—");
    }

    @FXML private void handleRefresh()      { loadStats(); }
    @FXML private void handleViewAllUsers() { /* navigation via sidebar */ }
}