package cz.muni.fi.sdipr.api.factories;

import cz.muni.fi.sdipr.api.managers.AuthManager;

/**
 * Implements singleton on Authorization Manager.
 * @author Milos Silhar (433614)
 */
public class AuthManagerFactory {
    private static AuthManager instance = new AuthManager();

    public static AuthManager getInstance() {
        return instance;
    }

    private AuthManagerFactory() {
    }
}
