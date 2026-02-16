package services;

import models.Role;
import utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoleService implements IServiceRole<Role>{

    private Connection conn;

    public RoleService() {
        this.conn = MyDB.getInstance().getConn();
    }

    // ================== ADD ==================
    public void add(Role role) {
        String sql = "INSERT INTO role (nomRole) VALUES (?)";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, role.getNomRole());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur ADD Role: " + e.getMessage());
        }
    }

    // ================== UPDATE ==================
    public void update(Role role) {
        String sql = "UPDATE role SET nomRole=? WHERE id_role=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, role.getNomRole());
            pst.setInt(2, role.getId_role());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur UPDATE Role: " + e.getMessage());
        }
    }

    // ================== DELETE ==================
    public void delete(Role role) {
        String sql = "DELETE FROM role WHERE id_role=?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, role.getId_role());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur DELETE Role: " + e.getMessage());
        }
    }

    // ================== GET ALL ==================
    public List<Role> getAll() {
        List<Role> roles = new ArrayList<>();
        String sql = "SELECT * FROM role";
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                Role r = new Role();
                r.setId_role(rs.getInt("id_role"));
                r.setNomRole(rs.getString("nomRole"));
                roles.add(r);
            }
        } catch (SQLException e) {
            System.out.println("Erreur GETALL Role: " + e.getMessage());
        }
        return roles;
    }
}
