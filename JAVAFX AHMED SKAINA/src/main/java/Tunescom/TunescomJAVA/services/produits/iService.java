package Tunescom.TunescomJAVA.services.produits;


import java.sql.SQLException;
import java.util.List;

public interface iService<T> {
    void ajouter(T p) throws SQLException;
    void supprimer(T p);
    void modifier(T p,String nom);
    List<T> recuperer() throws SQLException;
}