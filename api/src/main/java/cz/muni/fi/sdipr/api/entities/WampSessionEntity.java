package cz.muni.fi.sdipr.api.entities;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import cz.muni.fi.sdipr.api.enums.WampStatesEnum;
import cz.muni.fi.sdipr.api.exceptions.WebSocketSessionClosedException;
import cz.muni.fi.sdipr.api.managers.AuthManager;
import cz.muni.fi.sdipr.api.managers.SubscriptionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.wamp.jawampa.MessageType;

import javax.websocket.Session;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Basic implementation to WAMP protocol.
 * Does NOT complies to even Basic Profile.
 * Does NOT implement Realms.
 *
 * @author Milos Silhar (433614)
 */
public class WampSessionEntity {
    private final Logger logger = LoggerFactory.getLogger(WampSessionEntity.class);

    private Session session;
    private String authKey;
    private String compKey;
    private String sessionWampId;
    private WampStatesEnum currentState; //indicates current session state
    private AuthManager authManager;
    private SubscriptionManager subscriptionManager;

    public WampSessionEntity(Session session, AuthManager authManager, SubscriptionManager subscriptionManager) {
        if (session == null) {
            throw new NullPointerException("session is null");
        }
        if (authManager == null) {
            throw new NullPointerException("manager is null");
        }
        this.currentState = WampStatesEnum.CLOSED;
        this.session = session;
        this.sessionWampId = randomString(session.getId());
        this.authManager = authManager;
        this.subscriptionManager = subscriptionManager;
    }

    public String getAuthKey() {
        return authKey;
    }

    public String getCompKey() {
        return compKey;
    }

    public Session getSession() {
        return session;
    }

    public String getSessionWampId() {
        return sessionWampId;
    }

    /**
     * Received HELLO from the client, Realm and Details are omitted.
     * Sends back WELCOME message.
     */
    public void helloReceived() {
        if (currentState == WampStatesEnum.ESTABLISHED) {
            sendProtocolViolation("Receiving HELLO message, after session was established.");
        }
        if (currentState == WampStatesEnum.CLOSED) {
            sendWelcome();
            currentState = WampStatesEnum.ESTABLISHED;
        }

    }

    /**
     * Received GOODBYE from client, Details and Reason are omitted.
     * Sends back GOODBYE message.
     */
    public void goodbyeReceived() {
        if (currentState == WampStatesEnum.CLOSING || currentState == WampStatesEnum.SHUTTING_DOWN) {
            currentState = WampStatesEnum.CLOSED;
        } else if (currentState != WampStatesEnum.ESTABLISHED) {
            sendProtocolViolation("Receiving GOODBYE message, before session was established.");
        } else {
            sendGoodbye();
            currentState = WampStatesEnum.CLOSING;
        }
    }

    /**
     * Received ABORT from client, Details and Reason are omitted.
     * Just closes the Session.
     */
    public void abortReceived() {
        currentState = WampStatesEnum.CLOSED;
    }

    /**
     * Received CALL message from client, already parsed but Options are omitted.
     * @param requestId Unique ID of request, needed for the response.
     * @param procedure Name of procedure called.
     * @param arguments Positional arguments list from the client.
     */
    public void callReceived(Long requestId, String procedure, List<String> arguments) {
        if (currentState != WampStatesEnum.ESTABLISHED) {
            sendProtocolViolation("Receiving CALL message, but session was not established");
            return;
        }
        if (procedure.equals("auth")) {
            String authKey = arguments.get(0);
            String compKey = arguments.get(1);
            if (authKey.equals("anonym") && compKey.equals("ibm")) {
                this.authKey = authKey;
                authManager.addClient(authKey, this);
                sendCallResult(requestId, "ok");
                logger.info("Authenticated client " + sessionWampId);
                logger.info("Clients of company " + compKey + " are: " + authManager.getValue(authKey).getClients().toString());
            } else if (authManager.hasAuthKey(authKey) && authManager.getCompanyKey(authKey).equals(compKey)) {
                this.authKey = authKey;
                authManager.addClient(authKey, this);
                sendCallResult(requestId, "ok");
                logger.info("Authenticated client " + sessionWampId);
                logger.info("Clients of company " + compKey + " are: " + authManager.getValue(authKey).getClients().toString());
            } else {
                sendCallError(requestId, "wamp.error.authorization_failed", "Invalid credentials");
                closeSession();
                logger.warn("Authentication failed");
            }
            return;
        }

        sendCallError(requestId, "wamp.error.no_such_procedure", "You are not allowed to make calls");
        closeSession();
        logger.warn("Unsupported method call");
    }

    /**
     * Received SUBSCRIBE message from client, already parsed but Options are omitted.
     * @param requestId Unique ID of request, needed for the response.
     * @param topic Name of topic to subscribe to.
     */
    public void subscribeReceived(Long requestId, String topic) {
        if (currentState != WampStatesEnum.ESTABLISHED) {
            sendProtocolViolation("Receiving SUBSCRIBE message, but session was not established");
            return;
        }
        if (authKey == null) {
            logger.info("Unauthorized subscription to " + topic);
            closeSession();
            return;
        }
        logger.info("Received SUBSCRIBE: " + requestId + " | " + topic);
        this.compKey = topic;
        subscriptionManager.addSubscription(topic, this);
        sendSubscribed(requestId, topic);
    }

