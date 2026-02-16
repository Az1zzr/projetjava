package org.example;

import models.User;
import models.Role;

import services.IService;
import services.UserService;

import utils.MyDB;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {

        /*IService<User> service = new UserService();
        Role role = new Role();
        role.setId_role(11);

        User p = new User(25, "Leao", "Rafael@gmail.com", "*********", role);


        service.add(p);


        System.out.println(service.getAll());*/
        IService<User> service = new UserService();

        Role role = new Role();
        role.setId_role(1); // assure-toi que 1 existe

        User u = new User();
        u.setNom("Rafael");
        u.setEmail("rafael@gmail.com");
        u.setMotDePasse("1234");
        u.setRole(role);

        service.add(u);

        System.out.println(service.getAll());




    }
}