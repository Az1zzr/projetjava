package utils;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.File;
import java.util.function.Consumer;

public class ImageSafetyGuard {

    /**
     * Ouvre un FileChooser, vérifie l'image via Claude Vision,
     * appelle onAccepted(file) UNIQUEMENT si l'image est SAFE ou confirmée.
     *
     * @param ownerWindow  fenêtre parente
     * @param imgView      ImageView à remplir si SAFE
     * @param lblInitial   Label initiale à cacher
     * @param onAccepted   callback appelé APRÈS vérification si image acceptée
     */
    public static void pickAndCheck(Window ownerWindow,
                                    ImageView imgView,
                                    Label lblInitial,
                                    Consumer<File> onAccepted) {

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image de profil");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp"));

        File file = chooser.showOpenDialog(ownerWindow);
        if (file == null) return; // annulé

        // Lancer la vérification — le callback onAccepted n'est appelé que si SAFE
        checkAndDisplay(file, imgView, lblInitial, ownerWindow, onAccepted);
    }

    /**
     * Vérifie une image (asynchrone).
     * - SAFE    → affiche l'image + appelle onAccepted
     * - WARNING → demande confirmation, si oui → affiche + appelle onAccepted
     * - BLOCKED → affiche la page d'alerte, onAccepted jamais appelé
     */
    public static void checkAndDisplay(File imageFile,
                                       ImageView imgView,
                                       Label lblInitial,
                                       Window ownerWindow,
                                       Consumer<File> onAccepted) {

        // Page "Analyse en cours..."
        Stage loadingStage = showLoadingPage(ownerWindow);

        Task<SensitiveImageDetector.Result> task = new Task<>() {
            @Override
            protected SensitiveImageDetector.Result call() throws Exception {
                return SensitiveImageDetector.analyze(imageFile);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (loadingStage != null) loadingStage.close();

            SensitiveImageDetector.Result result = task.getValue();

            switch (result.getLevel()) {
                case SAFE -> {
                    // ✅ Image OK → afficher et notifier
                    applyPhoto(imageFile, imgView, lblInitial);
                    if (onAccepted != null) onAccepted.accept(imageFile);
                }
                case WARNING -> {
                    // ⚠️ Demander confirmation
                    showWarningPage(ownerWindow, result.getReason(), imageFile,
                            imgView, lblInitial, onAccepted);
                }
                case BLOCKED -> {
                    // 🚫 Bloquée → page d'alerte, onAccepted jamais appelé
                    showBlockedPage(ownerWindow, result.getReason());
                }
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            if (loadingStage != null) loadingStage.close();
            showBlockedPage(ownerWindow,
                    "Impossible de vérifier l'image. Veuillez réessayer.");
        }));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    // ══════════════════════════════════════════════════════════
    // Page : Analyse en cours
    // ══════════════════════════════════════════════════════════
    private static Stage showLoadingPage(Window owner) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        if (owner != null) stage.initOwner(owner);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0d1b2e; -fx-background-radius: 16px; " +
                "-fx-padding: 40 60; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.5),20,0,0,5);");

        Label icon  = new Label("🔍");
        icon.setStyle("-fx-font-size: 48px;");
        Label title = new Label("Analyse en cours...");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label sub   = new Label("Vérification par le modèle de modération");
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #7a9ab8;");

        root.getChildren().addAll(icon, title, sub);
        Scene scene = new Scene(root);
        scene.setFill(null);
        stage.setScene(scene);
        stage.show();
        return stage;
    }

    // ══════════════════════════════════════════════════════════
    // Page : Image BLOQUÉE
    // ══════════════════════════════════════════════════════════
    private static void showBlockedPage(Window owner, String reason) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        if (owner != null) stage.initOwner(owner);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0d1b2e; -fx-background-radius: 20px; " +
                "-fx-padding: 50 70; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.6),30,0,0,8);");

        Label icon = new Label("🚫");
        icon.setStyle("-fx-font-size: 64px;");

        Label title = new Label("Image sensible détectée !");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #ef4444;");

        HBox sep = new HBox();
        sep.setStyle("-fx-background-color: #ef4444; -fx-pref-height: 2px; -fx-pref-width: 300px; -fx-background-radius: 2px;");
        sep.setAlignment(Pos.CENTER);

        Label message = new Label("Cette image a été refusée par notre\nsystème de modération automatique.");
        message.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-text-alignment: center;");
        message.setAlignment(Pos.CENTER);

        Label reasonLbl = new Label("⚠  " + reason);
        reasonLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #fbbf24; " +
                "-fx-background-color: rgba(251,191,36,0.1); -fx-background-radius: 10px; " +
                "-fx-padding: 12 20; -fx-border-color: rgba(251,191,36,0.3); " +
                "-fx-border-radius: 10px; -fx-border-width: 1px;");
        reasonLbl.setWrapText(true);
        reasonLbl.setMaxWidth(320);
        reasonLbl.setAlignment(Pos.CENTER);

        Label conseil = new Label("S'il vous plaît, choisissez une autre image\nappropriée pour votre profil.");
        conseil.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-text-alignment: center;");
        conseil.setAlignment(Pos.CENTER);

        Button btnClose = new Button("📷  Choisir une autre image");
        btnClose.setStyle("-fx-background-color: #f97316; -fx-text-fill: white; -fx-font-size: 14px; " +
                "-fx-font-weight: bold; -fx-background-radius: 12px; -fx-padding: 12 30; -fx-cursor: hand;");
        btnClose.setOnAction(e -> stage.close());
        btnClose.setOnMouseEntered(e -> btnClose.setStyle(
                "-fx-background-color: #ea6c0a; -fx-text-fill: white; -fx-font-size: 14px; " +
                        "-fx-font-weight: bold; -fx-background-radius: 12px; -fx-padding: 12 30; -fx-cursor: hand;"));
        btnClose.setOnMouseExited(e -> btnClose.setStyle(
                "-fx-background-color: #f97316; -fx-text-fill: white; -fx-font-size: 14px; " +
                        "-fx-font-weight: bold; -fx-background-radius: 12px; -fx-padding: 12 30; -fx-cursor: hand;"));

        root.getChildren().addAll(icon, title, sep, message, reasonLbl, conseil, btnClose);

        Scene scene = new Scene(root);
        scene.setFill(null);
        stage.setScene(scene);
        stage.showAndWait();
    }

    // ══════════════════════════════════════════════════════════
    // Page : Image WARNING
    // ══════════════════════════════════════════════════════════
    private static void showWarningPage(Window owner, String reason,
                                        File imageFile, ImageView imgView,
                                        Label lblInitial, Consumer<File> onAccepted) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        if (owner != null) stage.initOwner(owner);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0d1b2e; -fx-background-radius: 20px; " +
                "-fx-padding: 50 70; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.6),30,0,0,8);");

        Label icon = new Label("⚠️");
        icon.setStyle("-fx-font-size: 64px;");

        Label title = new Label("Image potentiellement sensible");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #fbbf24;");

        Label reasonLbl = new Label("⚠  " + reason);
        reasonLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #fbbf24; " +
                "-fx-background-color: rgba(251,191,36,0.1); -fx-background-radius: 10px; " +
                "-fx-padding: 12 20; -fx-border-color: rgba(251,191,36,0.3); " +
                "-fx-border-radius: 10px; -fx-border-width: 1px;");
        reasonLbl.setWrapText(true);
        reasonLbl.setMaxWidth(320);

        Label question = new Label("Voulez-vous quand même utiliser cette image ?");
        question.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");

        HBox buttons = new HBox(16);
        buttons.setAlignment(Pos.CENTER);

        Button btnYes = new Button("✓  Oui, utiliser");
        btnYes.setStyle("-fx-background-color: #f97316; -fx-text-fill: white; -fx-font-size: 13px; " +
                "-fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 10 24; -fx-cursor: hand;");
        btnYes.setOnAction(e -> {
            applyPhoto(imageFile, imgView, lblInitial);
            if (onAccepted != null) onAccepted.accept(imageFile); // ✅ notifier seulement si confirmé
            stage.close();
        });

        Button btnNo = new Button("✗  Choisir une autre");
        btnNo.setStyle("-fx-background-color: transparent; -fx-text-fill: #7a9ab8; -fx-font-size: 13px; " +
                "-fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 10 24; " +
                "-fx-border-color: #1a2d47; -fx-border-radius: 10px; -fx-border-width: 1px; -fx-cursor: hand;");
        btnNo.setOnAction(e -> stage.close());

        buttons.getChildren().addAll(btnYes, btnNo);
        root.getChildren().addAll(icon, title, reasonLbl, question, buttons);

        Scene scene = new Scene(root);
        scene.setFill(null);
        stage.setScene(scene);
        stage.showAndWait();
    }

    // ══════════════════════════════════════════════════════════
    // Afficher la photo dans l'ImageView
    // ══════════════════════════════════════════════════════════
    private static void applyPhoto(File file, ImageView imgView, Label lblInitial) {
        Platform.runLater(() -> {
            try {
                imgView.setImage(new Image(file.toURI().toString()));
                imgView.setVisible(true);
                imgView.setManaged(true);
                if (lblInitial != null) {
                    lblInitial.setVisible(false);
                    lblInitial.setManaged(false);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}