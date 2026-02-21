package services;

import models.Role;
import models.User;
import utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IService<User> {

    private final Connection conn = MyDB.getInstance().getConn();

    // ─── ADD ─────────────────────────────────────────────────────────────────
    public void add(User user) throws SQLException {
        if (isAdminRole(user) && countAdmins() >= 1)
            throw new SQLException("Un seul administrateur est autorisé !");

        String sql = "INSERT INTO user (nom, prenom, email, motDePasse, date_naissance, id_role, photo_profil) " +
                "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom() != null ? user.getPrenom() : "");
            ps.setString(3, user.getEmail().toLowerCase());
            ps.setString(4, user.getMotDePasse());
            ps.setDate(5, user.getDateNaissance() != null ? Date.valueOf(user.getDateNaissance()) : null);
            if (user.getRole() != null) ps.setInt(6, user.getRole().getId_role());
            else ps.setNull(6, Types.INTEGER);
            ps.setString(7, user.getPhotoPath());
            ps.executeUpdate();

            // ✅ CRITIQUE : récupérer l'ID généré et le stocker dans l'objet
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) user.setId(keys.getInt(1));
            }
        }
    }

    // ─── GET ALL ─────────────────────────────────────────────────────────────
    public List<User> getAll() throws SQLException {
        return query("""
            SELECT u.id, u.nom, u.prenom, u.email, u.motDePasse, u.date_naissance,
                   u.photo_profil, r.id_role, r.nomRole
            FROM user u LEFT JOIN role r ON u.id_role = r.id_role
            ORDER BY u.nom, u.prenom
            """);
    }

    // ─── GET NON-ADMINS ───────────────────────────────────────────────────────
    public List<User> getNonAdmins() throws SQLException {
        return query("""
            SELECT u.id, u.nom, u.prenom, u.email, u.motDePasse, u.date_naissance,
                   u.photo_profil, r.id_role, r.nomRole
            FROM user u LEFT JOIN role r ON u.id_role = r.id_role
            WHERE LOWER(r.nomRole) != 'administrateur'
            ORDER BY u.nom, u.prenom
            """);
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────
    public void update(User user) throws SQLException {
        String sql = "UPDATE user SET nom=?, prenom=?, email=?, motDePasse=?, " +
                "date_naissance=?, id_role=?, photo_profil=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom() != null ? user.getPrenom() : "");
            ps.setString(3, user.getEmail().toLowerCase());
            ps.setString(4, user.getMotDePasse());
            ps.setDate(5, user.getDateNaissance() != null ? Date.valueOf(user.getDateNaissance()) : null);
            if (user.getRole() != null) ps.setInt(6, user.getRole().getId_role());
            else ps.setNull(6, Types.INTEGER);
            ps.setString(7, user.getPhotoPath());
            ps.setInt(8, user.getId());

            int rows = ps.executeUpdate();
            if (rows == 0)
                throw new SQLException("Aucun utilisateur mis à jour. ID introuvable : " + user.getId());
        }
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────
    public void delete(User user) throws SQLException {
        if (isAdminRole(user) && countAdmins() <= 1)
            throw new SQLException("Impossible de supprimer le seul administrateur !");
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM user WHERE id=?")) {
            ps.setInt(1, user.getId());
            ps.executeUpdate();
        }
    }

    // ─── AUTHENTICATE ─────────────────────────────────────────────────────────
    public User authenticate(String email, String password) throws SQLException {
        String sql = """
            SELECT u.id, u.nom, u.prenom, u.email, u.motDePasse, u.date_naissance,
                   u.photo_profil, r.id_role, r.nomRole
            FROM user u LEFT JOIN role r ON u.id_role = r.id_role
            WHERE u.email = ? AND u.motDePasse = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.toLowerCase());
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────
    private List<User> query(String sql) throws SQLException {
        List<User> list = new ArrayList<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    private boolean isAdminRole(User user) {
        return user.getRole() != null
                && user.getRole().getNomRole().equalsIgnoreCase("Administrateur");
    }

    private int countAdmins() throws SQLException {
        String sql = "SELECT COUNT(*) FROM user u JOIN role r ON u.id_role=r.id_role " +
                "WHERE LOWER(r.nomRole)='administrateur'";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setMotDePasse(rs.getString("motDePasse"));
        Date ddn = rs.getDate("date_naissance");
        if (ddn != null) u.setDateNaissance(ddn.toLocalDate());
        // Photo profil (peut être null si colonne pas encore créée)
        try { u.setPhotoPath(rs.getString("photo_profil")); } catch (SQLException ignored) {}
        int idRole = rs.getInt("id_role");
        if (!rs.wasNull()) {
            Role r = new Role();
            r.setId_role(idRole);
            r.setNomRole(rs.getString("nomRole"));
            u.setRole(r);
        }
        return u;
    }
}