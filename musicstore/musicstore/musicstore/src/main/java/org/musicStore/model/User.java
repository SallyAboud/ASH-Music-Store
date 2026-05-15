package org.musicStore.model;


public class User {
    private int id;
    private String username;
    private String password;
    private String email;

    public User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void login(String email, String password) throws IllegalArgumentException {
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Email cannot be empty.");
        if (password == null || password.length() < 6)
            throw new IllegalArgumentException("Password must be 6+ characters.");
        System.out.println(username + " logged in.");
    }

    public void logout() {
        System.out.println(username + " logged out.");
    }

    public void updateProfile(String username, String email) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username cannot be blank.");
        this.username = username;
        this.email = email;
    }

    public void makeId() { this.id = (int)(Math.random() * 10000); }


    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', email='" + email + "'}";
    }

}

