package com.example;

import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
@Property(name = "scenario", value = "4")
// 1. Auto startup disabled
// 2. ConsumerStateBatch consumer
// 3. No error strategy
public class Scenario4Test {
    @Inject
    Listener listener;

    @Test
    void firstBatchConsumedDespiteOfTurnedOffAutoStartup() {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertEquals(3, listener.values.size()));
    }

    @Requires(property = "scenario", value = "4")
    @KafkaListener(
            groupId = Listener.TOPIC,
            offsetReset = OffsetReset.EARLIEST,
            pollTimeout = "2s",
            properties = {
                    @Property(name = "max.poll.records", value = "3")
            },
            autoStartup = false,
            batch = true)
    static class Listener {
        static final Logger log = LoggerFactory.getLogger(Listener.class);
        static final String TOPIC = "scenario4";

        Set<Integer> values = new HashSet<>();

        @Topic(Listener.TOPIC)
        public void receive(List<Integer> values) {
            log.info("Received message with values: {}", values);
            this.values.addAll(values);
        }
    }
}
