package cz.muni.fi.sdipr.api.factories;

import cz.muni.fi.sdipr.api.managers.SubscriptionManager;

/**
 * Implements singleton on Subscription Manager.
 * @author Milos Silhar (433614)
 */
public class SubscriptionManagerFactory {
    private static SubscriptionManager instance = new SubscriptionManager();

    public static SubscriptionManager getInstance() {
        return instance;
    }

    private SubscriptionManagerFactory() {
    }
}
