package Controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import models.User;
import services.UserService;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML private Label lblTotalUsers;
    @FXML private Label lblTotalFournisseurs;
    @FXML private Label lblTotalEntrepreneurs;
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

    @FXML private PieChart                 pieChartRoles;
    @FXML private BarChart<String, Number> barChartUsers;
    @FXML private CategoryAxis             barXAxis;
    @FXML private NumberAxis               barYAxis;

    private final UserService userService = new UserService();
    private List<User> allUsers;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        LocalDate today  = LocalDate.now();
        String dayName   = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        String monthName = today.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        lblDate.setText(dayName + " " + today.getDayOfMonth() + " " + monthName + " " + today.getYear());
        setupTable();
        loadStats();
    }

    // =========================================================================
    // TABLE
    // =========================================================================
    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setStyle("-fx-alignment: CENTER;");

        colNomComplet.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNomComplet()));

        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        colRole.setCellValueFactory(c -> {
            models.Role r = c.getValue().getRole();
            return new SimpleStringProperty(r != null ? r.getNomRole() : "-");
        });

        colRole.setCellFactory(col -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(item);
                String r = item.trim().toLowerCase();
                String s;
                if (r.equals("fournisseur")) {
                    s = "-fx-background-color:#fff7ed;-fx-text-fill:#c2410c;";
                } else if (r.equals("entrepreneur")) {
                    s = "-fx-background-color:#f0fdf4;-fx-text-fill:#166534;";
                } else {
                    s = "-fx-background-color:#f1f5f9;-fx-text-fill:#475569;";
                }
                badge.setStyle(s + "-fx-font-size:11px;-fx-font-weight:bold;"
                        + "-fx-background-radius:20px;-fx-padding:4 12;");
                setGraphic(badge);
                setAlignment(Pos.CENTER);
            }
        });

        colRole.setStyle("-fx-alignment: CENTER;");

        colDDN.setCellValueFactory(c -> {
            LocalDate d = c.getValue().getDateNaissance();
            return new SimpleStringProperty(d != null ? d.format(FMT) : "-");
        });
        colDDN.setStyle("-fx-alignment: CENTER;");

        recentUsersTable.setStyle(
                "-fx-background-color:transparent;-fx-border-color:transparent;"
                        + "-fx-table-cell-border-color:#f1f5f9;-fx-font-size:13px;");
        recentUsersTable.setFixedCellSize(50);
        recentUsersTable.setPrefHeight(50 * 5 + 40);
    }

    // =========================================================================
    // STATS + GRAPHIQUES
    // =========================================================================
    private void loadStats() {
        try {
            allUsers = userService.getNonAdmins();

            Map<String, Long> byRole = allUsers.stream()
                    .filter(u -> u.getRole() != null)
                    .collect(Collectors.groupingBy(
                            u -> u.getRole().getNomRole().trim().toLowerCase(),
                            Collectors.counting()));

            final long nbFourn  = byRole.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("fournisseur"))
                    .mapToLong(Map.Entry::getValue).sum();

            final long nbEntrep = byRole.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("entrepreneur"))
                    .mapToLong(Map.Entry::getValue).sum();

            lblTotalUsers.setText(String.valueOf(allUsers.size()));
            if (lblTotalFournisseurs  != null) lblTotalFournisseurs.setText(String.valueOf(nbFourn));
            if (lblTotalEntrepreneurs != null) lblTotalEntrepreneurs.setText(String.valueOf(nbEntrep));

            int sz = allUsers.size();
            recentUsersTable.setItems(FXCollections.observableArrayList(
                    sz > 5 ? allUsers.subList(sz - 5, sz) : allUsers));

            refreshPieChart(nbFourn, nbEntrep);
            refreshBarChart(nbFourn, nbEntrep);

        } catch (Exception ex) {
            lblTotalUsers.setText("Err");
            ex.printStackTrace();
        }

        if (lblTotalProduits  != null) lblTotalProduits.setText("-");
        if (lblTotalCommandes != null) lblTotalCommandes.setText("-");
        if (lblTotalFeedbacks != null) lblTotalFeedbacks.setText("-");
    }

    private void refreshPieChart(long nbFourn, long nbEntrep) {
        if (pieChartRoles == null) return;
        pieChartRoles.getData().clear();
        if (nbFourn  > 0) pieChartRoles.getData().add(
                new PieChart.Data("Fournisseurs (" + nbFourn + ")",  nbFourn));
        if (nbEntrep > 0) pieChartRoles.getData().add(
                new PieChart.Data("Entrepreneurs (" + nbEntrep + ")", nbEntrep));
        javafx.application.Platform.runLater(new Runnable() {
            public void run() { colorPie(); }
        });
    }

    private void colorPie() {
        if (pieChartRoles == null) return;
        String[] colors = new String[]{"#f97316", "#10b981"};
        ObservableList<PieChart.Data> list = pieChartRoles.getData();
        for (int i = 0; i < list.size(); i++) {
            PieChart.Data d = list.get(i);
            if (d.getNode() != null) {
                d.getNode().setStyle("-fx-pie-color:" + colors[i % colors.length] + ";");
            }
        }
    }

    private void refreshBarChart(long nbFourn, long nbEntrep) {
        if (barChartUsers == null) return;
        barChartUsers.getData().clear();
        final XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Utilisateurs");
        series.getData().add(new XYChart.Data<String, Number>("Fournisseurs",  nbFourn));
        series.getData().add(new XYChart.Data<String, Number>("Entrepreneurs", nbEntrep));
        series.getData().add(new XYChart.Data<String, Number>("Total",         (long) allUsers.size()));
        barChartUsers.getData().add(series);
        javafx.application.Platform.runLater(new Runnable() {
            public void run() { colorBar(series); }
        });
    }

    private void colorBar(XYChart.Series<String, Number> series) {
        String[] cols = new String[]{"#f97316", "#10b981", "#1e3a5f"};
        ObservableList<XYChart.Data<String, Number>> list = series.getData();
        for (int i = 0; i < list.size(); i++) {
            XYChart.Data<String, Number> d = list.get(i);
            if (d.getNode() != null) {
                d.getNode().setStyle("-fx-bar-fill:" + cols[i % cols.length] + ";");
            }
        }
    }

    @FXML private void handleRefresh()      { loadStats(); }
    @FXML private void handleViewAllUsers() { }

    // =========================================================================
    // EXPORT EXCEL
    // =========================================================================
    @FXML
    private void handleExportExcel() {
        if (allUsers == null || allUsers.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Export", "Aucune donnee a exporter.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le rapport Excel");
        fc.setInitialFileName("LocalTrade_Rapport_"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichier Excel (*.xlsx)", "*.xlsx"));
        // Ouvrir dans Downloads par defaut
        File downloads = new File(System.getProperty("user.home") + java.io.File.separator + "Downloads");
        if (!downloads.exists() || !downloads.isDirectory()) {
            downloads = new File(System.getProperty("user.home"));
        }
        try { downloads = downloads.getCanonicalFile(); } catch (Exception ignored) {}
        fc.setInitialDirectory(downloads);

        File file = fc.showSaveDialog(recentUsersTable.getScene().getWindow());
        if (file == null) return;
        try {
            buildExcel(file);
            showAlert(Alert.AlertType.INFORMATION, "Export reussi",
                    "Fichier enregistre :\n" + file.getCanonicalPath());
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur export", ex.getMessage());
            ex.printStackTrace();
        }
    }

    // =========================================================================
    // CONSTRUCTION EXCEL
    // =========================================================================
    private void buildExcel(File file) throws Exception {
        // Creer le fichier avec chemin absolu canonique
        File target = file.getCanonicalFile();
        org.apache.poi.xssf.usermodel.XSSFWorkbook wb =
                new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        try {
            sheetUtilisateurs(wb);
            sheetStats(wb);
            FileOutputStream fos = new FileOutputStream(target);
            try {
                wb.write(fos);
                fos.flush();
            } finally {
                fos.close();
            }
        } finally {
            wb.close();
        }
    }

    private void sheetUtilisateurs(org.apache.poi.xssf.usermodel.XSSFWorkbook wb) {
        org.apache.poi.xssf.usermodel.XSSFSheet sh = wb.createSheet("Utilisateurs");

        org.apache.poi.ss.usermodel.CellStyle csTitre  = sTitre(wb);
        org.apache.poi.ss.usermodel.CellStyle csSub    = sSousTitre(wb);
        org.apache.poi.ss.usermodel.CellStyle csHead   = sEntete(wb);
        org.apache.poi.ss.usermodel.CellStyle csData   = sDonnee(wb, false);
        org.apache.poi.ss.usermodel.CellStyle csAlt    = sDonnee(wb, true);
        org.apache.poi.ss.usermodel.CellStyle csFourn  = sRole(wb,
                new byte[]{(byte)255,(byte)247,(byte)237},
                new byte[]{(byte)194,(byte)65,(byte)12});
        org.apache.poi.ss.usermodel.CellStyle csEntrep = sRole(wb,
                new byte[]{(byte)240,(byte)253,(byte)244},
                new byte[]{(byte)22,(byte)101,(byte)52});

        org.apache.poi.ss.usermodel.Row r0 = sh.createRow(0);
        r0.setHeight((short) 800);
        org.apache.poi.ss.usermodel.Cell c0 = r0.createCell(0);
        c0.setCellValue("RAPPORT UTILISATEURS - LocalTrade");
        c0.setCellStyle(csTitre);
        sh.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));

        org.apache.poi.ss.usermodel.Row r1 = sh.createRow(1);
        r1.setHeight((short) 450);
        org.apache.poi.ss.usermodel.Cell c1 = r1.createCell(0);
        c1.setCellValue("Genere le : " + LocalDate.now().format(FMT));
        c1.setCellStyle(csSub);
        sh.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 5));

        sh.createRow(2);

        String[] headers = new String[]{"ID", "Nom complet", "Email", "Role", "Date naissance", "Telephone"};
        org.apache.poi.ss.usermodel.Row r3 = sh.createRow(3);
        r3.setHeight((short) 650);
        for (int i = 0; i < headers.length; i++) {
            org.apache.poi.ss.usermodel.Cell c = r3.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(csHead);
        }

        int rowNum = 4;
        for (User u : allUsers) {
            org.apache.poi.ss.usermodel.Row row = sh.createRow(rowNum);
            row.setHeight((short) 500);
            org.apache.poi.ss.usermodel.CellStyle base = (rowNum % 2 == 0) ? csAlt : csData;

            String roleName = (u.getRole() != null) ? u.getRole().getNomRole() : "-";

            setCell(row, 0, String.valueOf(u.getId()), base);
            setCell(row, 1, u.getNomComplet(), base);
            setCell(row, 2, u.getEmail(), base);

            org.apache.poi.ss.usermodel.Cell cr = row.createCell(3);
            cr.setCellValue(roleName);
            String rl = roleName.trim().toLowerCase();
            if (rl.startsWith("fournisseur")) {
                cr.setCellStyle(csFourn);
            } else if (rl.startsWith("entrepreneur")) {
                cr.setCellStyle(csEntrep);
            } else {
                cr.setCellStyle(base);
            }

            String ddn = (u.getDateNaissance() != null) ? u.getDateNaissance().format(FMT) : "-";
            String tel = (u.getTelephone()     != null) ? u.getTelephone() : "-";
            setCell(row, 4, ddn, base);
            setCell(row, 5, tel, base);
            rowNum++;
        }

        org.apache.poi.ss.usermodel.Row rTot = sh.createRow(rowNum + 1);
        setCell(rTot, 0, "TOTAL", csHead);
        setCell(rTot, 1, allUsers.size() + " utilisateur(s)", csHead);

        sh.setColumnWidth(0, 2000);
        sh.setColumnWidth(1, 7000);
        sh.setColumnWidth(2, 9000);
        sh.setColumnWidth(3, 5000);
        sh.setColumnWidth(4, 5000);
        sh.setColumnWidth(5, 4000);
    }

    private void sheetStats(org.apache.poi.xssf.usermodel.XSSFWorkbook wb) {
        org.apache.poi.xssf.usermodel.XSSFSheet sh = wb.createSheet("Statistiques");

        org.apache.poi.ss.usermodel.CellStyle csTitre = sTitre(wb);
        org.apache.poi.ss.usermodel.CellStyle csSub   = sSousTitre(wb);
        org.apache.poi.ss.usermodel.CellStyle csHead  = sEntete(wb);
        org.apache.poi.ss.usermodel.CellStyle csData  = sDonnee(wb, false);
        org.apache.poi.ss.usermodel.CellStyle csNum   = sNombre(wb);

        org.apache.poi.ss.usermodel.Row r0 = sh.createRow(0);
        r0.setHeight((short) 800);
        org.apache.poi.ss.usermodel.Cell c0 = r0.createCell(0);
        c0.setCellValue("STATISTIQUES - LocalTrade");
        c0.setCellStyle(csTitre);
        sh.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 2));

        org.apache.poi.ss.usermodel.Row r1 = sh.createRow(1);
        org.apache.poi.ss.usermodel.Cell c1 = r1.createCell(0);
        c1.setCellValue("Genere le : " + LocalDate.now().format(FMT));
        c1.setCellStyle(csSub);
        sh.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 2));

        sh.createRow(2);

        String[] headers = new String[]{"Indicateur", "Valeur", "Details"};
        org.apache.poi.ss.usermodel.Row r3 = sh.createRow(3);
        r3.setHeight((short) 650);
        for (int i = 0; i < headers.length; i++) {
            org.apache.poi.ss.usermodel.Cell c = r3.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(csHead);
        }

        int total = allUsers.size();
        long f    = countByRole("fournisseur");
        long e    = countByRole("entrepreneur");

        addRowNum(sh,  csData, csNum, 4, "Total Utilisateurs",  total,        "Fournisseurs + Entrepreneurs");
        addRowNum(sh,  csData, csNum, 5, "Fournisseurs",        (int) f,       pct(f, total) + "% du total");
        addRowNum(sh,  csData, csNum, 6, "Entrepreneurs",       (int) e,       pct(e, total) + "% du total");
        addRowTxt(sh,  csData,        7, "Produits",            "-",           "Module en developpement");
        addRowTxt(sh,  csData,        8, "Commandes",           "-",           "Module en developpement");
        addRowTxt(sh,  csData,        9, "Feedbacks",           "-",           "Module en developpement");

        sh.setColumnWidth(0, 8000);
        sh.setColumnWidth(1, 4000);
        sh.setColumnWidth(2, 9000);
    }

    private void addRowNum(org.apache.poi.xssf.usermodel.XSSFSheet sh,
                           org.apache.poi.ss.usermodel.CellStyle csData,
                           org.apache.poi.ss.usermodel.CellStyle csNum,
                           int idx, String label, int value, String detail) {
        org.apache.poi.ss.usermodel.Row row = sh.createRow(idx);
        row.setHeight((short) 500);
        setCell(row, 0, label, csData);
        org.apache.poi.ss.usermodel.Cell c = row.createCell(1);
        c.setCellValue(value);
        c.setCellStyle(csNum);
        setCell(row, 2, detail, csData);
    }

    private void addRowTxt(org.apache.poi.xssf.usermodel.XSSFSheet sh,
                           org.apache.poi.ss.usermodel.CellStyle csData,
                           int idx, String label, String value, String detail) {
        org.apache.poi.ss.usermodel.Row row = sh.createRow(idx);
        row.setHeight((short) 500);
        setCell(row, 0, label,  csData);
        setCell(row, 1, value,  csData);
        setCell(row, 2, detail, csData);
    }

    // =========================================================================
    // HELPER CELLULE
    // =========================================================================
    private void setCell(org.apache.poi.ss.usermodel.Row row, int col,
                         String val, org.apache.poi.ss.usermodel.CellStyle style) {
        org.apache.poi.ss.usermodel.Cell c = row.createCell(col);
        c.setCellValue(val != null ? val : "");
        c.setCellStyle(style);
    }

    // =========================================================================
    // STYLES POI (fully qualified - zero ambiguite avec javafx)
    // =========================================================================
    private org.apache.poi.ss.usermodel.CellStyle sTitre(
            org.apache.poi.xssf.usermodel.XSSFWorkbook wb) {
        org.apache.poi.ss.usermodel.CellStyle s = wb.createCellStyle();
        org.apache.poi.xssf.usermodel.XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 16);
        f.setColor(new org.apache.poi.xssf.usermodel.XSSFColor(
                new byte[]{(byte)255,(byte)255,(byte)255}, null));
        s.setFont(f);
        ((org.apache.poi.xssf.usermodel.XSSFCellStyle)s).setFillForegroundColor(
                new org.apache.poi.xssf.usermodel.XSSFColor(
                        new byte[]{(byte)30,(byte)58,(byte)95}, null));
        s.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        s.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
        return s;
    }

    private org.apache.poi.ss.usermodel.CellStyle sSousTitre(
            org.apache.poi.xssf.usermodel.XSSFWorkbook wb) {
        org.apache.poi.ss.usermodel.CellStyle s = wb.createCellStyle();
        org.apache.poi.xssf.usermodel.XSSFFont f = wb.createFont();
        f.setItalic(true);
        f.setFontHeightInPoints((short) 11);
        s.setFont(f);
        ((org.apache.poi.xssf.usermodel.XSSFCellStyle)s).setFillForegroundColor(
                new org.apache.poi.xssf.usermodel.XSSFColor(
                        new byte[]{(byte)240,(byte)245,(byte)255}, null));
        s.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        s.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
        return s;
    }

    private org.apache.poi.ss.usermodel.CellStyle sEntete(
            org.apache.poi.xssf.usermodel.XSSFWorkbook wb) {
        org.apache.poi.ss.usermodel.CellStyle s = wb.createCellStyle();
        org.apache.poi.xssf.usermodel.XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 12);
        f.setColor(new org.apache.poi.xssf.usermodel.XSSFColor(
                new byte[]{(byte)255,(byte)255,(byte)255}, null));
        s.setFont(f);
        ((org.apache.poi.xssf.usermodel.XSSFCellStyle)s).setFillForegroundColor(
                new org.apache.poi.xssf.usermodel.XSSFColor(
                        new byte[]{(byte)249,(byte)115,(byte)22}, null));
        s.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        s.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
        s.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        s.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        s.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        s.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        return s;
    }

    private org.apache.poi.ss.usermodel.CellStyle sDonnee(
            org.apache.poi.xssf.usermodel.XSSFWorkbook wb, boolean alt) {
        org.apache.poi.ss.usermodel.CellStyle s = wb.createCellStyle();
        org.apache.poi.xssf.usermodel.XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 11);
        s.setFont(f);
        byte[] bg = alt
                ? new byte[]{(byte)248,(byte)250,(byte)252}
                : new byte[]{(byte)255,(byte)255,(byte)255};
        ((org.apache.poi.xssf.usermodel.XSSFCellStyle)s).setFillForegroundColor(
                new org.apache.poi.xssf.usermodel.XSSFColor(bg, null));
        s.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        s.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
        s.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        s.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        s.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        s.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        return s;
    }

    private org.apache.poi.ss.usermodel.CellStyle sRole(
            org.apache.poi.xssf.usermodel.XSSFWorkbook wb,
            byte[] bg, byte[] fg) {
        org.apache.poi.ss.usermodel.CellStyle s = wb.createCellStyle();
        org.apache.poi.xssf.usermodel.XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 11);
        f.setColor(new org.apache.poi.xssf.usermodel.XSSFColor(fg, null));
        s.setFont(f);
        ((org.apache.poi.xssf.usermodel.XSSFCellStyle)s).setFillForegroundColor(
                new org.apache.poi.xssf.usermodel.XSSFColor(bg, null));
        s.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        s.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
        s.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        s.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        s.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        s.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        return s;
    }

    private org.apache.poi.ss.usermodel.CellStyle sNombre(
            org.apache.poi.xssf.usermodel.XSSFWorkbook wb) {
        org.apache.poi.ss.usermodel.CellStyle s = wb.createCellStyle();
        org.apache.poi.xssf.usermodel.XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 12);
        s.setFont(f);
        ((org.apache.poi.xssf.usermodel.XSSFCellStyle)s).setFillForegroundColor(
                new org.apache.poi.xssf.usermodel.XSSFColor(
                        new byte[]{(byte)255,(byte)255,(byte)255}, null));
        s.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        s.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
        s.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        s.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        s.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        s.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        return s;
    }

    // =========================================================================
    // HELPERS
    // =========================================================================
    private long countByRole(String prefix) {
        return allUsers.stream()
                .filter(u -> u.getRole() != null &&
                        u.getRole().getNomRole().trim().toLowerCase().startsWith(prefix))
                .count();
    }

    private long pct(long value, int total) {
        if (total == 0) return 0;
        return Math.round(value * 100.0 / total);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.getDialogPane().setStyle("-fx-font-size: 13px;");
        a.showAndWait();
    }
}