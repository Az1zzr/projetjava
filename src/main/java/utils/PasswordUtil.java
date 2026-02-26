package utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    // ✅ Hasher un mot de passe
    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    // ✅ Vérifier mot de passe saisi vs hash en BD
    public static boolean verify(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}