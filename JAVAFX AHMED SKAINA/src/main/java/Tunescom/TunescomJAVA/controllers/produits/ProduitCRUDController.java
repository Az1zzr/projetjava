package Tunescom.TunescomJAVA.controllers.produits;

import Tunescom.TunescomJAVA.entities.produits.Produit;
import Tunescom.TunescomJAVA.services.produits.ProduitService;
import Tunescom.TunescomJAVA.tools.SelectedContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ProduitCRUDController {

    // TABLE
    @FXML private TableView<Produit> produitTable;
    @FXML private TableColumn<Produit, Integer> colId;
    @FXML private TableColumn<Produit, String> colNom;
    @FXML private TableColumn<Produit, String> colAdresse;
    @FXML private TableColumn<Produit, Double> colPrix;
    @FXML private TableColumn<Produit, Integer> colQuantite;
    @FXML private TableColumn<Produit, String> colImage;
    @FXML private TableColumn<Produit, Double> colPrixEUR;
    @FXML private TableColumn<Produit, Double> colPrixUSD;
    @FXML private TableColumn<Produit, Void> colDetail;

    // SEARCH
    @FXML private TextField searchField;

    // FORM
    @FXML private Label idLabel;
    @FXML private TextField nomField;
    @FXML private TextField adresseField;
    @FXML private TextField prixField;
    @FXML private TextField quantiteField;
    @FXML private Button uploadImageButton;
    @FXML private Label imageLabel;
    @FXML private Button addStockNavButton;
    @FXML private Button addStockForSelectedButton;

    @FXML private Label statusLabel;
    @FXML private ImageView qrImageView;

    private final ProduitService produitService = new ProduitService();
    private final ObservableList<Produit> data = FXCollections.observableArrayList();
    private Produit selected = null;
    private String uploadedImagePath = null;
    private double tauxEUR = 0;
    private double tauxUSD = 0;

    private final Path UPLOAD_DIR = Paths.get("uploads");

    @FXML
    public void initialize() {
        // bind columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nomProduit"));
        colAdresse.setCellValueFactory(new PropertyValueFactory<>("adresse"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colQuantite.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        colImage.setCellValueFactory(new PropertyValueFactory<>("imageURL"));

        // Affichage des images dans la colonne
        colImage.setCellFactory(column -> new TableCell<Produit, String>() {
            private final ImageView imageView = new ImageView();

            {
                imageView.setFitWidth(80);
                imageView.setFitHeight(60);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(String imagePath, boolean empty) {
                super.updateItem(imagePath, empty);

                if (empty || imagePath == null || imagePath.isEmpty()) {
                    setGraphic(null);
                } else {
                    try {
                        File file = new File(imagePath);
                        if (!file.exists()) {
                            file = new File("uploads/" + imagePath);
                        }

                        if (file.exists()) {
                            String fileUri = file.toURI().toString();
                            Image img = new Image(fileUri, 80, 60, true, true);
                            imageView.setImage(img);
                            setGraphic(imageView);
                        } else {
                            setGraphic(null);
                        }
                    } catch (Exception e) {
                        setGraphic(null);
                        e.printStackTrace();
                    }
                }
            }
        });

        // Colonne bouton "Détail" pour afficher l'image en grand
        colDetail.setCellFactory(column -> new TableCell<Produit, Void>() {
            private final Button detailButton = new Button("🔍 Détail");

            {
                detailButton.setStyle("-fx-cursor: hand; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 10;");
                detailButton.setOnAction(event -> {
                    Produit produit = getTableView().getItems().get(getIndex());
                    if (produit != null && produit.getImageURL() != null && !produit.getImageURL().isEmpty()) {
                        File file = new File(produit.getImageURL());
                        if (!file.exists()) {
                            file = new File("uploads/" + produit.getImageURL());
                        }
                        if (file.exists()) {
                            openImageWindow(file.toURI().toString(), produit.getImageURL());
                        } else {
                            alert("Erreur", "Image introuvable: " + produit.getImageURL());
                        }
                    } else {
                        alert("Erreur", "Aucune image associée à ce produit.");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(detailButton);
                }
            }
        });

        produitTable.setItems(data);

        // selection -> fill form
        produitTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            selected = newV;
            boolean hasSelection = newV != null;
            if (addStockNavButton != null) addStockNavButton.setDisable(!hasSelection);
            if (addStockForSelectedButton != null) addStockForSelectedButton.setDisable(!hasSelection);
            if (newV != null) fillForm(newV);
        });

        // créer le dossier uploads s'il n'existe pas
        try {
            if (!Files.exists(UPLOAD_DIR)) Files.createDirectories(UPLOAD_DIR);
        } catch (IOException e) {
            e.printStackTrace();
            setStatus("Erreur création dossier uploads: " + e.getMessage());
        }



        // Colonnes prix converti
        colPrixEUR.setCellValueFactory(cellData -> {
            double prix = cellData.getValue().getPrix();
            return new javafx.beans.property.SimpleDoubleProperty(prix * tauxEUR).asObject();
        });

        colPrixUSD.setCellValueFactory(cellData -> {
            double prix = cellData.getValue().getPrix();
            return new javafx.beans.property.SimpleDoubleProperty(prix * tauxUSD).asObject();
        });

        produitTable.setItems(data);

        // Selection listener
        produitTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            selected = newV;
            boolean hasSelection = newV != null;
            if (addStockNavButton != null) addStockNavButton.setDisable(!hasSelection);
            if (addStockForSelectedButton != null) addStockForSelectedButton.setDisable(!hasSelection);
            if (newV != null) fillForm(newV); // Formulaire peut rester inchangé
        });

        // Charger taux de conversion une seule fois
        tauxEUR = fetchRate("TND", "EUR");
        tauxUSD = fetchRate("TND", "USD");

        refresh();
        clearForm();
        if (addStockNavButton != null) addStockNavButton.setDisable(true);
        if (addStockForSelectedButton != null) addStockForSelectedButton.setDisable(true);
    }

    // ================= CRUD =================

    @FXML
    private void onAdd() {
        Produit p = readForm(false);
        if (p == null) return;

        produitService.ajouter(p);
        setStatus("✅ Produit ajouté (ID: " + p.getId() + ")");
        refresh();
        clearForm();
    }

    private double fetchRate(String from, String to) {
        try {
            // Construire l'URL FastForex (limité à EUR et USD)
            String apiUrl = "https://api.fastforex.io/fetch-multi?api_key=e753119b45-50aa8059a8-tb1hys&from=" + from + "&to=" + to;
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                if (json.has("results")) {
                    JsonObject results = json.getAsJsonObject("results");
                    if (results.has(to) && !results.get(to).isJsonNull()) {
                        return results.get(to).getAsDouble();
                    } else {
                        System.err.println("⚠️ Taux non disponible pour " + from + " -> " + to);
                        return 0;
                    }
                } else {
                    System.err.println("⚠️ Réponse invalide de l'API FastForex");
                    return 0;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("⚠️ Impossible de récupérer le taux " + from + " -> " + to);
            return 0; // fallback
        }
    }

    @FXML
    private void onUpdate() {
        if (selected == null) {
            alert("Sélection requise", "Sélectionne un produit dans le tableau.");
            return;
        }

        Produit p = readForm(true);
        if (p == null) return;

        p.setId(selected.getId());
        produitService.modifier(p, "");
        setStatus("✅ Produit modifié (ID: " + p.getId() + ")");
        refresh();
        selectRowById(p.getId());
    }

    @FXML
    private void onDelete() {
        if (selected == null) {
            alert("Sélection requise", "Sélectionne un produit dans le tableau.");
            return;
        }

        if (!confirm("Confirmation", "Supprimer le produit #" + selected.getId() + " ?")) return;

        produitService.supprimer(selected);
        setStatus("🗑️ Produit supprimé (ID: " + selected.getId() + ")");
        refresh();
        clearForm();
    }

    @FXML
    private void onClear() {
        clearForm();
        setStatus("Formulaire vidé.");
    }

    @FXML
    private void onRefresh() {
        refresh();
        setStatus("✅ Liste actualisée.");
    }

    @FXML
    private void onSearch() {
        String q = safe(searchField.getText()).trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            refresh();
            return;
        }

        var filtered = produitService.recuperer().stream()
                .filter(p -> safe(p.getNomProduit()).toLowerCase(Locale.ROOT).contains(q)
                        || safe(p.getAdresse()).toLowerCase(Locale.ROOT).contains(q))
                .collect(Collectors.toList());

        data.setAll(filtered);
        setStatus("🔎 " + filtered.size() + " résultat(s).");
    }

    @FXML
    private void onAddStockForSelected() {
        if (selected == null) {
            alert("Sélection requise", "Sélectionne un produit dans le tableau.");
            return;
        }

        SelectedContext.setSelectedProduitId(selected.getId());
        Stage stage = openWindow("/produits/crudStock.fxml", "Gestion Stock");
        if (stage != null) stage.setOnHidden(e -> refresh());
    }

    // ================= Upload Image =================

    @FXML
    private void onUploadImage() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = fc.showOpenDialog(uploadImageButton.getScene().getWindow());

        if (file != null) {
            try {
                String filename = System.currentTimeMillis() + "_" + file.getName();
                Path target = UPLOAD_DIR.resolve(filename);
                Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
                uploadedImagePath = "uploads/" + filename;
                imageLabel.setText(filename);
                setStatus("✅ Image uploadée: " + filename);
            } catch (IOException e) {
                e.printStackTrace();
                alert("Erreur upload", "Impossible de copier le fichier.\n" + e.getMessage());
            }
        }
    }

    // ===== Navigation stubs =====
    @FXML private void onAccueil() { setStatus("ACCUEIL (à brancher)"); }
    @FXML
    private void onOpenStock() {
        if (selected == null) {
            alert("Sélection requise", "Sélectionne un produit dans le tableau.");
            return;
        }
        SelectedContext.setSelectedProduitId(selected.getId());
        Stage stage = openWindow("/produits/crudStock.fxml", "Gestion Stock");
        if (stage != null) stage.setOnHidden(e -> refresh());
        setStatus("✅ Ouverture gestion stock.");
    }
    @FXML private void onRetour() { setStatus("RETOUR (à brancher)"); }
    @FXML private void onAvecStock() { setStatus("AVEC STOCK (à brancher)"); }
    @FXML private void onFocusForm() { nomField.requestFocus(); }

    // ================= Helpers =================

    private void refresh() { data.setAll(produitService.recuperer()); }

    private void fillForm(Produit p) {
        idLabel.setText(String.valueOf(p.getId()));
        nomField.setText(safe(p.getNomProduit()));
        adresseField.setText(safe(p.getAdresse()));
        prixField.setText(String.valueOf(p.getPrix()));
        quantiteField.setText(String.valueOf(p.getQuantite()));
        uploadedImagePath = safe(p.getImageURL());
        imageLabel.setText(uploadedImagePath != null ? new File(uploadedImagePath).getName() : "");
    }

    private void clearForm() {
        selected = null;
        produitTable.getSelectionModel().clearSelection();
        idLabel.setText("(auto)");
        nomField.clear();
        adresseField.clear();
        prixField.clear();
        quantiteField.clear();
        uploadedImagePath = null;
        imageLabel.setText("");
    }

    private Produit readForm(boolean isUpdate) {
        String nom = safe(nomField.getText()).trim();
        String adresse = safe(adresseField.getText()).trim();
        String prixStr = safe(prixField.getText()).trim();
        String qteStr = safe(quantiteField.getText()).trim();

        if (nom.isEmpty() || adresse.isEmpty() || prixStr.isEmpty() || qteStr.isEmpty()) {
            alert("Champs obligatoires", "Nom, Adresse, Prix et Quantité sont obligatoires.");
            return null;
        }

        double prix;
        int qte;

        try { prix = Double.parseDouble(prixStr.replace(",", ".")); }
        catch (Exception e) { alert("Prix invalide", "Prix doit être un nombre (ex: 50.00)"); return null; }

        try { qte = Integer.parseInt(qteStr); }
        catch (Exception e) { alert("Quantité invalide", "Quantité doit être un entier (ex: 10)"); return null; }

        if (prix < 0 || qte < 0) {
            alert("Valeurs invalides", "Prix et Quantité doivent être >= 0.");
            return null;
        }

        return new Produit(nom, adresse, prix, qte, uploadedImagePath);
    }

    private void selectRowById(int id) {
        for (Produit p : data) {
            if (p.getId() == id) {
                produitTable.getSelectionModel().select(p);
                produitTable.scrollTo(p);
                break;
            }
        }
    }

    private void setStatus(String msg) { if (statusLabel != null) statusLabel.setText(msg); }

    private void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private boolean confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        return a.showAndWait().filter(btn -> btn == ButtonType.OK).isPresent();
    }

    private String safe(String s) { return s == null ? "" : s; }

    private Stage openWindow(String fxmlPath, String title) {
        try {
            var url = getClass().getResource(fxmlPath);
            if (url == null) throw new IllegalStateException("FXML introuvable: " + fxmlPath);
            var root = javafx.fxml.FXMLLoader.load(url);
            var stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new javafx.scene.Scene((Parent) root));
            stage.show();
            return stage;
        } catch (Exception e) {
            e.printStackTrace();
            alert("Erreur", "Impossible d'ouvrir la page stock.\n" + e.getMessage());
            return null;
        }
    }


    @FXML
    private void onGenerateQR() {
        if (selected == null) {
            alert("Sélection requise", "Sélectionne un produit dans le tableau.");
            return;
        }

        try {
            // Texte du QR code : ID + nom du produit
            String qrText = "Produit#" + selected.getId() + " - " + selected.getNomProduit();
            String encoded = URLEncoder.encode(qrText, StandardCharsets.UTF_8);
            String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" + encoded;

            Image qrImage = new Image(qrUrl);

            // Création d'une nouvelle fenêtre pour afficher le QR
            ImageView qrView = new ImageView(qrImage);
            qrView.setFitWidth(300);
            qrView.setFitHeight(300);
            qrView.setPreserveRatio(true);

            Stage qrStage = new Stage();
            qrStage.setTitle("QR Code - Produit #" + selected.getId());
            qrStage.setScene(new javafx.scene.Scene(new javafx.scene.layout.StackPane(qrView), 320, 320));
            qrStage.show();

            setStatus("✅ QR code ouvert pour le produit #" + selected.getId());

        } catch (Exception e) {
            e.printStackTrace();
            alert("Erreur QR", "Impossible de générer le QR code.\n" + e.getMessage());
        }
    }


    @FXML
    private void onShowStats() {
        try {
            // GridPane pour organiser les stats
            GridPane grid = new GridPane();
            grid.setPadding(new Insets(20));
            grid.setHgap(20);
            grid.setVgap(12);
            grid.setStyle("-fx-background-color: #f5f5f5; -fx-font-size: 14px;");

            int row = 0;

            // Valeur totale du stock
            double totalValue = data.stream()
                    .mapToDouble(p -> p.getPrix() * p.getQuantite())
                    .sum();
            Label totalValueLabel = new Label("💰 Valeur totale du stock:");
            Label totalValueValue = new Label(String.format("%.2f TND", totalValue));
            totalValueValue.setStyle("-fx-font-weight: bold;");
            grid.add(totalValueLabel, 0, row);
            grid.add(totalValueValue, 1, row++);

            // Produit le plus cher
            Produit mostExpensive = data.stream()
                    .max((p1, p2) -> Double.compare(p1.getPrix(), p2.getPrix()))
                    .orElse(null);
            Label mostExpLabel = new Label("🏆 Produit le plus cher:");
            Label mostExpValue = new Label(mostExpensive != null ? mostExpensive.getNomProduit() + " – " + mostExpensive.getPrix() + " TND" : "N/A");
            mostExpValue.setStyle("-fx-font-weight: bold;");
            grid.add(mostExpLabel, 0, row);
            grid.add(mostExpValue, 1, row++);

            // Quantité totale
            int totalQuantity = data.stream().mapToInt(Produit::getQuantite).sum();
            Label totalQtyLabel = new Label("📦 Quantité totale:");
            Label totalQtyValue = new Label(String.valueOf(totalQuantity));
            totalQtyValue.setStyle("-fx-font-weight: bold;");
            grid.add(totalQtyLabel, 0, row);
            grid.add(totalQtyValue, 1, row++);

            // Moyenne du prix
            double avgPrice = data.stream().mapToDouble(Produit::getPrix).average().orElse(0);
            Label avgPriceLabel = new Label("📊 Prix moyen par produit:");
            Label avgPriceValue = new Label(String.format("%.2f TND", avgPrice));
            avgPriceValue.setStyle("-fx-font-weight: bold;");
            grid.add(avgPriceLabel, 0, row);
            grid.add(avgPriceValue, 1, row++);

            // Répartition par adresse
            Map<String, Long> byAddress = data.stream()
                    .collect(Collectors.groupingBy(Produit::getAdresse, Collectors.counting()));
            Label repartitionLabel = new Label("📍 Répartition par adresse:");
            VBox addressBox = new VBox(4);
            byAddress.forEach((addr, count) -> {
                Label l = new Label(addr + " : " + count + " produit(s)");
                l.setStyle("-fx-font-weight: bold;");
                addressBox.getChildren().add(l);
            });
            grid.add(repartitionLabel, 0, row);
            grid.add(addressBox, 1, row++);

            // Fenêtre
            Stage stage = new Stage();
            stage.setTitle("📊 Statistiques Produits");
            Scene scene = new Scene(grid, 500, 400); // taille plus grande
            stage.setScene(scene);
            stage.setResizable(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            alert("Erreur", "Impossible d'afficher les statistiques.\n" + e.getMessage());
        }
    }

    // Méthode pour ouvrir l'image en grand dans une nouvelle fenêtre
    private void openImageWindow(String imageUri, String imagePath) {
        try {
            // Créer une ImageView avec l'image en taille réelle
            ImageView largeImageView = new ImageView();
            Image largeImage = new Image(imageUri);
            largeImageView.setImage(largeImage);
            largeImageView.setPreserveRatio(true);

            // Limiter la taille maximale si l'image est très grande
            double maxWidth = 800;
            double maxHeight = 600;

            if (largeImage.getWidth() > maxWidth || largeImage.getHeight() > maxHeight) {
                largeImageView.setFitWidth(maxWidth);
                largeImageView.setFitHeight(maxHeight);
            }

            // Conteneur avec style
            javafx.scene.layout.StackPane pane = new javafx.scene.layout.StackPane(largeImageView);
            pane.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 20;");

            // Créer la fenêtre
            Stage imageStage = new Stage();
            imageStage.setTitle("📷 Image Produit - " + new File(imagePath).getName());

            Scene scene = new Scene(pane);
            imageStage.setScene(scene);
            imageStage.sizeToScene();
            imageStage.setResizable(true);
            imageStage.show();

            setStatus("✅ Image affichée en grand");

        } catch (Exception e) {
            e.printStackTrace();
            alert("Erreur", "Impossible d'afficher l'image.\n" + e.getMessage());
        }
    }
}

