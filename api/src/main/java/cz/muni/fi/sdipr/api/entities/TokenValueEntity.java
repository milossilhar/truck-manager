package cz.muni.fi.sdipr.api.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Milos Silhar (433614)
 */
public class TokenValueEntity {
    private String compKey;
    private List<WampSessionEntity> clients = new ArrayList<>();

    public TokenValueEntity(String compKey) {
        if (compKey == null) {
            throw new NullPointerException("compKey is null");
        }

        this.compKey = compKey;
    }

    public String getCompKey() {
        return compKey;
    }

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
