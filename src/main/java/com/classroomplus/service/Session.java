package com.classroomplus.util;

import com.classroomplus.model.User;

public final class Session {

    private static User currentUser;

    private Session() {
    }

    public static void start(User user) {
        currentUser = user;
    }

    /** Used by the logout button (F-1.3). */
    public static void end() {
        currentUser = null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }
}