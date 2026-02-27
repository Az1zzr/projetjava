package Tunescom.TunescomJAVA.entities.produits;

public class Stock {
    private int id;
    private int produitId;
    private int quantite;

    public Stock() {}

    public Stock(int id, int produitId, int quantite) {
        this.id = id;
        this.produitId = produitId;
        this.quantite = quantite;
    }

    public Stock(int produitId, int quantite) {
        this.produitId = produitId;
        this.quantite = quantite;
    }

    public int getId() { return id; }
    public int getProduitId() { return produitId; }
    public int getQuantite() { return quantite; }

    public void setId(int id) { this.id = id; }
    public void setProduitId(int produitId) { this.produitId = produitId; }
    public void setQuantite(int quantite) { this.quantite = quantite; }
}
