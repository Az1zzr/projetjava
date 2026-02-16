package services;

import models.User;
import models.Role;
import utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IService<User> {

    private Connection c;

    public UserService() {
        this.c = MyDB.getInstance().getConn();
    }

    // ================= ADD =================
    @Override
    public void add(User user) {

        String sql = "INSERT INTO user (nom, email, motDePasse, id_role) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, user.getNom());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getMotDePasse());
            ps.setInt(4, user.getRole().getId_role());

            ps.executeUpdate();
            System.out.println("Utilisateur ajouté avec succès ");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // ================= UPDATE =================
    @Override
    public void update(User user) {

        String sql = "UPDATE user SET nom=?, email=?, motDePasse=?, id_role=? WHERE id=?";

        try (PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, user.getNom());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getMotDePasse());
            ps.setInt(4, user.getRole().getId_role());
            ps.setInt(5, user.getId());

            ps.executeUpdate();
            System.out.println("Utilisateur modifié avec succès ✅");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // ================= DELETE =================
    @Override
    public void delete(User user) {

        String sql = "DELETE FROM user WHERE id=?";

        try (PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, user.getId());
            ps.executeUpdate();

            System.out.println("Utilisateur supprimé avec succès ✅");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // ================= GET ALL =================
    @Override
    public List<User> getAll() {

        List<User> users = new ArrayList<>();

        String sql = """
        SELECT u.id, u.nom, u.email, u.motDePasse,
               r.id_role, r.nomRole
        FROM user u
        JOIN role r ON u.id_role = r.id_role
        """;


        try (Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {

                Role role = new Role();
                role.setId_role(rs.getInt("id_role"));
                role.setNomRole(rs.getString("nomRole"));

                User u = new User();
                u.setId(rs.getInt("id"));
                u.setNom(rs.getString("nom"));
                u.setEmail(rs.getString("email"));
                u.setMotDePasse(rs.getString("motDePasse"));
                u.setRole(role);

                users.add(u);
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return users;
    }
}
