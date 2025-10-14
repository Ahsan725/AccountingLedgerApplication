package com.pluralsight;

public class User {
    private int id;
    private String name;
    private String pin;          // now a String (preserves leading zeros)
    private boolean adminAccess; // stays boolean

    public User(int id, String name, String pin) {
        this.id = id;
        this.name = name;
        this.pin = pin;
        this.adminAccess = false;
    }

    // Optional convenience constructor if you want to set admin in one go
    public User(int id, String name, String pin, boolean adminAccess) {
        this.id = id;
        this.name = name;
        this.pin = pin;
        this.adminAccess = adminAccess;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    public boolean isAdminAccess() { return adminAccess; }
    public void setAdminAccess(boolean adminAccess) { this.adminAccess = adminAccess; }
}
