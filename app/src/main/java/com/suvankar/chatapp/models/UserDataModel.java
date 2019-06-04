package com.suvankar.chatapp.models;

import com.google.firebase.auth.FirebaseUser;

public class UserDataModel {

    private String id;
    private String email;
    private String name;

    public UserDataModel() {
    }

    public UserDataModel(FirebaseUser user) {
        this.id = user.getUid();
        this.email = user.getEmail();
        this.name = user.getDisplayName();
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "UserDataModel{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
