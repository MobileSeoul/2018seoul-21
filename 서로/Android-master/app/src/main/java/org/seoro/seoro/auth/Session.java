package org.seoro.seoro.auth;

import org.seoro.seoro.model.User;

public class Session {
    public static final String HOST = "https://whispering-sea-68497.herokuapp.com";

    private static Session mInstance;

    private User mUser;

    public static Session getInstance() {
        if (mInstance == null)  {
            mInstance = new Session();
        }

        return mInstance;
    }

    public User getUser() {
        return mUser;
    }

    public boolean isHaveSession() {
        return mUser != null;
    }

    public void setUser(User user) {
        this.mUser = user;
    }
}
