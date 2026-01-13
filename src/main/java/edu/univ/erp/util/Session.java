package edu.univ.erp.util;

import edu.univ.erp.model.AuthUser;

public class Session {
    private static AuthUser currentUser = null;

    public static void setCurrentUser(AuthUser u) { currentUser = u; }
    public static AuthUser getCurrentUser() { return currentUser; }
    public static void clear() { currentUser = null; }
    public static boolean isLoggedIn() { return currentUser != null; }
}

