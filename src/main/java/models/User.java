package models;

import java.time.LocalDate;

public class User {

    private int       id;
    private String    nom;
    private String    prenom;
    private String    email;
    private String    telephone;  // ✅ Nouveau champ pour le SMS
    private String    motDePasse;
    private LocalDate dateNaissance;
    private Role      role;
    private String    photoPath;   // ✅ Photo de profil

    public User() {}

    public User(String nom, String prenom, String email, String motDePasse,
                LocalDate dateNaissance, Role role) {
        this.nom = nom; this.prenom = prenom; this.email = email;
        this.motDePasse = motDePasse; this.dateNaissance = dateNaissance; this.role = role;
    }

    public int       getId()              { return id; }
    public void      setId(int id)        { this.id = id; }

    public String    getNom()             { return nom; }
    public void      setNom(String n)     { this.nom = n; }

    public String    getPrenom()          { return prenom; }
    public void      setPrenom(String p)  { this.prenom = p; }

    public String    getEmail()           { return email; }
    public void      setEmail(String e)   { this.email = e; }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String    getMotDePasse()           { return motDePasse; }
    public void      setMotDePasse(String mp)  { this.motDePasse = mp; }

    public LocalDate getDateNaissance()              { return dateNaissance; }
    public void      setDateNaissance(LocalDate d)   { this.dateNaissance = d; }

    public Role      getRole()            { return role; }
    public void      setRole(Role r)      { this.role = r; }

    public String    getPhotoPath()            { return photoPath; }
    public void      setPhotoPath(String p)    { this.photoPath = p; }

    public String getNomComplet() {
        return (prenom != null ? prenom + " " : "") + (nom != null ? nom : "");
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", nom='" + getNomComplet() + "', email='" + email + "'}";
    }
}