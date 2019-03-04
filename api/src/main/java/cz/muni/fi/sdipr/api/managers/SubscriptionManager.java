package cz.muni.fi.sdipr.api.managers;

import cz.muni.fi.sdipr.api.entities.WampSessionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SubscriptionManager {
    private final Logger logger = LoggerFactory.getLogger(SubscriptionManager.class);

    // valid clients that are currently subscribed to particular topic [key: comp_key, value: wamp session]
    private ConcurrentMap<String, Set<WampSessionEntity>> subscriptions = new ConcurrentHashMap<>();

    /**
     * Adds client to subscription to specific company key
     * @param compKey
     * @param session
     */
    public void addSubscription(String compKey, WampSessionEntity session) {
        if (compKey == null) {
            throw new NullPointerException("compKey is null");
        }
        if (session == null) {
            throw new NullPointerException("session is null");
        }

        subscriptions.computeIfPresent(compKey, (key, clients) -> {
            clients.add(session);
            return clients;
        });
        subscriptions.putIfAbsent(compKey, new HashSet<>(Collections.singleton(session)));
    }

    /**
     * Gets all clients for given company key
     * @param compKey
     * @return
     */
    public Set<WampSessionEntity> getClients(String compKey) {
        if (compKey == null) {
            throw new NullPointerException("compKey is null");
        }
        return Collections.unmodifiableSet(subscriptions.getOrDefault(compKey, Collections.emptySet()));
    }

    /**
     * Sends message to every subscribed client
     * @param compKey
     * @param jsonData
     */
    public void broadcast(String compKey, String jsonData) {
        if (compKey == null) {
            throw new NullPointerException("compKey is null");
        }
        if (jsonData == null) {
            throw new NullPointerException("jsonData is null");
        }

        Set<WampSessionEntity> clientsToRemove = new HashSet<>();
        Set<WampSessionEntity> clients = subscriptions.getOrDefault(compKey, null);
        if (clients != null) {
            clients.forEach(client -> {
                if (client.isClosed()) {
                    clientsToRemove.add(client);
                } else {
                    client.sendEvent(compKey, jsonData);
                }
            });
        }
        subscriptions.computeIfPresent(compKey, (key, cl) -> {
            cl.removeAll(clientsToRemove);
            return cl;
        });
    }
}
