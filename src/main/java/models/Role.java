package models;

public class Role {
    private int id_role;
    private String nomRole;

    public Role() {}

    public Role(String nomRole) {
        this.nomRole = nomRole;
    }

    public int getId_role() {
        return id_role;
    }

    public void setId_role(int id) {
        this.id_role = id;
    }

    public String getNomRole() {
        return nomRole;
    }

    public void setNomRole(String nomRole) {
        this.nomRole = nomRole;
    }

    @Override
    public String toString() {
        return nomRole; // utile pour ComboBox
    }

}

