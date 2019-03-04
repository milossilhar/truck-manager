package cz.muni.fi.sdipr.websocket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import cz.muni.fi.sdipr.api.entities.WampSessionEntity;
import cz.muni.fi.sdipr.api.exceptions.WampMessageFormatException;
import cz.muni.fi.sdipr.api.factories.AuthManagerFactory;
import cz.muni.fi.sdipr.api.factories.SubscriptionManagerFactory;
import cz.muni.fi.sdipr.api.managers.AuthManager;
import cz.muni.fi.sdipr.api.managers.SubscriptionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.wamp.jawampa.MessageType;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Main websocket server endpoint for the application.
 * @author Milos Silhar (433614)
 */
@ServerEndpoint(value = "/socket", subprotocols = {"wamp.2.json"})
public class GpsServerEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(GpsServerEndpoint.class);

    // valid wamp sessions that are currently active [key: websocket session id, value: wamp session]
    private static ConcurrentMap<String, WampSessionEntity> sessions = new ConcurrentHashMap<>();

    private static AuthManager authManager = AuthManagerFactory.getInstance();

    private static SubscriptionManager subscriptionManager = SubscriptionManagerFactory.getInstance();

    @OnOpen
    public void onOpen(Session session) {
        logger.info("Connected client: " + session.getId());
        sessions.putIfAbsent(session.getId(), new WampSessionEntity(session));
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        JsonParser parser = new JsonParser();
        try {
            JsonElement element = parser.parse(message);
            JsonArray array = element.getAsJsonArray();
            int messageType = array.get(0).getAsInt();
            switch (messageType) {
                case MessageType.HELLO:
                    logger.info("HELLO received");
                    sessions.get(session.getId()).helloReceived();
                    break;
                case MessageType.GOODBYE:
                    logger.info("GOODBYE received");
                    sessions.get(session.getId()).goodbyeReceived();
                    break;
                case MessageType.ABORT:
                    logger.info("ABORT received");
                    sessions.get(session.getId()).abortReceived();
                    break;
                case MessageType.CALL:
                    logger.info("CALL received");
                    JsonArray arguments = array.get(4).getAsJsonArray(); // get Arguments|list
                    List<String> positionalArguments = new ArrayList<>();
                    for(int i = 0; i < arguments.size(); i++) {
                        positionalArguments.add(arguments.get(i).getAsString());
                    }
                    sessions.get(session.getId()).callReceived(
                            Long.valueOf(array.get(1).getAsString()), // get Request|id
                            array.get(3).getAsString(), // get Procedure|uri
                            positionalArguments);
                    break;
                case MessageType.SUBSCRIBE:
                    logger.info("SUBSCRIBE received");
                    sessions.get(session.getId()).subscribeReceived(
                            Long.valueOf(array.get(1).getAsString()), // get Request|id
                            array.get(3).getAsString()); // get Topic|uri
                    break;
            }
        } catch (JsonParseException | IllegalStateException | ClassCastException | IndexOutOfBoundsException ex) {
            throw new WampMessageFormatException(ex.getMessage(), ex.getCause());
        }
    }

    @OnClose
    public void onClose(Session session) {
        logger.info("Closing client: "  + session.getId());
        WampSessionEntity wampSession = sessions.get(session.getId());
        String authKey = wampSession.getAuthKey();
        String compKey = wampSession.getCompKey();
        if (authKey != null) {
            authManager.removeClient(authKey, wampSession);
        }
        //if (compKey != null) {
        //    subscriptionManager.removeSubscription(compKey, wampSession);
        //}
        sessions.remove(session.getId());
    }

    @OnError
    public void onError(Throwable error) {
        error.printStackTrace();
        logger.error("Some error");
    }
}
