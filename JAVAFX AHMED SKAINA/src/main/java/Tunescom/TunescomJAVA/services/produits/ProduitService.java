package Tunescom.TunescomJAVA.services.produits;

import Tunescom.TunescomJAVA.entities.produits.Produit;
import Tunescom.TunescomJAVA.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitService implements iService<Produit> {

    private final Connection cnx;

    public ProduitService() {
        this.cnx = MyConnection.getInstance().getConnection();
    }

    @Override
    public void ajouter(Produit p) {
        String req = "INSERT INTO produit (nom_produit, adresse, prix, quantite, image_url) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {

            pst.setString(1, p.getNomProduit());
            pst.setString(2, p.getAdresse());
            pst.setDouble(3, p.getPrix());
            pst.setInt(4, p.getQuantite());
            pst.setString(5, p.getImageURL());

            int affectedRows = pst.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pst.getGeneratedKeys()) {
                    if (rs.next()) {
                        p.setId(rs.getInt(1));
                    }
                }
                upsertStockForProduit(p);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du produit : " + e.getMessage());
        }
    }

    @Override
    public void supprimer(Produit p) {
        deleteStockForProduitId(p.getId());

        String req = "DELETE FROM produit WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, p.getId());

            int rowsDeleted = pst.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("Produit supprimé avec succès !");
            } else {
                System.out.println("Aucun produit trouvé avec l'ID: " + p.getId());
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du produit : " + e.getMessage());
        }
    }

    @Override
    public void modifier(Produit p, String ignored) {
        // Le paramètre "ignored" existe car ton iService l'exige.
        // Ici on modifie le produit avec les valeurs présentes dans l'objet p.
        String req = "UPDATE produit SET nom_produit = ?, adresse = ?, prix = ?, quantite = ?, image_url = ? WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {

            pst.setString(1, p.getNomProduit());
            pst.setString(2, p.getAdresse());
            pst.setDouble(3, p.getPrix());
            pst.setInt(4, p.getQuantite());
            pst.setString(5, p.getImageURL());
            pst.setInt(6, p.getId());

            int rowsUpdated = pst.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Produit modifié avec succès !");
                upsertStockForProduit(p);
            } else {
                System.out.println("Aucun produit trouvé avec l'ID: " + p.getId());
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la modification du produit : " + e.getMessage());
        }
    }

    @Override
    public List<Produit> recuperer() {
        List<Produit> produits = new ArrayList<>();
        String req = "SELECT * FROM produit";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {

            while (rs.next()) {
                Produit p = new Produit(
                        rs.getInt("id"),
                        rs.getString("nom_produit"),
                        rs.getString("adresse"),
                        rs.getDouble("prix"),
                        rs.getInt("quantite"),
                        rs.getString("image_url")
                );
                produits.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des produits : " + e.getMessage());
        }

        return produits;
    }

    // ✅ Bonus utile (CRUD+) : récupérer un produit par ID
    public Produit recupererParId(int id) {
        String req = "SELECT * FROM produit WHERE id = ?";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, id);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return new Produit(
                            rs.getInt("id"),
                            rs.getString("nom_produit"),
                            rs.getString("adresse"),
                            rs.getDouble("prix"),
                            rs.getInt("quantite"),
                            rs.getString("image_url")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du produit avec l'ID " + id + " : " + e.getMessage());
        }

        return null;
    }

    private void upsertStockForProduit(Produit p) {
        String select = "SELECT id FROM stock WHERE produit_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(select)) {
            pst.setInt(1, p.getId());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String update = "UPDATE stock SET quantite_disponible = ? WHERE produit_id = ?";
                    try (PreparedStatement up = cnx.prepareStatement(update)) {
                        up.setInt(1, p.getQuantite());
                        up.setInt(2, p.getId());
                        up.executeUpdate();
                    }
                } else {
                    String insert = "INSERT INTO stock (produit_id, quantite_disponible) VALUES (?, ?)";
                    try (PreparedStatement ins = cnx.prepareStatement(insert)) {
                        ins.setInt(1, p.getId());
                        ins.setInt(2, p.getQuantite());
                        ins.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la synchro stock produit: " + e.getMessage());
        }
    }

    private void deleteStockForProduitId(int produitId) {
        String req = "DELETE FROM stock WHERE produit_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, produitId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression stock produit: " + e.getMessage());
        }
    }
}
