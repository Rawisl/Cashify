package com.example.cashify.database;

public class User {

    public String email;
    public String uid;
    public String name;


    public User() {
    }


    public User(String email, String uid, String name) {
        this.email = email;
        this.uid = uid;
        this.name = name;
    }
}