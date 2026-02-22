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

import javafx.animation.FadeTransition;
import javafx.util.Duration;
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
    @FXML private VBox      imagePreviewPane;
    @FXML private ImageView imgPreview;
    @FXML private VBox      feedBox;
    @FXML private TextField tfSearch;
    @FXML private Button    btnClearSearch;
    @FXML private Button    btnFiltreAll;
    @FXML private Button    btnFiltreFournisseur;
    @FXML private Button    btnFiltreEntrepreneur;
    @FXML private Button    btnFiltreMes;
    @FXML private Button    btnFiltreGroupe;
    @FXML private Button    btnTriRecent;
    @FXML private Button    btnTriPop;
    @FXML private Label     lblResultat;
    @FXML private VBox      emojiPanel;
    @FXML private Button    btnToggleEmoji;
    @FXML private VBox      groupePane;
    @FXML private VBox      notifPane;
    @FXML private VBox      notifListBox;
    @FXML private Label     lblNotifBadge;
    @FXML private Button    btnNotif;
    // groupeListBox et detailPane construits dynamiquement

    private String selectedImagePath = null;
    private boolean emojiPanelVisible = false;
    private String currentFilter  = "all";   // all | fournisseur | entrepreneur | mes
    private String currentSort    = "recent"; // recent | populaire
    private String currentSearch  = "";

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
    // Modèle Groupe
    // ══════════════════════════════════════════════════════════════════════════
    private static class Groupe {
        int    id;
        int    createurId;
        String createurNom;
        String createurInitiale;
        String nom;
        String description;
        String categorie;
        LocalDateTime dateCreation;
        List<String>      membres      = new ArrayList<>();
        List<Publication> posts        = new ArrayList<>();

        Groupe(int id, int createurId, String createurNom, String createurInitiale,
               String nom, String description, String categorie) {
            this.id = id; this.createurId = createurId;
            this.createurNom = createurNom; this.createurInitiale = createurInitiale;
            this.nom = nom; this.description = description; this.categorie = categorie;
            this.dateCreation = LocalDateTime.now();
            this.membres.add(createurNom);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Modèle Notification
    // ══════════════════════════════════════════════════════════════════════════
    private static class Notif {
        int    destinataireId;   // ID du user qui reçoit la notif
        String auteurNom;        // Qui a posté
        String groupeNom;        // Dans quel groupe
        String apercu;           // Début du message
        LocalDateTime date;
        boolean lue = false;

        Notif(int destId, String auteur, String groupe, String apercu) {
            this.destinataireId = destId;
            this.auteurNom = auteur;
            this.groupeNom = groupe;
            this.apercu    = apercu;
            this.date      = LocalDateTime.now();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Storage statique (session)
    // ══════════════════════════════════════════════════════════════════════════
    private static final List<Publication> PUBLICATIONS  = new ArrayList<>();
    private static final List<Groupe>      GROUPES       = new ArrayList<>();
    private static final List<Notif>       NOTIFICATIONS = new ArrayList<>();
    private static int nextId      = 1;
    private static int nextGroupId = 1;

    // ══════════════════════════════════════════════════════════════════════════
    // Init
    // ══════════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        // ✅ Reset complet à chaque chargement (nouveau user ou re-navigation)
        taNewPost.clear();
        taNewPost.setEditable(true);
        taNewPost.setDisable(false);
        selectedImagePath = null;
        currentFilter     = "all";
        currentSort       = "recent";
        currentSearch     = "";
        emojiPanelVisible = false;
        if (emojiPanel      != null) { emojiPanel.setVisible(false);      emojiPanel.setManaged(false); }
        if (imagePreviewPane!= null) { imagePreviewPane.setVisible(false); imagePreviewPane.setManaged(false); }
        if (lblCharCount    != null)   lblCharCount.setText("0 / 500");
        if (btnClearSearch  != null) { btnClearSearch.setVisible(false);  btnClearSearch.setManaged(false); }
        if (tfSearch        != null)   tfSearch.clear();

        setupCreatorAvatar(user);
        updateFilterButtons();
        updateSortButtons();

        taNewPost.textProperty().addListener((o, ov, nv) -> {
            int len = nv.length();
            lblCharCount.setText(len + " / 500");
            lblCharCount.setStyle("-fx-font-size: 11px; " +
                    (len > 450 ? "-fx-text-fill: #ef4444;" : "-fx-text-fill: #94a3b8;"));
        });

        // Recherche en temps réel
        tfSearch.textProperty().addListener((o, ov, nv) -> {
            currentSearch = nv.trim().toLowerCase();
            btnClearSearch.setVisible(!currentSearch.isEmpty());
            btnClearSearch.setManaged(!currentSearch.isEmpty());
            applyFiltersAndSort();
        });

        refreshFeed();
    }

    private void setupCreatorAvatar(User user) {
        String display = buildDisplayName(user);
        String init    = initiale(display);
        lblCreateAvatar.setText(init);
        // Prompt text personnalisé avec le prénom
        String prenom = user.getPrenom() != null ? user.getPrenom().trim() : display.split(" ")[0];
        taNewPost.setPromptText("Quoi de neuf, " + prenom + " ?");

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

    @FXML private void handleAddVideo() { /* placeholder */ }

    // ✅ Toggle panneau emoji
    @FXML private void handleToggleEmoji() {
        emojiPanelVisible = !emojiPanelVisible;
        emojiPanel.setVisible(emojiPanelVisible);
        emojiPanel.setManaged(emojiPanelVisible);
        btnToggleEmoji.setStyle("-fx-background-color:" +
                (emojiPanelVisible ? "rgba(249,115,22,0.2);" : "transparent;") +
                "-fx-font-size:18px; -fx-cursor:hand; -fx-padding:4; -fx-background-radius:8px;");
    }

    // ✅ Insérer emoji à la position du curseur
    @FXML private void insertEmoji(javafx.event.ActionEvent event) {
        if (event.getSource() instanceof javafx.scene.control.Button btn) {
            String emoji = (String) btn.getUserData();
            if (emoji == null) return;
            int pos = taNewPost.getCaretPosition();
            taNewPost.insertText(pos, emoji);
            taNewPost.requestFocus();
            taNewPost.positionCaret(pos + emoji.length());
        }
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
    // ══════════════════════════════════════════════════════════════════════════
    // Filtres rôle
    // ══════════════════════════════════════════════════════════════════════════
    @FXML private void filtrerTous()          { currentFilter = "all";          updateFilterButtons(); showFeedView(); applyFiltersAndSort(); }
    @FXML private void filtrerFournisseurs()  { currentFilter = "fournisseur";  updateFilterButtons(); showFeedView(); applyFiltersAndSort(); }
    @FXML private void filtrerEntrepreneurs() { currentFilter = "entrepreneur"; updateFilterButtons(); showFeedView(); applyFiltersAndSort(); }
    @FXML private void filtrerMesPosts()      { currentFilter = "mes";          updateFilterButtons(); showFeedView(); applyFiltersAndSort(); }
    @FXML private void filtrerGroupe() {
        currentFilter = "groupe";
        updateFilterButtons();
        feedBox.setVisible(false);   feedBox.setManaged(false);
        groupePane.setVisible(true); groupePane.setManaged(true);
        refreshGroupes();
    }

    private void showFeedView() {
        if (groupePane != null) { groupePane.setVisible(false); groupePane.setManaged(false); }
        if (feedBox    != null) { feedBox.setVisible(true);     feedBox.setManaged(true); }
    }
    @FXML private void handleClearSearch()    { tfSearch.clear(); }

    // Tri
    @FXML private void trierRecents()    { currentSort = "recent";    updateSortButtons(); applyFiltersAndSort(); }
    @FXML private void trierPopulaires() { currentSort = "populaire"; updateSortButtons(); applyFiltersAndSort(); }

    private void refreshFeed() { applyFiltersAndSort(); }

    private void updateFilterButtons() {
        String base   = "-fx-font-size:11px; -fx-font-weight:bold; -fx-background-radius:20px; -fx-border-radius:20px; -fx-padding:5 14; -fx-cursor:hand;";
        String active = "-fx-background-color:#1e3a5f; -fx-text-fill:white; -fx-border-color:#1e3a5f; -fx-border-width:1px;";
        String inactiveAll  = "-fx-background-color:#f1f4f9; -fx-text-fill:#64748b; -fx-border-color:#e2e8f0; -fx-border-width:1px;";
        String inactiveFour = "-fx-background-color:#fff7ed; -fx-text-fill:#f97316; -fx-border-color:#fed7aa; -fx-border-width:1px;";
        String inactiveEntr = "-fx-background-color:#f0fdf4; -fx-text-fill:#10b981; -fx-border-color:#a7f3d0; -fx-border-width:1px;";
        String inactiveMes    = "-fx-background-color:#f5f3ff; -fx-text-fill:#7c3aed; -fx-border-color:#ddd6fe; -fx-border-width:1px;";
        String inactiveGroupe = "-fx-background-color:#fdf4ff; -fx-text-fill:#a855f7; -fx-border-color:#e9d5ff; -fx-border-width:1px;";

        btnFiltreAll.setStyle(base + (currentFilter.equals("all")          ? active : inactiveAll));
        btnFiltreFournisseur.setStyle(base + (currentFilter.equals("fournisseur")  ? active : inactiveFour));
        btnFiltreEntrepreneur.setStyle(base + (currentFilter.equals("entrepreneur") ? active : inactiveEntr));
        btnFiltreMes.setStyle(base + (currentFilter.equals("mes")          ? active : inactiveMes));
        if (btnFiltreGroupe != null)
            btnFiltreGroupe.setStyle(base + (currentFilter.equals("groupe") ? active : inactiveGroupe));
    }

    private void updateSortButtons() {
        String base = "-fx-font-size:11px; -fx-background-radius:8px; -fx-padding:5 12; -fx-cursor:hand;";
        btnTriRecent.setStyle(base + (currentSort.equals("recent")
                ? "-fx-background-color:#1e3a5f; -fx-text-fill:white;"
                : "-fx-background-color:#f1f4f9; -fx-text-fill:#64748b;"));
        btnTriPop.setStyle(base + (currentSort.equals("populaire")
                ? "-fx-background-color:#ef4444; -fx-text-fill:white;"
                : "-fx-background-color:#f1f4f9; -fx-text-fill:#64748b;"));
    }

    private void applyFiltersAndSort() {
        User me   = SessionManager.getInstance().getCurrentUser();
        int  myId = me != null ? me.getId() : -1;

        // 1. Filtrer par rôle
        List<Publication> filtered = PUBLICATIONS.stream()
                .filter(p -> {
                    String role = p.auteurRole.trim().toLowerCase();
                    return switch (currentFilter) {
                        case "fournisseur"  -> role.startsWith("fournisseur");
                        case "entrepreneur" -> role.startsWith("entrepreneur");
                        case "mes"          -> p.auteurId == myId;
                        case "groupe"       -> role.startsWith("fournisseur") || role.startsWith("entrepreneur");
                        default             -> true;
                    };
                })
                // 2. Filtrer par recherche (texte contenu OU nom auteur)
                .filter(p -> currentSearch.isEmpty()
                        || p.contenu.toLowerCase().contains(currentSearch)
                        || p.auteurNom.toLowerCase().contains(currentSearch)
                        || p.auteurRole.toLowerCase().contains(currentSearch))
                // 3. Trier
                .sorted(currentSort.equals("populaire")
                        ? Comparator.comparingInt(Publication::getLikes).reversed()
                        : Comparator.comparing((Publication p) -> p.dateHeure, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        // 4. Afficher résultat
        feedBox.getChildren().clear();

        // Label résultat recherche
        if (!currentSearch.isEmpty()) {
            lblResultat.setText(filtered.size() + " résultat(s) pour \"" + currentSearch + "\"");
            lblResultat.setVisible(true); lblResultat.setManaged(true);
        } else {
            lblResultat.setVisible(false); lblResultat.setManaged(false);
        }

        if (filtered.isEmpty()) {
            VBox empty = new VBox(12); empty.setAlignment(Pos.CENTER);
            empty.setStyle("-fx-padding: 50;");
            String icon = currentSearch.isEmpty() ? "📰" : "🔍";
            String msg  = currentSearch.isEmpty()
                    ? "Aucune publication pour ce filtre."
                    : "Aucun résultat pour \"" + currentSearch + "\"";
            Label iLbl = new Label(icon); iLbl.setStyle("-fx-font-size: 44px;");
            Label mLbl = new Label(msg);
            mLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-text-alignment: center;");
            mLbl.setWrapText(true);
            empty.getChildren().addAll(iLbl, mLbl);
            feedBox.getChildren().add(empty);
        } else {
            filtered.forEach(p -> feedBox.getChildren().add(buildCard(p)));
        }
    }

    private void refreshFeedSorted(Comparator<Publication> cmp) { applyFiltersAndSort(); }

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
            box.getChildren().add(buildCommentItem(pub, c, ignored -> {
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

    // ══════════════════════════════════════════════════════════════════════════
    // GESTION GROUPES
    // ══════════════════════════════════════════════════════════════════════════
    private void refreshGroupes() {
        if (groupePane == null) return;
        groupePane.getChildren().clear();
        buildGroupeMainPane();
    }

    @FXML private void handleCreerGroupe() {
        User me = SessionManager.getInstance().getCurrentUser();
        if (me == null) return;

        Dialog<Groupe> dialog = new Dialog<>();
        dialog.setTitle("Créer un groupe");
        dialog.setHeaderText("👥  Nouveau Groupe");

        ButtonType creerBtn = new ButtonType("✅ Créer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(creerBtn, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-font-size: 13px; -fx-pref-width: 480px;");

        String fieldStyle = "-fx-pref-height: 38px; -fx-font-size: 13px;" +
                "-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0;" +
                "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px;" +
                "-fx-padding: 6 12;";

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(12); grid.setVgap(14);
        grid.setPadding(new javafx.geometry.Insets(20, 30, 10, 20));

        TextField tfNom = new TextField(); tfNom.setPromptText("Ex: Fournisseurs Tunis"); tfNom.setStyle(fieldStyle);
        TextField tfDesc = new TextField(); tfDesc.setPromptText("Description du groupe..."); tfDesc.setStyle(fieldStyle);

        ChoiceBox<String> cbCateg = new ChoiceBox<>();
        cbCateg.getItems().addAll("🌐 Mixte (tous)", "🏪 Fournisseurs", "💼 Entrepreneurs");
        cbCateg.setValue("🌐 Mixte (tous)");
        cbCateg.setStyle("-fx-pref-height: 38px; -fx-font-size: 13px;");

        // Couleur du label nom selon longueur
        Label nomErr = new Label(""); nomErr.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
        tfNom.textProperty().addListener((o, ov, nv) -> {
            if (nv.trim().length() < 3 && !nv.isEmpty())
                nomErr.setText("⚠ Minimum 3 caractères");
            else nomErr.setText("");
        });

        grid.add(new Label("Nom du groupe *"), 0, 0); grid.add(tfNom,    1, 0); grid.add(nomErr, 1, 1);
        grid.add(new Label("Description"),     0, 2); grid.add(tfDesc,   1, 2);
        grid.add(new Label("Catégorie"),        0, 3); grid.add(cbCateg,  1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == creerBtn) {
                String nom  = tfNom.getText().trim();
                String desc = tfDesc.getText().trim();
                if (nom.length() < 3) return null;
                String cat = cbCateg.getValue().contains("Fournisseurs") ? "fournisseur"
                        : cbCateg.getValue().contains("Entrepreneurs") ? "entrepreneur" : "mixte";
                String display = buildDisplayName(me);
                return new Groupe(nextGroupId++, me.getId(), display, initiale(display),
                        nom, desc, cat);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(g -> {
            if (g == null) { showAlert("⚠ Le nom doit avoir au moins 3 caractères."); return; }
            GROUPES.add(0, g);
            refreshGroupes();
        });
    }

    private VBox buildGroupeCard(Groupe groupe) {
        User me   = SessionManager.getInstance().getCurrentUser();
        int  myId = me != null ? me.getId() : -1;

        // Couleur selon catégorie
        String color = groupe.categorie.equals("fournisseur") ? "#f97316"
                : groupe.categorie.equals("entrepreneur") ? "#10b981" : "#a855f7";
        String bgColor = groupe.categorie.equals("fournisseur") ? "#fff7ed"
                : groupe.categorie.equals("entrepreneur") ? "#f0fdf4" : "#fdf4ff";
        String catLabel = groupe.categorie.equals("fournisseur") ? "🏪 Fournisseurs"
                : groupe.categorie.equals("entrepreneur") ? "💼 Entrepreneurs" : "🌐 Mixte";

        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16px;" +
                "-fx-border-color: #e2e8f0; -fx-border-width: 1px; -fx-border-radius: 16px;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");

        // Barre couleur top
        Region topBar = new Region();
        topBar.setStyle("-fx-background-color:" + color + "; -fx-pref-height:4px; -fx-background-radius:16 16 0 0;");
        card.getChildren().add(topBar);

        VBox body = new VBox(12); body.setStyle("-fx-padding: 16 20;");

        // Header
        HBox header = new HBox(14); header.setAlignment(Pos.CENTER_LEFT);

        // Avatar groupe
        StackPane avt = new StackPane();
        avt.setMinSize(50, 50); avt.setMaxSize(50, 50);
        avt.setStyle("-fx-background-color:" + bgColor + "; -fx-background-radius:14px;" +
                "-fx-border-color:" + color + "; -fx-border-width:2px; -fx-border-radius:14px;");
        Label avtLbl = new Label(groupe.nom.substring(0, Math.min(2, groupe.nom.length())).toUpperCase());
        avtLbl.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:" + color + ";");
        avt.getChildren().add(avtLbl);

        VBox info = new VBox(4); HBox.setHgrow(info, Priority.ALWAYS);
        Label nomLbl = new Label(groupe.nom);
        nomLbl.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#1e293b;");

        HBox meta = new HBox(10); meta.setAlignment(Pos.CENTER_LEFT);
        Label catBadge = new Label(catLabel);
        catBadge.setStyle("-fx-background-color:" + bgColor + "; -fx-text-fill:" + color + ";" +
                "-fx-font-size:10px; -fx-font-weight:bold; -fx-background-radius:20px; -fx-padding:3 10;");
        Label membreCount = new Label("👤 " + groupe.membres.size() + " membre(s)");
        membreCount.setStyle("-fx-font-size:11px; -fx-text-fill:#64748b;");
        Label dateLbl = new Label("Créé le " + groupe.dateCreation.format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dateLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#94a3b8;");
        meta.getChildren().addAll(catBadge, membreCount, dateLbl);

        info.getChildren().addAll(nomLbl, meta);
        header.getChildren().addAll(avt, info);

        // Bouton supprimer si créateur
        if (groupe.createurId == myId) {
            Button del = new Button("🗑️");
            del.setStyle("-fx-background-color:transparent; -fx-text-fill:#ef4444;" +
                    "-fx-font-size:14px; -fx-cursor:hand; -fx-padding:4 8;");
            del.setOnAction(e -> {
                Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
                conf.setTitle("Confirmation");
                conf.setHeaderText(null);
                conf.setContentText("Supprimer le groupe « " + groupe.nom + " » ?");
                conf.showAndWait().ifPresent(b -> {
                    if (b == ButtonType.OK) { GROUPES.remove(groupe); refreshGroupes(); }
                });
            });
            header.getChildren().add(del);
        }

        body.getChildren().add(header);

        // Description
        if (!groupe.description.isEmpty()) {
            Label descLbl = new Label(groupe.description);
            descLbl.setWrapText(true);
            descLbl.setStyle("-fx-font-size:13px; -fx-text-fill:#475569;");
            body.getChildren().add(descLbl);
        }

        body.getChildren().add(separator());

        // Créateur
        HBox footer = new HBox(8); footer.setAlignment(Pos.CENTER_LEFT);
        StackPane creatorAvt = buildAvatar(groupe.createurNom, groupe.createurInitiale, null, color, 28);
        Label creatorLbl = new Label("Créé par " + groupe.createurNom);
        creatorLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        // Bouton rejoindre/quitter
        boolean isMember = groupe.membres.contains(buildDisplayName(me != null ? me : new User()));
        Button joinBtn = new Button(isMember ? "✓ Membre" : "＋ Rejoindre");
        joinBtn.setStyle((isMember
                ? "-fx-background-color:#f0fdf4; -fx-text-fill:#10b981; -fx-border-color:#a7f3d0;"
                : "-fx-background-color:" + color + "; -fx-text-fill:white; -fx-border-color:" + color + ";") +
                "-fx-border-width:1px; -fx-border-radius:20px; -fx-background-radius:20px;" +
                "-fx-font-size:12px; -fx-font-weight:bold; -fx-padding:6 16; -fx-cursor:hand;");

        joinBtn.setOnAction(e -> {
            if (me == null) return;
            String myName = buildDisplayName(me);
            if (groupe.membres.contains(myName)) {
                groupe.membres.remove(myName);
            } else {
                groupe.membres.add(myName);
            }
            refreshGroupes();
        });

        footer.getChildren().addAll(creatorAvt, creatorLbl, sp, joinBtn);
        body.getChildren().add(footer);

        // ─── Clic sur la carte → ouvrir le détail ───────────────────────────────
        card.setOnMouseClicked(e -> openGroupeDetail(groupe));
        card.setStyle(card.getStyle() + " -fx-cursor: hand;");

        card.getChildren().add(body);
        return card;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // VUE DÉTAIL GROUPE
    // ══════════════════════════════════════════════════════════════════════════
    private Groupe currentGroupe = null;

    private void openGroupeDetail(Groupe groupe) {
        currentGroupe = groupe;
        User me = SessionManager.getInstance().getCurrentUser();
        if (me == null) return;

        String color   = getGroupeColor(groupe);
        String bgColor = getGroupeBgColor(groupe);
        String catLabel = getGroupeCatLabel(groupe);
        boolean isMember = groupe.membres.contains(buildDisplayName(me));

        // Construire la vue détail inline dans groupePane
        groupePane.getChildren().clear();

        // ── Bouton retour ──
        HBox topBar = new HBox(12); topBar.setAlignment(Pos.CENTER_LEFT);
        Button backBtn = new Button("← Retour aux groupes");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #1e3a5f;" +
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 0;");
        backBtn.setOnAction(e -> {
            currentGroupe = null;
            groupePane.getChildren().clear();
            buildGroupeMainPane();
        });
        topBar.getChildren().add(backBtn);
        groupePane.getChildren().add(topBar);

        // ── Bannière groupe ──
        VBox banner = new VBox(10);
        banner.setStyle("-fx-background-color:" + bgColor + "; -fx-background-radius: 16px;" +
                "-fx-border-color:" + color + "; -fx-border-width: 2px; -fx-border-radius: 16px;" +
                "-fx-padding: 20 24;");

        HBox bannerHeader = new HBox(16); bannerHeader.setAlignment(Pos.CENTER_LEFT);

        // Avatar grand
        StackPane bigAvt = new StackPane();
        bigAvt.setMinSize(64, 64); bigAvt.setMaxSize(64, 64);
        bigAvt.setStyle("-fx-background-color: white; -fx-background-radius: 18px;" +
                "-fx-border-color:" + color + "; -fx-border-width: 2.5px; -fx-border-radius: 18px;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.1),8,0,0,2);");
        Label bigAvtLbl = new Label(groupe.nom.substring(0, Math.min(2, groupe.nom.length())).toUpperCase());
        bigAvtLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill:" + color + ";");
        bigAvt.getChildren().add(bigAvtLbl);

        VBox bannerInfo = new VBox(6); HBox.setHgrow(bannerInfo, Priority.ALWAYS);
        Label bannerNom = new Label(groupe.nom);
        bannerNom.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        HBox bannerMeta = new HBox(12); bannerMeta.setAlignment(Pos.CENTER_LEFT);
        Label catBadge = new Label(catLabel);
        catBadge.setStyle("-fx-background-color: white; -fx-text-fill:" + color + ";" +
                "-fx-font-size:11px; -fx-font-weight:bold; -fx-background-radius:20px; -fx-padding:3 12;");
        Label memLbl = new Label("👤 " + groupe.membres.size() + " membre(s)");
        memLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        Label postLbl = new Label("📝 " + groupe.posts.size() + " post(s)");
        postLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        bannerMeta.getChildren().addAll(catBadge, memLbl, postLbl);

        if (!groupe.description.isEmpty()) {
            Label descLbl = new Label(groupe.description);
            descLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
            descLbl.setWrapText(true);
            bannerInfo.getChildren().addAll(bannerNom, bannerMeta, descLbl);
        } else {
            bannerInfo.getChildren().addAll(bannerNom, bannerMeta);
        }

        // Bouton rejoindre/quitter dans bannière
        Button joinBtn = new Button(isMember ? "✓ Membre" : "＋ Rejoindre");
        joinBtn.setStyle((isMember
                ? "-fx-background-color:#f0fdf4; -fx-text-fill:#10b981; -fx-border-color:#a7f3d0;"
                : "-fx-background-color:" + color + "; -fx-text-fill:white;") +
                "-fx-font-size:13px; -fx-font-weight:bold;" +
                "-fx-border-width:1px; -fx-border-radius:20px; -fx-background-radius:20px;" +
                "-fx-padding:8 20; -fx-cursor:hand;");
        joinBtn.setOnAction(e -> {
            String myName = buildDisplayName(me);
            if (groupe.membres.contains(myName)) groupe.membres.remove(myName);
            else groupe.membres.add(myName);
            openGroupeDetail(groupe);
        });

        // Bouton supprimer si créateur
        if (groupe.createurId == me.getId()) {
            Button delBtn = new Button("🗑️ Supprimer");
            delBtn.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444;" +
                    "-fx-font-size:12px; -fx-background-radius:20px; -fx-padding:8 16; -fx-cursor:hand;");
            delBtn.setOnAction(e -> {
                Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
                conf.setHeaderText(null);
                conf.setContentText("Supprimer le groupe « " + groupe.nom + " » ?");
                conf.showAndWait().ifPresent(b -> {
                    if (b == ButtonType.OK) {
                        GROUPES.remove(groupe);
                        currentGroupe = null;
                        groupePane.getChildren().clear();
                        buildGroupeMainPane();
                    }
                });
            });
            bannerHeader.getChildren().addAll(bigAvt, bannerInfo, delBtn, joinBtn);
        } else {
            bannerHeader.getChildren().addAll(bigAvt, bannerInfo, joinBtn);
        }

        banner.getChildren().add(bannerHeader);
        groupePane.getChildren().add(banner);

        // ── Membres ──
        VBox membresCard = new VBox(10);
        membresCard.setStyle("-fx-background-color: white; -fx-background-radius: 14px;" +
                "-fx-border-color: #e2e8f0; -fx-border-width: 1px; -fx-border-radius: 14px;" +
                "-fx-padding: 14 18;");
        Label membresTitle = new Label("👥  Membres (" + groupe.membres.size() + ")");
        membresTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        HBox membresFlow = new HBox(8); membresFlow.setAlignment(Pos.CENTER_LEFT);
        membresFlow.setStyle("-fx-flex-wrap: wrap;");
        for (String m : groupe.membres) {
            Label mBadge = new Label(m);
            mBadge.setStyle("-fx-background-color:" + bgColor + "; -fx-text-fill:" + color + ";" +
                    "-fx-font-size:12px; -fx-font-weight:bold; -fx-background-radius:20px; -fx-padding:5 14;");
            membresFlow.getChildren().add(mBadge);
        }
        membresCard.getChildren().addAll(membresTitle, membresFlow);
        groupePane.getChildren().add(membresCard);

        // ── Zone publier dans le groupe ──
        if (isMember) {
            VBox postZone = new VBox(10);
            postZone.setStyle("-fx-background-color: white; -fx-background-radius: 14px;" +
                    "-fx-border-color: #e2e8f0; -fx-border-width: 1px; -fx-border-radius: 14px;" +
                    "-fx-padding: 14 18;");
            Label postTitle = new Label("✏️  Écrire dans le groupe");
            postTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            HBox postRow = new HBox(10); postRow.setAlignment(Pos.CENTER_LEFT);
            StackPane myAvt = buildAvatar(buildDisplayName(me), initiale(buildDisplayName(me)),
                    me.getPhotoPath(), color, 36);

            TextArea taPost = new TextArea();
            taPost.setPromptText("Partagez quelque chose avec le groupe...");
            taPost.setPrefRowCount(2); taPost.setWrapText(true);
            taPost.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0;" +
                    "-fx-border-width: 1.5px; -fx-border-radius: 12px; -fx-background-radius: 12px;" +
                    "-fx-font-size: 13px; -fx-text-fill: #1e293b; -fx-padding: 10 14;");
            HBox.setHgrow(taPost, Priority.ALWAYS);

            Label charCount = new Label("0 / 300");
            charCount.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
            taPost.textProperty().addListener((o, ov, nv) -> {
                charCount.setText(nv.length() + " / 300");
                charCount.setStyle("-fx-font-size: 11px; -fx-text-fill:" +
                        (nv.length() > 270 ? "#ef4444;" : "#94a3b8;"));
            });

            postRow.getChildren().addAll(myAvt, taPost);

            HBox postActions = new HBox(8); postActions.setAlignment(Pos.CENTER_RIGHT);
            postActions.getChildren().add(charCount);

            Button pubBtn = new Button("Publier dans le groupe →");
            pubBtn.setStyle("-fx-background-color:" + color + "; -fx-text-fill: white;" +
                    "-fx-font-size: 12px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 20px; -fx-padding: 8 18; -fx-cursor: hand;");
            pubBtn.setOnAction(e -> {
                String txt = taPost.getText().trim();
                if (txt.isEmpty()) return;
                if (txt.length() > 300) { showAlert("Maximum 300 caractères."); return; }
                String display = buildDisplayName(me);
                String role = me.getRole() != null ? me.getRole().getNomRole() : "";
                Publication p = new Publication(nextId++, me.getId(), display, role,
                        initiale(display), me.getPhotoPath(), txt, null);
                groupe.posts.add(0, p);
                // ✅ Envoyer une notification à tous les membres sauf l'auteur
                envoyerNotifGroupe(groupe, display, txt, me.getId());
                taPost.clear();
                openGroupeDetail(groupe); // refresh
            });
            postActions.getChildren().add(pubBtn);

            postZone.getChildren().addAll(postTitle, postRow, postActions);
            groupePane.getChildren().add(postZone);
        }

        // ── Posts du groupe ──
        Label postsTitle = new Label("📝  Publications du groupe");
        postsTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        groupePane.getChildren().add(postsTitle);

        if (groupe.posts.isEmpty()) {
            VBox empty = new VBox(10); empty.setAlignment(Pos.CENTER);
            empty.setStyle("-fx-padding: 30; -fx-background-color: white; -fx-background-radius: 14px;" +
                    "-fx-border-color: #e2e8f0; -fx-border-width: 1px; -fx-border-radius: 14px;");
            Label emptyIcon = new Label("📭"); emptyIcon.setStyle("-fx-font-size: 36px;");
            Label emptyMsg = new Label("Aucune publication dans ce groupe pour le moment.");
            emptyMsg.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
            empty.getChildren().addAll(emptyIcon, emptyMsg);
            groupePane.getChildren().add(empty);
        } else {
            groupe.posts.forEach(p -> groupePane.getChildren().add(buildCard(p)));
        }
    }

    private void buildGroupeMainPane() {
        // Reconstruire l'en-tête + liste des groupes
        HBox headerLine = new HBox(12); headerLine.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("👥  Mes Groupes");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button creerBtn = new Button("＋  Créer un groupe");
        creerBtn.setStyle("-fx-background-color: #a855f7; -fx-text-fill: white;" +
                "-fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-background-radius: 20px; -fx-padding: 9 20; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian,rgba(168,85,247,0.4),10,0,0,3);");
        creerBtn.setOnAction(e -> handleCreerGroupe());
        headerLine.getChildren().addAll(title, sp, creerBtn);
        groupePane.getChildren().add(headerLine);

        VBox listBox = new VBox(14);
        if (GROUPES.isEmpty()) {
            VBox empty = new VBox(12); empty.setAlignment(Pos.CENTER);
            empty.setStyle("-fx-padding: 50;");
            Label icon = new Label("👥"); icon.setStyle("-fx-font-size: 44px;");
            Label msg  = new Label("Aucun groupe pour le moment.\nCréez le premier groupe !");
            msg.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-text-alignment: center;");
            msg.setWrapText(true);
            empty.getChildren().addAll(icon, msg);
            listBox.getChildren().add(empty);
        } else {
            GROUPES.forEach(g -> listBox.getChildren().add(buildGroupeCard(g)));
        }
        groupePane.getChildren().add(listBox);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SYSTÈME NOTIFICATIONS
    // ══════════════════════════════════════════════════════════════════════════

    /** Envoie une notif à tous les membres du groupe sauf l'auteur */
    private void envoyerNotifGroupe(Groupe groupe, String auteurNom, String texte, int auteurId) {
        User me = SessionManager.getInstance().getCurrentUser();
        if (me == null) return;

        // Pour chaque membre du groupe : on ne peut pas avoir leur ID en session,
        // donc on crée une notif globale destinée à "tous sauf l'auteur"
        // En pratique : on stocke destinataireId = -1 (tous membres) et on filtre à l'affichage
        String apercu = texte.length() > 60 ? texte.substring(0, 60) + "..." : texte;
        // Notif pour tous les membres qui ne sont pas l'auteur
        // On identifie les membres par nom (pas d'ID), on crée une notif par membre connu
        for (String membreNom : groupe.membres) {
            if (!membreNom.equals(auteurNom)) {
                NOTIFICATIONS.add(0, new Notif(-1, auteurNom, groupe.nom, apercu));
                break; // Une notif globale suffit (filtrée par nom membre à l'affichage)
            }
        }
        // En réalité : stocker une notif pour chaque membre sauf l'auteur
        // Puisqu'on n'a pas les IDs, on stocke destinataireId = auteurId * -1 (marqueur)
        // Simple : on recrée proprement
        NOTIFICATIONS.clear(); // reset et recréer
        String apercuFinal = apercu;
        // Stocker une notif "broadcast" avec destinataireId = auteurId
        // Les autres membres la voient si leur nom est dans groupe.membres et != auteurNom
        NOTIFICATIONS.add(0, new Notif(auteurId * -1, auteurNom, groupe.nom, apercuFinal));

        updateNotifBadge();
        animerCloche();
    }

    private void updateNotifBadge() {
        if (lblNotifBadge == null) return;
        User me = SessionManager.getInstance().getCurrentUser();
        if (me == null) return;
        String myName = buildDisplayName(me);

        long count = NOTIFICATIONS.stream()
                .filter(n -> !n.lue)
                .filter(n -> {
                    // Visible si je suis membre du groupe concerné et pas l'auteur
                    Groupe g = GROUPES.stream()
                            .filter(gr -> gr.nom.equals(n.groupeNom)).findFirst().orElse(null);
                    return g != null && g.membres.contains(myName) && !n.auteurNom.equals(myName);
                })
                .count();

        if (count > 0) {
            lblNotifBadge.setText(count > 9 ? "9+" : String.valueOf(count));
            lblNotifBadge.setVisible(true);  lblNotifBadge.setManaged(true);
            if (btnNotif != null) btnNotif.setText("🔔");
        } else {
            lblNotifBadge.setVisible(false); lblNotifBadge.setManaged(false);
            if (btnNotif != null) btnNotif.setText("🔕");
        }
    }

    private void animerCloche() {
        if (btnNotif == null) return;
        FadeTransition ft = new FadeTransition(Duration.millis(200), btnNotif);
        ft.setFromValue(1.0); ft.setToValue(0.3); ft.setCycleCount(6);
        ft.setAutoReverse(true); ft.play();
    }

    @FXML private void handleToggleNotifications() {
        if (notifPane == null) return;
        boolean show = !notifPane.isVisible();
        notifPane.setVisible(show); notifPane.setManaged(show);
        if (show) refreshNotifList();
    }

    @FXML private void handleMarkAllRead() {
        User me = SessionManager.getInstance().getCurrentUser();
        if (me == null) return;
        String myName = buildDisplayName(me);
        NOTIFICATIONS.stream()
                .filter(n -> {
                    Groupe g = GROUPES.stream()
                            .filter(gr -> gr.nom.equals(n.groupeNom)).findFirst().orElse(null);
                    return g != null && g.membres.contains(myName) && !n.auteurNom.equals(myName);
                })
                .forEach(n -> n.lue = true);
        updateNotifBadge();
        refreshNotifList();
    }

    private void refreshNotifList() {
        if (notifListBox == null) return;
        notifListBox.getChildren().clear();
        User me = SessionManager.getInstance().getCurrentUser();
        if (me == null) return;
        String myName = buildDisplayName(me);

        List<Notif> mesNotifs = NOTIFICATIONS.stream()
                .filter(n -> {
                    Groupe g = GROUPES.stream()
                            .filter(gr -> gr.nom.equals(n.groupeNom)).findFirst().orElse(null);
                    return g != null && g.membres.contains(myName) && !n.auteurNom.equals(myName);
                })
                .collect(Collectors.toList());

        if (mesNotifs.isEmpty()) {
            Label empty = new Label("  Aucune notification pour le moment.");
            empty.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8; -fx-padding: 16 18;");
            notifListBox.getChildren().add(empty);
            return;
        }

        for (Notif n : mesNotifs) {
            HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle((n.lue ? "-fx-background-color: white;" : "-fx-background-color: #f0f7ff;") +
                    "-fx-padding: 12 18; -fx-border-color: #f1f5f9; -fx-border-width: 0 0 1 0;" +
                    "-fx-cursor: hand;");

            // Point non lu
            Label dot = new Label("●");
            dot.setStyle("-fx-font-size: 8px; -fx-text-fill:" +
                    (n.lue ? "transparent;" : "#3b82f6;"));

            // Avatar auteur
            StackPane avt = buildAvatar(n.auteurNom, initiale(n.auteurNom), null, "#a855f7", 36);

            VBox info = new VBox(3); HBox.setHgrow(info, Priority.ALWAYS);
            Label titleLbl = new Label(n.auteurNom + " a publié dans " + n.groupeNom);
            titleLbl.setStyle("-fx-font-size: 13px; -fx-font-weight:" +
                    (n.lue ? "normal;" : "bold;") + " -fx-text-fill: #1e293b;");
            titleLbl.setWrapText(true);

            Label apercuLbl = new Label(n.apercu);
            apercuLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            apercuLbl.setWrapText(true);

            Label dateLbl = new Label(n.date.format(FMT));
            dateLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");

            info.getChildren().addAll(titleLbl, apercuLbl, dateLbl);
            row.getChildren().addAll(dot, avt, info);

            // Clic → marquer lue + aller au groupe
            row.setOnMouseClicked(e -> {
                n.lue = true;
                updateNotifBadge();
                notifPane.setVisible(false); notifPane.setManaged(false);
                // Naviguer vers le groupe
                GROUPES.stream()
                        .filter(g -> g.nom.equals(n.groupeNom))
                        .findFirst()
                        .ifPresent(g -> {
                            currentFilter = "groupe";
                            updateFilterButtons();
                            feedBox.setVisible(false); feedBox.setManaged(false);
                            groupePane.setVisible(true); groupePane.setManaged(true);
                            openGroupeDetail(g);
                        });
            });

            notifListBox.getChildren().add(row);
        }
    }

    // ── Helpers couleur groupe ──────────────────────────────────────────────
    private String getGroupeColor(Groupe g) {
        return g.categorie.equals("fournisseur") ? "#f97316"
                : g.categorie.equals("entrepreneur") ? "#10b981" : "#a855f7";
    }
    private String getGroupeBgColor(Groupe g) {
        return g.categorie.equals("fournisseur") ? "#fff7ed"
                : g.categorie.equals("entrepreneur") ? "#f0fdf4" : "#fdf4ff";
    }
    private String getGroupeCatLabel(Groupe g) {
        return g.categorie.equals("fournisseur") ? "🏪 Fournisseurs"
                : g.categorie.equals("entrepreneur") ? "💼 Entrepreneurs" : "🌐 Mixte";
    }
}