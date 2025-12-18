package com.custom.otp;

import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;

public final class KafkaProducerProvider {

    private static volatile KafkaProducer<String, String> instance;

    private KafkaProducerProvider() {
        // prevent instantiation
    }

    public static KafkaProducer<String, String> getInstance() {
        if (instance == null) {
            synchronized (KafkaProducerProvider.class) {
                if (instance == null) {
                    instance = createProducer();
                }
            }
        }
        return instance;
    }

    private static KafkaProducer<String, String> createProducer() {
        Properties props = new Properties();
        props.put("bootstrap.servers",
                System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092"));
        props.put("key.serializer",
                "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer",
                "org.apache.kafka.common.serialization.StringSerializer");
        props.put("acks", "all");
        props.put("retries", "3");

        return new KafkaProducer<>(props);
    }

    public static void close() {
        if (instance != null) {
            instance.close();
        }
    }
}