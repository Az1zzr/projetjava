package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDB {
    private static MyDB instance;
    private Connection conn;

    private final String URL = "jdbc:mysql://localhost:3306/markethub_db";
    private final String USER = "root";
    private final String PASSWORD = "";

    private MyDB() {
        try {
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connexion réussie ✅");
        } catch (SQLException e) {
            System.out.println("Erreur de connexion : " + e.getMessage());
        }
    }

    public static MyDB getInstance() {
        if (instance == null) {
            instance = new MyDB();
        }
        return instance;
    }

    public Connection getConn() {
        return conn;
    }
}