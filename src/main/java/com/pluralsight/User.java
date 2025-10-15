package com.pluralsight;

//created a user class to represent a user that uses the ledger, is associated with a transaction, and has
//different permissions for access.
public class User {
    private int id;
    private String name;
    private String pin; // used a String for PIN since
    private boolean adminAccess; // this represents if the user is regular user or superuser

    //User obj constructor sets everything but the admin access.
    //This way every new user would by default be a regular user
    public User(int id, String name, String pin) {
        this.id = id;
        this.name = name;
        this.pin = pin;
        this.adminAccess = false;
    }

    //Getters and Setters for member fields
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public boolean isAdminAccess() {
        return adminAccess;
    }

    //this will be used to set access permissions and make a user an Admin
    public void setAdminAccess(boolean adminAccess) {
        this.adminAccess = adminAccess;
    }
}
