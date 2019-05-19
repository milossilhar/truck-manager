package cz.muni.fi.sdipr.auth;

import com.google.gson.Gson;
import cz.muni.fi.sdipr.api.entities.KafkaAuthKeyEntity;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author Milos Silhar (433614)
 */
public class App {
    private static Logger logger = LoggerFactory.getLogger(App.class);

    private static final String KAFKA_SERVERS = "localhost:9092,localhost:9093,localhost:9094";
    private static final String AUTH_KEY_TOPIC = "auth-keys";

    public static void main( String[] args )
    {
        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", KAFKA_SERVERS);
        producerProps.put("acks", "all");
        producerProps.put("delivery.timeout.ms", 30000);
        producerProps.put("batch.size", 16384);
        producerProps.put("linger.ms", 0);
        producerProps.put("buffer.memory", 33554432);
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        Properties consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", KAFKA_SERVERS);
        consumerProps.put("group.id", "debug-auth-key-generator");
        consumerProps.put("enable.auto.commit", "true");
        consumerProps.put("auto.commit.interval.ms", "1000");
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);

        List<String> topics = new ArrayList<>();
        consumer.listTopics().forEach((name, partitions) -> {
            logger.info("Found topic: " + name + ", partitions: " + partitions.size());
            if (name.startsWith("company-")) {
                topics.add(name.substring(8));
            }
        });

        try (InputStreamReader isr = new InputStreamReader(System.in);
             BufferedReader br = new BufferedReader(isr)) {
            String line = "";
            while (!line.equals("exit")) {
                Gson gson = new Gson();
                KafkaAuthKeyEntity kafkaAuthKeyEntity = new KafkaAuthKeyEntity();

                // reading operation
                logger.info("Choose operation [insert/delete]: ");
                line = br.readLine();
                if (line.equals("insert")) {
                    kafkaAuthKeyEntity.setOperation("insert");
                } else if (line.equals("delete")) {
                    kafkaAuthKeyEntity.setOperation("delete");
                } else {
                    logger.info("Unsupported operation");
                    continue;
                }

                // reading auth_key
                logger.info("Auth key: ");
                line = br.readLine();
                String authKey = line;

                if (line.equals("insert")) {
                    // reading company_key
                    logger.info("Company key: ");
                    line = br.readLine();
                    if (topics.contains(line)) {
                        kafkaAuthKeyEntity.setCompKey(line);
                    } else {
                        logger.info("This company key is not supported");
                        continue;
                    }
                } else {
                    // no company_key needed
                    kafkaAuthKeyEntity.setCompKey("");
                }

                // reading expiration decision
                logger.info("Include expiration (120s) (yes/no): ");
                line = br.readLine();
                if (line.equals("yes")) {
                    kafkaAuthKeyEntity.setExpiresAt(Instant.now().plusSeconds(120).toString());
                } else {
                    kafkaAuthKeyEntity.setExpiresAt("");
                }

                producer.send(new ProducerRecord<>(AUTH_KEY_TOPIC, authKey, gson.toJson(kafkaAuthKeyEntity, KafkaAuthKeyEntity.class)));
            }
        } catch (IOException ex) {
            logger.error("IO exception: " + ex.getMessage());
        }

        producer.close();
    }
}
