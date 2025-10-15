package com.pluralsight;

//created a user class to represent a user that uses the ledger, is associated with a transaction, and has
//different permissions to access ledger.
//Main idea is that a user should only be able to look at their own transactions, make transactions only on their
//own behalf and be restricted from viewing transactions of other users.
//Admin or superusers should be able to view all transactions and perform admin operations
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

    public User(int id, String name, String pin, boolean adminAccess) {
        this.id = id;
        this.name = name;
        this.pin = pin;
        this.adminAccess = adminAccess;
    }

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

    //used to change admin access
    public void setAdminAccess(boolean adminAccess) {
        this.adminAccess = adminAccess;
    }
}
