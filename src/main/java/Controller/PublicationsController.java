package Controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import models.User;
import utils.SessionManager;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class PublicationsController {

    @FXML private Label     lblCreateAvatar;
    @FXML private ImageView imgCreateAvatar;
    @FXML private TextArea  taNewPost;
    @FXML private Label     lblCharCount;
    @FXML private StackPane imagePreviewPane;
    @FXML private ImageView imgPreview;
    @FXML private VBox      feedBox;

    private String selectedImagePath = null;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ══════════════════════════════════════════════════════════════════════════
    // Modèle Publication
    // ══════════════════════════════════════════════════════════════════════════
    private static class Publication {
        int    id;
        int    auteurId;        // ✅ ID unique de l'auteur
        String auteurNom;
        String auteurRole;
        String auteurInitiale;
        String auteurPhoto;
        String contenu;
        String imagePath;
        LocalDateTime dateHeure;

        // ✅ Set d'IDs des users qui ont liké (1 like par user)
        Set<Integer> likedByUsers = new HashSet<>();

        List<Commentaire> commentaires = new ArrayList<>();

        Publication(int id, int auteurId, String nom, String role,
                    String init, String photo, String contenu, String img) {
            this.id = id; this.auteurId = auteurId;
            this.auteurNom = nom; this.auteurRole = role;
            this.auteurInitiale = init; this.auteurPhoto = photo;
            this.contenu = contenu; this.imagePath = img;
            this.dateHeure = LocalDateTime.now();
        }

        int    getLikes()           { return likedByUsers.size(); }
        boolean likedBy(int userId) { return likedByUsers.contains(userId); }
        void toggleLike(int userId) {
            if (likedByUsers.contains(userId)) likedByUsers.remove(userId);
            else likedByUsers.add(userId);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Modèle Commentaire
    // ══════════════════════════════════════════════════════════════════════════
    private static class Commentaire {
        int    auteurId;
        String auteurNom;
        String auteurInitiale;
        String auteurPhoto;
        String texte;
        LocalDateTime date;

        // ✅ Likes par commentaire (Set d'IDs)
        Set<Integer> likedByUsers = new HashSet<>();

        // ✅ Réponses (sous-commentaires)
        List<Commentaire> reponses = new ArrayList<>();

        Commentaire(int auteurId, String n, String i, String photo, String t) {
            this.auteurId = auteurId; this.auteurNom = n;
            this.auteurInitiale = i; this.auteurPhoto = photo;
            this.texte = t; this.date = LocalDateTime.now();
        }

        int     getLikes()           { return likedByUsers.size(); }
        boolean likedBy(int userId)  { return likedByUsers.contains(userId); }
        void    toggleLike(int userId) {
            if (likedByUsers.contains(userId)) likedByUsers.remove(userId);
            else likedByUsers.add(userId);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Storage statique (session)
    // ══════════════════════════════════════════════════════════════════════════
    private static final List<Publication> PUBLICATIONS = new ArrayList<>();
    private static int nextId = 1;

    // ══════════════════════════════════════════════════════════════════════════
    // Init
    // ══════════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        setupCreatorAvatar(user);

        taNewPost.textProperty().addListener((o, ov, nv) -> {
            int len = nv.length();
            lblCharCount.setText(len + " / 500");
            lblCharCount.setStyle("-fx-font-size: 11px; " +
                    (len > 450 ? "-fx-text-fill: #ef4444;" : "-fx-text-fill: #94a3b8;"));
        });

        refreshFeed();
    }

    private void setupCreatorAvatar(User user) {
        String display = buildDisplayName(user);
        String init    = initiale(display);
        lblCreateAvatar.setText(init);

        if (hasPhoto(user)) {
            try {
                imgCreateAvatar.setImage(new Image(new File(user.getPhotoPath()).toURI().toString()));
                imgCreateAvatar.setVisible(true);  imgCreateAvatar.setManaged(true);
                lblCreateAvatar.setVisible(false); lblCreateAvatar.setManaged(false);
            } catch (Exception ignored) {}
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Actions image
    // ══════════════════════════════════════════════════════════════════════════
    @FXML private void handleAddImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images","*.png","*.jpg","*.jpeg","*.gif","*.bmp"));
        File f = fc.showOpenDialog(taNewPost.getScene().getWindow());
        if (f == null) return;
        selectedImagePath = f.getAbsolutePath();
        try {
            imgPreview.setImage(new Image(f.toURI().toString()));
            imagePreviewPane.setVisible(true); imagePreviewPane.setManaged(true);
        } catch (Exception ignored) {}
    }

    @FXML private void handleRemoveImage() {
        selectedImagePath = null;
        imgPreview.setImage(null);
        imagePreviewPane.setVisible(false); imagePreviewPane.setManaged(false);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Publier
    // ══════════════════════════════════════════════════════════════════════════
    @FXML private void handlePublier() {
        String texte = taNewPost.getText().trim();
        if (texte.isEmpty() && selectedImagePath == null) {
            showAlert("Écrivez quelque chose ou ajoutez une image avant de publier."); return;
        }
        if (texte.length() > 500) { showAlert("Maximum 500 caractères."); return; }

        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        String display = buildDisplayName(user);
        String role    = user.getRole() != null ? user.getRole().getNomRole() : "";

        PUBLICATIONS.add(0, new Publication(
                nextId++, user.getId(), display, role,
                initiale(display), user.getPhotoPath(), texte, selectedImagePath
        ));

        taNewPost.clear();
        handleRemoveImage();
        lblCharCount.setText("0 / 500");
        refreshFeed();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tri
    // ══════════════════════════════════════════════════════════════════════════
    @FXML private void trierRecents()    {
        refreshFeedSorted(Comparator.comparing((Publication p) -> p.dateHeure, Comparator.reverseOrder()));
    }
    @FXML private void trierPopulaires() {
        refreshFeedSorted(Comparator.comparingInt(Publication::getLikes).reversed());
    }

    private void refreshFeed() { trierRecents(); }

    private void refreshFeedSorted(Comparator<Publication> cmp) {
        feedBox.getChildren().clear();

        if (PUBLICATIONS.isEmpty()) {
            VBox empty = new VBox(10); empty.setAlignment(Pos.CENTER);
            empty.setStyle("-fx-padding: 60;");
            Label icon = new Label("📰"); icon.setStyle("-fx-font-size: 48px;");
            Label txt  = new Label("Aucune publication.\nSoyez le premier à partager !");
            txt.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-text-alignment: center;");
            txt.setWrapText(true);
            empty.getChildren().addAll(icon, txt);
            feedBox.getChildren().add(empty);
            return;
        }

        PUBLICATIONS.stream().sorted(cmp)
                .forEach(p -> feedBox.getChildren().add(buildCard(p)));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Carte publication
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildCard(Publication pub) {
        User me   = SessionManager.getInstance().getCurrentUser();
        int  myId = me != null ? me.getId() : -1;

        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 18px;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),10,0,0,3);");

        String barColor = pub.auteurRole.trim().toLowerCase().startsWith("fournisseur")
                ? "#f97316" : "#10b981";
        Region topBar = new Region();
        topBar.setStyle("-fx-background-color:" + barColor +
                "; -fx-pref-height:4px; -fx-background-radius:18 18 0 0;");
        card.getChildren().add(topBar);

        VBox body = new VBox(14);
        body.setStyle("-fx-padding: 16 20;");

        // ── Header auteur ──
        HBox header = new HBox(12); header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().add(buildAvatar(pub.auteurNom, pub.auteurInitiale, pub.auteurPhoto, barColor, 44));

        VBox authorInfo = new VBox(3);
        Label nameLabel = new Label(pub.auteurNom.isEmpty() ? "Utilisateur" : pub.auteurNom);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label roleLabel = new Label(pub.auteurRole);
        roleLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;" +
                "-fx-text-fill: " + barColor + ";" +
                "-fx-background-color: " + barColor.replace("#", "rgba(") + "," + (barColor.equals("#f97316") ? "0.1);" : "0.1);") +
                "-fx-background-radius: 20px; -fx-padding: 2 10;");
        authorInfo.getChildren().addAll(nameLabel, roleLabel);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label dateLbl = new Label(pub.dateHeure.format(FMT));
        dateLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        header.getChildren().addAll(authorInfo, sp, dateLbl);

        // Bouton supprimer si c'est mon post
        if (pub.auteurId == myId) {
            Button del = new Button("🗑️");
            del.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444;" +
                    "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 6;");
            del.setOnAction(e -> { PUBLICATIONS.remove(pub); refreshFeed(); });
            header.getChildren().add(del);
        }

        body.getChildren().add(header);

        // ── Contenu texte ──
        if (!pub.contenu.isEmpty()) {
            Label content = new Label(pub.contenu);
            content.setWrapText(true);
            content.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151; -fx-line-spacing: 2;");
            body.getChildren().add(content);
        }

        // ── Image du post ──
        if (pub.imagePath != null) {
            File imgF = new File(pub.imagePath);
            if (imgF.exists()) {
                try {
                    ImageView iv = new ImageView(new Image(imgF.toURI().toString()));
                    iv.setFitWidth(660); iv.setPreserveRatio(true);
                    body.getChildren().add(iv);
                } catch (Exception ignored) {}
            }
        }

        // ── Séparateur ──
        body.getChildren().add(separator());

        // ── Stats (likes) ──
        HBox stats = new HBox(16); stats.setAlignment(Pos.CENTER_LEFT);
        Label likeStat = new Label();
        updateLikeStat(likeStat, pub);
        Label commentStat = new Label(pub.commentaires.size() + " commentaire(s)");
        commentStat.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        stats.getChildren().addAll(likeStat, commentStat);
        body.getChildren().add(stats);

        body.getChildren().add(separator());

        // ── Boutons actions ──
        HBox actions = new HBox(6); actions.setAlignment(Pos.CENTER_LEFT);

        Button likeBtn = buildLikePostButton(pub, myId, likeStat);
        Button commentBtn = new Button("💬  Commenter");
        commentBtn.setStyle(btnStyle("#f8fafc", "#64748b"));

        actions.getChildren().addAll(likeBtn, commentBtn);
        body.getChildren().add(actions);

        // ── Zone commentaires ──
        VBox commentsBox = new VBox(12);
        renderComments(pub, commentsBox, commentStat, myId);

        // ── Input commentaire (toggle) ──
        VBox commentInput = buildCommentInput(pub, commentsBox, commentStat, myId);
        commentInput.setVisible(false); commentInput.setManaged(false);

        commentBtn.setOnAction(e -> {
            boolean show = !commentInput.isVisible();
            commentInput.setVisible(show); commentInput.setManaged(show);
        });

        body.getChildren().addAll(commentsBox, commentInput);
        card.getChildren().add(body);
        return card;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Bouton like du POST
    // ══════════════════════════════════════════════════════════════════════════
    private Button buildLikePostButton(Publication pub, int myId, Label likeStat) {
        Button btn = new Button();
        refreshLikePostBtn(btn, pub, myId);
        btn.setOnAction(e -> {
            pub.toggleLike(myId);           // ✅ toggle per-user
            refreshLikePostBtn(btn, pub, myId);
            updateLikeStat(likeStat, pub);
        });
        return btn;
    }

    private void refreshLikePostBtn(Button btn, Publication pub, int myId) {
        boolean liked = pub.likedBy(myId);
        btn.setText((liked ? "❤️" : "🤍") + "  J'aime");
        btn.setStyle((liked
                ? "-fx-background-color:#fef2f2; -fx-text-fill:#ef4444; -fx-border-color:#fecaca;"
                : "-fx-background-color:#f8fafc; -fx-text-fill:#64748b; -fx-border-color:#e2e8f0;") +
                "-fx-border-width:1px; -fx-border-radius:8px; -fx-background-radius:8px;" +
                "-fx-font-size:12px; -fx-padding:8 16; -fx-cursor:hand;");
    }

    private void updateLikeStat(Label lbl, Publication pub) {
        int n = pub.getLikes();
        lbl.setText(n == 0 ? "" : "❤️ " + n + (n == 1 ? " J'aime" : " J'aimes"));
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #ef4444;");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Afficher les commentaires
    // ══════════════════════════════════════════════════════════════════════════
    private void renderComments(Publication pub, VBox box, Label counter, int myId) {
        box.getChildren().clear();
        counter.setText(pub.commentaires.size() + " commentaire(s)");
        for (Commentaire c : pub.commentaires)
            box.getChildren().add(buildCommentItem(pub, c, commentsBox -> {
                renderComments(pub, box, counter, myId);
            }, counter, myId, false));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Carte commentaire (avec like + répondre)
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildCommentItem(Publication pub, Commentaire c,
                                  java.util.function.Consumer<VBox> onUpdate,
                                  Label counter, int myId, boolean isReply) {
        VBox wrapper = new VBox(6);
        wrapper.setStyle(isReply
                ? "-fx-padding: 0 0 0 40;"   // indentation pour les réponses
                : "-fx-padding: 0;");

        HBox row = new HBox(10); row.setAlignment(Pos.TOP_LEFT);

        // Avatar commentaire
        StackPane avt = buildAvatar(c.auteurNom, c.auteurInitiale, c.auteurPhoto, "#1e3a5f", isReply ? 28 : 34);

        VBox right = new VBox(4); HBox.setHgrow(right, Priority.ALWAYS);

        // Bulle du commentaire
        VBox bubble = new VBox(4);
        bubble.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 0 12 12 12;" +
                "-fx-padding: 10 14;");
        Label cName = new Label(c.auteurNom.isEmpty() ? "Utilisateur" : c.auteurNom);
        cName.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label cTxt  = new Label(c.texte);
        cTxt.setWrapText(true);
        cTxt.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151;");
        bubble.getChildren().addAll(cName, cTxt);
        right.getChildren().add(bubble);

        // ── Barre actions commentaire ──
        HBox cActions = new HBox(14); cActions.setAlignment(Pos.CENTER_LEFT);

        // Date
        Label cDate = new Label(c.date.format(FMT));
        cDate.setStyle("-fx-font-size: 10px; -fx-text-fill: #cbd5e1;");

        // ✅ Like commentaire (per-user)
        Button cLikeBtn = new Button();
        refreshCommentLikeBtn(cLikeBtn, c, myId);
        cLikeBtn.setOnAction(e -> {
            c.toggleLike(myId);
            refreshCommentLikeBtn(cLikeBtn, c, myId);
        });

        cActions.getChildren().addAll(cDate, cLikeBtn);

        // ✅ Bouton Répondre (seulement pour les commentaires principaux)
        if (!isReply) {
            Button replyBtn = new Button("↩ Répondre");
            replyBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b;" +
                    "-fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 2 4;");

            VBox replyInput = buildReplyInput(pub, c, onUpdate, counter, myId);
            replyInput.setVisible(false); replyInput.setManaged(false);

            replyBtn.setOnAction(e -> {
                boolean show = !replyInput.isVisible();
                replyInput.setVisible(show); replyInput.setManaged(show);
            });

            cActions.getChildren().add(replyBtn);
            right.getChildren().addAll(cActions, replyInput);

            // Réponses existantes
            if (!c.reponses.isEmpty()) {
                VBox repliesBox = new VBox(8);
                for (Commentaire rep : c.reponses)
                    repliesBox.getChildren().add(
                            buildCommentItem(pub, rep, onUpdate, counter, myId, true));
                right.getChildren().add(repliesBox);
            }
        } else {
            right.getChildren().add(cActions);
        }

        row.getChildren().addAll(avt, right);
        wrapper.getChildren().add(row);
        return wrapper;
    }

    private void refreshCommentLikeBtn(Button btn, Commentaire c, int myId) {
        boolean liked = c.likedBy(myId);
        int     n     = c.getLikes();
        btn.setText((liked ? "❤️ " : "🤍 ") + (n > 0 ? n : ""));
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 11px;" +
                "-fx-text-fill: " + (liked ? "#ef4444;" : "#94a3b8;") + " -fx-padding: 2 4;");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Input nouveau commentaire principal
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildCommentInput(Publication pub, VBox commentsBox,
                                   Label counter, int myId) {
        User me = SessionManager.getInstance().getCurrentUser();
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12px;" +
                "-fx-padding: 12 14; -fx-border-color: #e2e8f0;" +
                "-fx-border-width: 1px; -fx-border-radius: 12px;");

        HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT);
        TextField tf = new TextField();
        tf.setPromptText("Écrire un commentaire...");
        tf.setStyle(inputStyle());
        HBox.setHgrow(tf, Priority.ALWAYS);
        Button send = new Button("Envoyer");
        send.setStyle("-fx-background-color:#f97316; -fx-text-fill:white; -fx-font-size:12px;" +
                "-fx-font-weight:bold; -fx-background-radius:8px; -fx-padding:8 16; -fx-cursor:hand;");
        row.getChildren().addAll(tf, send);
        box.getChildren().add(row);

        Runnable doSend = () -> {
            String txt = tf.getText().trim();
            if (txt.isEmpty() || me == null) return;
            String display = buildDisplayName(me);
            pub.commentaires.add(new Commentaire(
                    me.getId(), display, initiale(display), me.getPhotoPath(), txt));
            tf.clear();
            renderComments(pub, commentsBox, counter, myId);
        };

        send.setOnAction(e -> doSend.run());
        tf.setOnAction(e -> doSend.run());
        return box;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Input réponse à un commentaire
    // ══════════════════════════════════════════════════════════════════════════
    private VBox buildReplyInput(Publication pub, Commentaire parent,
                                 java.util.function.Consumer<VBox> onUpdate,
                                 Label counter, int myId) {
        User me = SessionManager.getInstance().getCurrentUser();
        VBox box = new VBox(0);
        box.setStyle("-fx-padding: 8 0 0 0;");

        HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT);
        TextField tf = new TextField();
        tf.setPromptText("Répondre à " + (parent.auteurNom.isEmpty() ? "ce commentaire" : parent.auteurNom) + "...");
        tf.setStyle(inputStyle());
        HBox.setHgrow(tf, Priority.ALWAYS);

        Button send = new Button("↩ Répondre");
        send.setStyle("-fx-background-color:#1e3a5f; -fx-text-fill:white; -fx-font-size:11px;" +
                "-fx-font-weight:bold; -fx-background-radius:8px; -fx-padding:7 14; -fx-cursor:hand;");
        row.getChildren().addAll(tf, send);
        box.getChildren().add(row);

        Runnable doReply = () -> {
            String txt = tf.getText().trim();
            if (txt.isEmpty() || me == null) return;
            String display = buildDisplayName(me);
            parent.reponses.add(new Commentaire(
                    me.getId(), display, initiale(display), me.getPhotoPath(),
                    "@" + parent.auteurNom + " " + txt));
            tf.clear();
            box.setVisible(false); box.setManaged(false);
            // Rebuild the full feed to reflect reply
            refreshFeed();
        };

        send.setOnAction(e -> doReply.run());
        tf.setOnAction(e -> doReply.run());
        return box;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers visuels
    // ══════════════════════════════════════════════════════════════════════════
    private StackPane buildAvatar(String nom, String init, String photo, String color, int size) {
        StackPane avt = new StackPane();
        avt.setMinSize(size, size); avt.setMaxSize(size, size);
        avt.setStyle("-fx-background-color:" + color + "; -fx-background-radius:50%;");

        Label lbl = new Label(init);
        lbl.setStyle("-fx-font-size:" + (size / 2.8) + "px; -fx-font-weight:bold; -fx-text-fill:white;");
        avt.getChildren().add(lbl);

        if (photo != null && !photo.isBlank()) {
            File f = new File(photo);
            if (f.exists()) {
                try {
                    ImageView iv = new ImageView(new Image(f.toURI().toString()));
                    iv.setFitWidth(size); iv.setFitHeight(size); iv.setPreserveRatio(false);
                    iv.setClip(new Circle(size / 2.0, size / 2.0, size / 2.0));
                    avt.getChildren().add(iv);
                    lbl.setVisible(false);
                } catch (Exception ignored) {}
            }
        }
        return avt;
    }

    private Region separator() {
        Region r = new Region();
        r.setStyle("-fx-background-color: #f1f5f9; -fx-pref-height: 1px;");
        return r;
    }

    private String btnStyle(String bg, String fg) {
        return "-fx-background-color:" + bg + "; -fx-text-fill:" + fg + ";" +
                "-fx-font-size:12px; -fx-background-radius:8px;" +
                "-fx-padding:8 16; -fx-cursor:hand;" +
                "-fx-border-color:#e2e8f0; -fx-border-width:1px; -fx-border-radius:8px;";
    }

    private String inputStyle() {
        return "-fx-pref-height:38px; -fx-background-color:white;" +
                "-fx-border-color:#e2e8f0; -fx-border-width:1px;" +
                "-fx-border-radius:8px; -fx-background-radius:8px;" +
                "-fx-font-size:13px; -fx-padding:8 12;";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Utilitaires User
    // ══════════════════════════════════════════════════════════════════════════
    private String buildDisplayName(User u) {
        String p = u.getPrenom() != null ? u.getPrenom().trim() : "";
        String n = u.getNom()    != null ? u.getNom().trim()    : "";
        String r = !p.isEmpty() ? p + " " + n : n;
        return r.isBlank() ? (u.getEmail() != null ? u.getEmail().split("@")[0] : "User") : r.trim();
    }

    private String initiale(String name) {
        return name.isBlank() ? "U" : String.valueOf(name.trim().charAt(0)).toUpperCase();
    }

    private boolean hasPhoto(User u) {
        return u.getPhotoPath() != null && !u.getPhotoPath().isBlank()
                && new File(u.getPhotoPath()).exists();
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Attention"); a.setHeaderText(null); a.setContentText(msg);
        a.getDialogPane().setStyle("-fx-font-size: 13px;"); a.showAndWait();
    }
}