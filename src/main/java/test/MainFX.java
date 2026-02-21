package test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
        Parent root = loader.load();

        // ✅ Taille adaptée au layout 2 colonnes du login
        Scene scene = new Scene(root, 1200, 700);

        stage.setTitle("LocalTrade — Plateforme Marketplace");
        stage.setScene(scene);

        // Taille minimale
        stage.setMinWidth(900);
        stage.setMinHeight(600);

        // Centrer à l'écran
        stage.centerOnScreen();

        stage.show();
    }
}