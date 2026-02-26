package org.example;

import models.Role;
import models.User;
import services.UserService;
import utils.PasswordUtil;
import java.sql.SQLException;
import java.time.LocalDate;

public class Main {
    public static void main(String[] args) throws SQLException {

        // ✅ Utilise UserService directement — évite l'incompatibilité IService<User>
        UserService service = new UserService();

        Role role = new Role();
        role.setId_role(1);

        User u = new User();
        u.setNom("Leao");
        u.setPrenom("Rafael");
        u.setEmail("rafael@gmail.com");
        u.setMotDePasse("1234");
        u.setDateNaissance(LocalDate.of(1999, 6, 15));
        u.setRole(role);
        // ✅ Génère le hash et affiche-le dans la console
        System.out.println(utils.PasswordUtil.hash("Admis200"));

        // Lance l'app normalement après
        //launch(args);

        service.add(u);
        System.out.println("✅ Utilisateur ajouté !");

        System.out.println("\n📋 Liste :");
        service.getAll().forEach(System.out::println);
    }
}