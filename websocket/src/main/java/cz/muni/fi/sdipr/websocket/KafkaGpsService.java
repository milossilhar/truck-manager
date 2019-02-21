package cz.muni.fi.sdipr.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import cz.muni.fi.sdipr.api.entities.KafkaAuthKeyEntity;
import cz.muni.fi.sdipr.api.exceptions.KafkaMessageFormatException;
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
import java.util.regex.Pattern;

public class KafkaGpsService implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(KafkaGpsService.class);

    private static final String KAFKA_SERVERS = "localhost:9092,localhost:9093,localhost:9094";
    private static final String TOPIC_PREFIX = "company-";

    private static KafkaGpsService instance;
    private static SubscriptionManager subscriptionManager;
    private static KafkaConsumer<String, String> kafkaConsumer;

    public static void initialize(SubscriptionManager manager) {
        if (instance == null) {
            logger.info("Initializing Kafka companies consumer...");
            Properties props = new Properties();
            props.put("bootstrap.servers", KAFKA_SERVERS);
            props.put("group.id", "company_worker");
            props.put("enable.auto.commit", "true");
            props.put("auto.commit.interval.ms", "200");
            props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            kafkaConsumer = new KafkaConsumer<>(props);
            String regex = TOPIC_PREFIX + ".*";
            kafkaConsumer.subscribe(Pattern.compile(regex));
            subscriptionManager = manager;

            logger.info("Running Kafka companies consumer...");
            instance = new KafkaGpsService();
            new Thread(instance).start();
        }
    }

    @Override
    public void run() {
//        while (true) {
//            try {
//                Thread.sleep(3000);
//
//                logger.info("SUBSCRIPTION REPORT: " + subscriptionManager.getAll().size());
//                subscriptionManager.getAll().forEach((key, clients) -> {
//                    logger.info("### KEY: " + key + " , clients: " + clients.size());
//                    clients.forEach((cl) -> {
//                        logger.info("Client " + cl.getSessionWampId() + " , session: " + cl.getSession().getId());
//                    });
//                });
//            } catch (InterruptedException ex) {
//                logger.error("Exception KafkaGPSService: " + ex.getMessage());
//            }
//        }
        while(true) {
            ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : records) {
                String compKey = record.topic().substring(TOPIC_PREFIX.length());
                subscriptionManager.broadcast(compKey, record.value());
            }
        }
    }
}
