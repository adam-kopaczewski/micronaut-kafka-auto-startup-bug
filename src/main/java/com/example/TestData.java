package com.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

public class TestData {
    public static void main(String[] args) {
        var log = LoggerFactory.getLogger(TestData.class);
        var topics = List.of("scenario1", "scenario2", "scenario3", "scenario4", "scenario5", "scenario6");
        var cfg = new Properties();
        cfg.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
        cfg.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
        try (var producer = new KafkaProducer<Integer, Integer>(cfg)) {
            for (var topic : topics) {
                var recordsMetadata = IntStream.range(0, 100)
                        .mapToObj(i -> producer.send(new ProducerRecord<>(topic, i)))
                        .toList();
                for (var recordMetadata : recordsMetadata) {
                    recordMetadata.get(10, TimeUnit.SECONDS);
                }
                log.info("100 messages were sent to topic: {}", topic);
            }
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
