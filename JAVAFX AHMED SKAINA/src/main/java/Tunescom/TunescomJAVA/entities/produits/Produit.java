package Tunescom.TunescomJAVA.entities.produits;

public class Produit {

    private int id;
    private String nomProduit;
    private String adresse;
    private double prix;
    private int quantite;
    private String imageURL;

    // Constructeur complet
    public Produit(int id, String nomProduit, String adresse, double prix, int quantite, String imageURL) {
        this.id = id;
        this.nomProduit = nomProduit;
        this.adresse = adresse;
        this.prix = prix;
        this.quantite = quantite;
        this.imageURL = imageURL;
    }

    // Constructeur sans id (pour insertion DB)
    public Produit(String nomProduit, String adresse, double prix, int quantite, String imageURL) {
        this.nomProduit = nomProduit;
        this.adresse = adresse;
        this.prix = prix;
        this.quantite = quantite;
        this.imageURL = imageURL;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getNomProduit() {
        return nomProduit;
    }

    public String getAdresse() {
        return adresse;
    }

    public double getPrix() {
        return prix;
    }

    public int getQuantite() {
        return quantite;
    }

    public String getImageURL() {
        return imageURL;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setNomProduit(String nomProduit) {
        this.nomProduit = nomProduit;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public void setPrix(double prix) {
        this.prix = prix;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    @Override
    public String toString() {
        return "Produit{" +
                "id=" + id +
                ", nomProduit='" + nomProduit + '\'' +
                ", adresse='" + adresse + '\'' +
                ", prix=" + prix +
                ", quantite=" + quantite +
                ", imageURL='" + imageURL + '\'' +
                '}';
    }
}
