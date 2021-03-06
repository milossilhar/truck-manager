package cz.muni.fi.sdipr.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import cz.muni.fi.sdipr.api.entities.KafkaAuthKeyEntity;
import cz.muni.fi.sdipr.api.exceptions.KafkaMessageFormatException;
import cz.muni.fi.sdipr.api.managers.AuthManager;
import cz.muni.fi.sdipr.api.managers.SubscriptionManager;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Authorization service that gets and pushes auth keys to Kafka.
 * @author Milos Silhar (433614)
 */
public class KafkaAuthService implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(KafkaAuthService.class);

    public static final String KAFKA_SERVERS = "localhost:9092,localhost:9093,localhost:9094";
    public static final String AUTH_KEY_TOPIC = "auth-keys";
    public static String OPER_INSERT = "insert";
    public static String OPER_UPDATE = "update";
    public static String OPER_DELETE = "delete";

    private static KafkaAuthService instance;
    private static AuthManager authManager;
    private static SubscriptionManager subscriptionManager;
    private static KafkaConsumer<String, String> consumer;
    private static AtomicBoolean keepGoing = new AtomicBoolean(true);
    private static Thread runningThread;

    /**
     * Initializes Kafka Authentication service.
     * Runs instance in standalone thread.
     * Each message has json format:
     *  key: string (auth_key)
     *  value: {
     *  "operation": insert/delete,
     *  "comp_key": string (comp_key),
     *  "expires_at": timestamp
     *  }
     */
    public static void initialize(AuthManager auManager, SubscriptionManager subManager) {
        if (instance == null) {
            logger.info("Initializing Kafka auth consumer...");
            Properties props = new Properties();
            props.put("bootstrap.servers", KAFKA_SERVERS);
            //TODO seek to beginning is not working
            //props.put("group.id", "auth_key_worker");
            props.put("group.id", UUID.randomUUID().toString());
            props.put("enable.auto.commit", "true");
            props.put("auto.commit.interval.ms", "200");
            props.put("auto.offset.reset", "earliest");
            props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Collections.singletonList(AUTH_KEY_TOPIC));
            authManager = auManager;
            subscriptionManager = subManager;

            logger.info("Running Kafka auth consumer...");
            instance = new KafkaAuthService();
            runningThread = new Thread(instance, "KafkaAuthService");
            runningThread.start();
        }
    }

    public static void stop() {
        if (instance != null) {
            logger.info("Stopping Kafka auth service...");
            keepGoing.set(false);
            try {
                runningThread.join();
            } catch (InterruptedException e) {
                logger.error("Kafka Auth Service thread interrupted");
            }
            logger.info("Stopping Kafka auth consumer...");
            consumer.close();
            consumer = null;
            authManager = null;
            subscriptionManager = null;
            instance = null;
            keepGoing.compareAndSet(false, true);
        }
    }

    @Override
    public void run() {
        while (keepGoing.get()) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : records) {
                try {
                    Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).create();
                    KafkaAuthKeyEntity value = gson.fromJson(record.value(), KafkaAuthKeyEntity.class);
                    Instant expiresAt = value.getExpiresAt().equals("") ?
                            null :
                            Instant.parse(value.getExpiresAt());
                    if (value.getOperation().equals(OPER_INSERT)) {
                        if (expiresAt == null || expiresAt.isAfter(Instant.now())) {
                            logger.info("Insert token: " + record.key() + " compKey: " + value.getCompKey());
                            authManager.addToken(record.key(), value.getCompKey(), expiresAt);
                        }
                    } else if (value.getOperation().equals(OPER_UPDATE)) {
                        logger.info("Update token: " + record.key() + " expiresAt: " + expiresAt);
                        authManager.extendToken(record.key(), expiresAt);
                    } else if (value.getOperation().equals(OPER_DELETE)) {
                        logger.info("Remove token: " + record.key());
                        authManager.removeToken(record.key());
                        //subscriptionManager.removeAuthorization(value.getCompKey(), record.key());
                    } else {
                        throw new KafkaMessageFormatException("topic: auth_keys, error: unsupported operation in json");
                    }
                } catch (JsonSyntaxException ex) {
                    throw new KafkaMessageFormatException("topic: auth_keys, error: wrong json format in kafka", ex);
                }
            }
        }
    }
}
