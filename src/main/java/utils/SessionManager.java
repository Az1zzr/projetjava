package utils;

import models.User;

/**
 * Singleton — garde l'utilisateur connecté en mémoire.
 */
public class SessionManager {

    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void setCurrentUser(User user) { this.currentUser = user; }
    public User getCurrentUser()          { return currentUser; }
    public void logout()                  { currentUser = null; }

    /**
     * ✅ Vérifie si le rôle commence par "fournisseur" (gère "fournisseur" ET "fournisseurs")
     */
    public boolean isFournisseur() {
        return currentUser != null && currentUser.getRole() != null
                && currentUser.getRole().getNomRole().trim().toLowerCase().startsWith("fournisseur");
    }

    /**
     * ✅ Vérifie si le rôle commence par "entrepreneur" (gère "entrepreneur" ET "entrepreneurs")
     */
    public boolean isEntrepreneur() {
        return currentUser != null && currentUser.getRole() != null
                && currentUser.getRole().getNomRole().trim().toLowerCase().startsWith("entrepreneur");
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() != null
                && currentUser.getRole().getNomRole().trim().equalsIgnoreCase("admin");
    }

    /** ✅ Retourne true si l'utilisateur est Fournisseur OU Entrepreneur (pas admin) */
    public boolean isRegularUser() {
        return isFournisseur() || isEntrepreneur();
    }

    public String getRoleName() {
        if (currentUser == null || currentUser.getRole() == null) return "";
        return currentUser.getRole().getNomRole();
    }
}