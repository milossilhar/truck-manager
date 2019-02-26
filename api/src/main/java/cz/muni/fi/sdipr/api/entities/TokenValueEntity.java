package cz.muni.fi.sdipr.api.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Milos Silhar (433614)
 */
public class TokenValueEntity {
    private String compKey;
    private Instant expiresAt;
    private List<WampSessionEntity> clients = new ArrayList<>();

    public TokenValueEntity(String compKey, Instant expiresAt) {
        if (compKey == null) {
            throw new NullPointerException("compKey is null");
        }
        if (expiresAt == null) {
            throw new NullPointerException("expiresAt is null");
        }

        this.compKey = compKey;
        this.expiresAt = expiresAt;
    }

    public String getCompKey() {
        return compKey;
    }

    public Instant getExpiresAt() { return expiresAt; }

    public List<WampSessionEntity> getClients() {
        return Collections.unmodifiableList(clients);
    }

    public boolean addClient(WampSessionEntity client) {
        if (client == null) {
            throw new NullPointerException("client is null");
        }
        return clients.add(client);
    }

    public boolean removeClient(WampSessionEntity client) {
        if (client == null) {
            throw new NullPointerException("client is null");
        }
        return clients.remove(client);
    }
}
