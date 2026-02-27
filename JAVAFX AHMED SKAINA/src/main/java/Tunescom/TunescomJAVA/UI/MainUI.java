package Tunescom.TunescomJAVA.UI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainUI extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        MainUI.primaryStage = primaryStage;
        Parent root = FXMLLoader.load(getClass().getResource("/produits/crudProduit.fxml"));

        Scene scene = new Scene(root, 900, 600);

        // Ajout explicite du stylesheet pour garantir son chargement
        String css = getClass().getResource("/produits/produit-style.css").toExternalForm();
        scene.getStylesheets().add(css);

        System.out.println("✅ CSS chargé depuis: " + css);
        System.out.println("✅ Stylesheets dans la scène: " + scene.getStylesheets());

        primaryStage.setScene(scene);
        primaryStage.setTitle("Gestion de Produits");
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}