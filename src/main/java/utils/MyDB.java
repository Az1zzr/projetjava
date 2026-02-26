package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDB {
    private static MyDB instance;
    private Connection conn;

    // ✅ allowPublicKeyRetrieval=true résout les erreurs SSL avec MySQL 8
    private final String URL  = "jdbc:mysql://localhost:3306/markethub_db";
    private final String USER     = "root";
    private final String PASSWORD = "";

    private MyDB() {
        connect();
    }

    private void connect() {
        // Charger le driver MySQL
        String[] drivers = {
                "com.mysql.cj.jdbc.Driver",  // MySQL Connector/J 8+
                "com.mysql.jdbc.Driver"       // MySQL Connector/J 5.x
        };

        for (String driver : drivers) {
            try {
                Class.forName(driver);
                System.out.println("✅ Driver chargé : " + driver);
                break;
            } catch (ClassNotFoundException e) {
                System.out.println("⚠️ Driver non trouvé : " + driver);
            }
        }

        try {
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            conn.setAutoCommit(true);
            System.out.println("✅ Connexion MySQL réussie !");
        } catch (SQLException e) {
            System.err.println("❌ Erreur connexion MySQL : " + e.getMessage());
            System.err.println("   → Vérifiez que MySQL est démarré (XAMPP/WAMP)");
            System.err.println("   → Base : markethub_db  |  User : root");
            System.err.println("   → URL  : " + URL);
            conn = null;
        }
    }

    public static MyDB getInstance() {
        if (instance == null) {
            synchronized (MyDB.class) {
                if (instance == null) instance = new MyDB();
            }
        }
        return instance;
    }

    /**
     * Retourne une connexion valide.
     * Lance une RuntimeException explicite si MySQL est inaccessible,
     * pour avoir un message d'erreur clair dans l'UI.
     */
    public Connection getConn() {
        try {
            if (conn == null || conn.isClosed() || !conn.isValid(3)) {
                System.out.println("🔄 Reconnexion MySQL...");
                connect();
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Vérification connexion échouée : " + e.getMessage());
            connect();
        }

        if (conn == null) {
            throw new RuntimeException(
                    "❌ Impossible de se connecter à MySQL.\n" +
                            "Vérifiez que XAMPP/WAMP est démarré et que la base 'markethub_db' existe."
            );
        }
        return conn;
    }
}