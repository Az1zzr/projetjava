package services;

import java.sql.SQLException;
import java.util.List;

/**
 * Interface générique CRUD.
 * Les méthodes déclarent SQLException pour être compatibles
 * avec les implémentations qui accèdent à la base de données.
 */
public interface IService<T> {

    void add(T t) throws SQLException;

    void update(T t) throws SQLException;

    void delete(T t) throws SQLException;

    List<T> getAll() throws SQLException;
}
