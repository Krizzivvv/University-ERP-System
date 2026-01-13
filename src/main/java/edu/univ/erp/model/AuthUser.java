package edu.univ.erp.model;

public class AuthUser {
    private final int userId;
    private final String username;
    private final String role;

    public AuthUser(int userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }

    @Override
    public String toString() {
        return "AuthUser{" + "userId=" + userId + ", username='" + username + '\'' + ", role='" + role + '\'' + '}';
    }
}