    /**
     * Closes session by issuing GOODBYE message to client
     */
    public void closeSession() {
        if (currentState == WampStatesEnum.ESTABLISHED) {
            sendGoodbye();
            currentState = WampStatesEnum.SHUTTING_DOWN;
        }
    }

    /**
     * Sends new event to this client
     * @param topic
     * @param jsonData Data must be in json format
     */
    public void sendEvent(String topic, String jsonData) {
        JsonArray wampArray = new JsonArray();

        wampArray.add(MessageType.EVENT); // EVENT
        wampArray.add(topic.hashCode()); // SUBSCRIBED.Subscription|id
        wampArray.add("kafka".hashCode()); // PUBLISHED.Publication|id
        wampArray.add(new JsonObject()); // Details|dict
        wampArray.add(new JsonArray()); // PUBLISH.Arguments|list
        wampArray.add(jsonData); // PUBLISH.ArgumentKw|dict

        sendMessageToSession(wampArray.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WampSessionEntity)) return false;
        WampSessionEntity that = (WampSessionEntity) o;
        return Objects.equals(sessionWampId, that.sessionWampId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionWampId);
    }

    @Override
    public String toString() {
        return "WampSessionEntity{" +
                "sessionID='" + session.getId() + '\'' +
                ", wampID='" + sessionWampId + '\'' +
                ", authKey='" + authKey + '\'' +
                ", compKey='" + compKey + '\'' +
                '}';
    }

    // sends WELCOME message to client with new wamp session id created
    private void sendWelcome() {
        JsonArray wampArray = new JsonArray();
        JsonObject rolesWrap = new JsonObject();
        JsonObject rolesDict = new JsonObject();

        rolesDict.add("broker", new JsonObject());
        rolesDict.add("dealer", new JsonObject());
        rolesWrap.add("roles", rolesDict);

        wampArray.add(MessageType.WELCOME); // WELCOME
        wampArray.add(this.sessionWampId); // Session|id
        wampArray.add(rolesWrap); // Details|dict

        sendMessageToSession(wampArray.toString());
    }

    // sends GOODBYE message to client
    private void sendGoodbye() {
        JsonArray wampArray = new JsonArray();

        wampArray.add(MessageType.GOODBYE);
        wampArray.add(new JsonObject());
        wampArray.add("wamp.close.goodbye_and_out");

        sendMessageToSession(wampArray.toString());
    }

    // sends Call RESULT message to client
    private void sendCallResult(Long requestId, String resultValue) {
        JsonArray wampArray = new JsonArray();
        JsonObject result = new JsonObject();

        result.addProperty("result", resultValue); // always as {"result": "resultValue"}

        wampArray.add(MessageType.RESULT); // RESULT
        wampArray.add(requestId); // CALL.Request|id
        wampArray.add(new JsonObject()); // Details|dict
        wampArray.add(new JsonArray()); // YIELD.Arguments|list
        wampArray.add(result); // YIELD.ArgumentsKw|dict

        sendMessageToSession(wampArray.toString());
    }

    // sends Call ERROR message to client
    private void sendCallError(Long requestId, String errorUri, String desc) {
        JsonArray wampArray = new JsonArray();
        JsonArray arguments = new JsonArray();

        arguments.add(desc);

        wampArray.add(MessageType.ERROR); // ERROR
        wampArray.add(MessageType.CALL); // CALL
        wampArray.add(requestId); // CALL.Request|id
        wampArray.add(new JsonObject()); // Details|dict
        wampArray.add(errorUri); // Error|Uri
        wampArray.add(arguments); //Arguments|list

        sendMessageToSession(wampArray.toString());
    }

    private void sendSubscribed(Long requestId, String topic) {
        JsonArray wampArray = new JsonArray();

        wampArray.add(MessageType.SUBSCRIBED); // SUBSCRIBED
        wampArray.add(requestId); // SUBSCRIBE.REQUEST|id
        wampArray.add(topic.hashCode()); // Subscription|id

        sendMessageToSession(wampArray.toString());
    }

    // sends json protocol violation ABORT message to client
    private void sendProtocolViolation(String humanReadableMessage) {
        JsonArray wampArray = new JsonArray();
        JsonObject messageObject = new JsonObject();

        messageObject.addProperty("message", humanReadableMessage);

        wampArray.add(MessageType.ABORT); // ABORT
        wampArray.add(messageObject); // Details|dict
        wampArray.add("wamp.error.protocol_violation"); // Reason|uri

        sendMessageToSession(wampArray.toString());
        currentState = WampStatesEnum.CLOSED;
    }

    // sends message to client from session
    private void sendMessageToSession(String message) {
        if (!session.isOpen()) {
            throw new WebSocketSessionClosedException();
        }

        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException ex) {
            logger.error(ex.getMessage());
        }
    }

    private String randomString(String sessionId) {
        StringBuilder sb = new StringBuilder();
        sb.append(UUID.randomUUID().toString().replace("-",""));
        sb.append(sessionId);
        return sb.toString();
    }
}
