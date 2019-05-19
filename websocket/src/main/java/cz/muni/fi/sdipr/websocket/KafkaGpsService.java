package cz.muni.fi.sdipr.websocket;

import cz.muni.fi.sdipr.api.managers.SubscriptionManager;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class KafkaGpsService implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(KafkaGpsService.class);

    private static final String KAFKA_SERVERS = "localhost:9092,localhost:9093,localhost:9094";
    private static final String TOPIC_PREFIX = "company-";

    private static KafkaGpsService instance;
    private static SubscriptionManager subscriptionManager;
    private static KafkaConsumer<String, String> kafkaConsumer;
    private static AtomicBoolean keepGoing = new AtomicBoolean(true);
    private static Thread runningThread;

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
            runningThread = new Thread(instance, "KafkaGPSService");
            runningThread.start();
        }
    }

    public static void stop() {
        if (instance != null) {
            logger.info("Stopping Kafka companies service...");
            keepGoing.set(false);
            try {
                runningThread.join();
            } catch (InterruptedException e) {
                logger.error("Kafka GPS Service thread interrupted");
            }
            logger.info("Stopping Kafka companies consumer...");
            kafkaConsumer.close();
            kafkaConsumer = null;
            subscriptionManager = null;
            instance = null;
            keepGoing.compareAndSet(false, true);
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
        while(keepGoing.get()) {
            ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : records) {
                String compKey = record.topic().substring(TOPIC_PREFIX.length());
                subscriptionManager.broadcast(compKey, record.value());
            }
        }
    }
}
