package cz.muni.fi.sdipr.generator;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import java.util.Random;

/**
 * Hello world!
 *
 */
public class App {
    private static Logger logger = LoggerFactory.getLogger(App.class);

    private static final String KAFKA_SERVERS = "localhost:9092,localhost:9093,localhost:9094";

    public static void main( String[] args ) {
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
        consumerProps.put("group.id", "debug-gps-generator");
        consumerProps.put("enable.auto.commit", "true");
        consumerProps.put("auto.commit.interval.ms", "1000");
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);

        List<String> topics = new ArrayList<>();
        consumer.listTopics().forEach((name, partitions) -> {
            if (name.startsWith("company-")) {
                topics.add(name);
            }
        });

        try (InputStreamReader isr = new InputStreamReader(System.in);
             BufferedReader br = new BufferedReader(isr)) {
            String line = "";
            int i = 0;
            Random random = new Random(Instant.now().getNano());
            while (true) {
                int topicIndex = i % topics.size();
                int longitude = random.nextInt(180);
                int latitude = random.nextInt(90);
                JsonObject gpsLocation = new JsonObject();
                gpsLocation.addProperty("latitude", latitude);
                gpsLocation.addProperty("longitude", longitude);

                logger.info("Send data " + gpsLocation.toString() + " to topic " + topics.get(topicIndex));
                line = br.readLine();
                if (line.equals("exit")) {
                    break;
                }
                producer.send(new ProducerRecord<>(topics.get(topicIndex), String.valueOf(i), gpsLocation.toString()));
                i++;
            }
        } catch (IOException ex) {
            logger.error("IO exception: " + ex.getMessage());
        }

        producer.close();
    }
}
