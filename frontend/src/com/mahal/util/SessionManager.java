package com.mahal.util;

import com.mahal.model.User;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    private String authToken;
    
    private SessionManager() {}
    
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    public void setUser(User user, String token) {
        this.currentUser = user;
        this.authToken = token;
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public String getAuthToken() {
        return authToken;
    }
    
    public boolean isLoggedIn() {
        return currentUser != null && authToken != null;
    }
    
    public void logout() {
        this.currentUser = null;
        this.authToken = null;
    }
}

