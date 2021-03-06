package cz.muni.fi.sdipr.api.managers;

import cz.muni.fi.sdipr.api.entities.TokenValueEntity;
import cz.muni.fi.sdipr.api.entities.WampSessionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AuthManager {
    private final Logger logger = LoggerFactory.getLogger(AuthManager.class);

    // valid tokens that are currently logged on [key: auth_key, value: tokenValueEntity]
    private ConcurrentMap<String, TokenValueEntity> validTokens = new ConcurrentHashMap<>();

    public AuthManager() {
        //TODO remove anonym access
        // adds anonym access, just for develop purposes
        this.addToken("anonym", "ibm", Instant.now().plusSeconds(240));
    }

    /**
     * Adds new token to valid tokens
     * @param authKey
     * @param compKey
     */
    public void addToken(String authKey, String compKey, Instant expiresAt) {
        if (authKey == null) {
            throw new NullPointerException("authKey is null");
        }
        if (compKey == null) {
            throw new NullPointerException("compKey is null");
        }
        validTokens.putIfAbsent(authKey, new TokenValueEntity(compKey, expiresAt));
    }

    /**
     * Removes token from valid tokens and closes all clients
     * @param authKey
     */
    public void removeToken(String authKey) {
        if (authKey == null) {
            throw new NullPointerException("authKey is null");
        }

        TokenValueEntity removedValue = validTokens.remove(authKey);

        if (removedValue != null) {
            for (WampSessionEntity session : removedValue.getClients()) {
                logger.info("Closing client for authKey: " + authKey);
                session.closeSession();
            }
        }
    }

    /**
     * Extends validity of given authorization key
     * @param authKey
     * @param expiresAt
     */
    public void extendToken(String authKey, Instant expiresAt) {
        if (authKey == null) {
            throw new NullPointerException("authKey is null");
        }
        if (expiresAt == null) {
            throw new NullPointerException("expiresAt is null");
        }

        TokenValueEntity valueEntity = getValue(authKey);
        if (valueEntity != null) {
            valueEntity.setExpiresAt(expiresAt);
        }
    }
    /**
     * Checks if there is authorized specified token
     * @param authKey Auth token to check
     * @return True if authorization token is valid, false otherwise
     */
    public boolean hasAuthKey(String authKey) {
        if (authKey == null) {
            throw new NullPointerException("authKey is null");
        }
        return validTokens.getOrDefault(authKey, null) != null;
    }

    /**
     * Gets value for given authorized token
     * @param authKey authorization key to get value for
     * @return authorization value or null if authorization key is not present
     */
    public TokenValueEntity getValue(String authKey) {
        if (authKey == null) {
            throw new NullPointerException("authKey is null");
        }
        return validTokens.getOrDefault(authKey, null);
    }

    /**
     * Gets company key for specified authorization token
     * @param authKey Auth token to get key for
     * @return Company key for given token, null if that auth token is not valid
     */
    public String getCompanyKey(String authKey) {
        TokenValueEntity value = getValue(authKey);
        if (value != null) {
            return value.getCompKey();
        }
        return null;
    }

    /**
     * Adds client to specific authKey
     * @param authKey
     * @param client
     */
    public boolean addClient(String authKey, WampSessionEntity client) {
        if (authKey == null) {
            throw new NullPointerException("authKey is null");
        }
        if (client == null) {
            throw new NullPointerException("client is null");
        }
        return getValue(authKey).addClient(client);
    }

    /**
     * Removes client from specific authKey
     * @param authKey
     * @param client
     * @return
     */
    public boolean removeClient(String authKey, WampSessionEntity client) {
        if (authKey == null) {
            throw new NullPointerException("authKey is null");
        }
        if (client == null) {
            throw new NullPointerException("client is null");
        }

        TokenValueEntity value = getValue(authKey);
        if (value != null) {
            return value.removeClient(client);
        }
        return false;
    }
}
