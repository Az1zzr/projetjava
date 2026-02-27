package Tunescom.TunescomJAVA.services.produits;

import Tunescom.TunescomJAVA.entities.produits.Stock;
import Tunescom.TunescomJAVA.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StockService implements iService<Stock> {

    private final Connection cnx;

    public StockService() {
        this.cnx = MyConnection.getInstance().getConnection();
    }

    @Override
    public void ajouter(Stock s) {
        Stock existing = recupererParProduitId(s.getProduitId());
        if (existing != null) {
            s.setId(existing.getId());
            modifier(s, "");
            return;
        }

        String req = "INSERT INTO stock (produit_id, quantite_disponible) VALUES (?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {

            pst.setInt(1, s.getProduitId());
            pst.setInt(2, s.getQuantite());

            int affectedRows = pst.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pst.getGeneratedKeys()) {
                    if (rs.next()) {
                        s.setId(rs.getInt(1));
                    }
                }
                updateProduitQuantite(s.getProduitId(), s.getQuantite());
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du stock : " + e.getMessage());
        }
    }

    @Override
    public void supprimer(Stock s) {
        int produitId = s.getProduitId();
        if (produitId <= 0) {
            Stock existing = recupererParId(s.getId());
            if (existing != null) {
                produitId = existing.getProduitId();
            }
        }

        String req = "DELETE FROM stock WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {

            pst.setInt(1, s.getId());

            int rowsDeleted = pst.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("Stock supprimé avec succès !");
                if (produitId > 0) {
                    updateProduitQuantite(produitId, 0);
                }
            } else {
                System.out.println("Aucun stock trouvé avec l'ID: " + s.getId());
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du stock : " + e.getMessage());
        }
    }

    @Override
    public void modifier(Stock s, String ignored) {
        // Le paramètre "ignored" existe car ton iService l'exige.
        String req = "UPDATE stock SET produit_id = ?, quantite_disponible = ? WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {

            pst.setInt(1, s.getProduitId());
            pst.setInt(2, s.getQuantite());
            pst.setInt(3, s.getId());

            int rowsUpdated = pst.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Stock modifié avec succès !");
                updateProduitQuantite(s.getProduitId(), s.getQuantite());
            } else {
                System.out.println("Aucun stock trouvé avec l'ID: " + s.getId());
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la modification du stock : " + e.getMessage());
        }
    }

    @Override
    public List<Stock> recuperer() {
        List<Stock> stocks = new ArrayList<>();
        String req = "SELECT * FROM stock";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {

            while (rs.next()) {
                Stock s = new Stock(
                        rs.getInt("id"),
                        rs.getInt("produit_id"),
                        rs.getInt("quantite_disponible")
                );
                stocks.add(s);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des stocks : " + e.getMessage());
        }

        return stocks;
    }

    // ✅ Bonus utile (CRUD+) : récupérer un stock par ID
    public Stock recupererParId(int id) {
        String req = "SELECT * FROM stock WHERE id = ?";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, id);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return new Stock(
                            rs.getInt("id"),
                            rs.getInt("produit_id"),
                            rs.getInt("quantite_disponible")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du stock avec l'ID " + id + " : " + e.getMessage());
        }

        return null;
    }

    // ✅ Bonus très pratique : récupérer le stock d’un produit
    public Stock recupererParProduitId(int produitId) {
        String req = "SELECT * FROM stock WHERE produit_id = ?";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, produitId);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return new Stock(
                            rs.getInt("id"),
                            rs.getInt("produit_id"),
                            rs.getInt("quantite_disponible")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du stock pour produit_id " + produitId + " : " + e.getMessage());
        }

        return null;
    }

    private void updateProduitQuantite(int produitId, int quantite) {
        String req = "UPDATE produit SET quantite = ? WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, quantite);
            pst.setInt(2, produitId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la synchro quantite produit: " + e.getMessage());
        }
    }
}
