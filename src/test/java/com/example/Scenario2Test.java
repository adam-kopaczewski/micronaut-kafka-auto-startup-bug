package com.example;

import io.micronaut.configuration.kafka.annotation.*;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
@Property(name = "scenario", value = "2")
// 1. Auto startup disabled
// 2. ConsumerStateSingle consumer
// 3. Error strategy + error in first poll
public class Scenario2Test {
    @Inject
    Listener listener;

    @Test
    void onErrorInFirstBatchAutoStartupIsIgnored() {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertTrue(listener.values.size() > 10));
    }

    @Requires(property = "scenario", value = "2")
    @KafkaListener(
            groupId = Listener.TOPIC,
            offsetReset = OffsetReset.EARLIEST,
            pollTimeout = "2s",
            properties = {
                    @Property(name = "max.poll.records", value = "5")
            },
            errorStrategy = @ErrorStrategy(value = ErrorStrategyValue.RETRY_ON_ERROR, retryCount = 2, retryDelay = "0.1s"),
            autoStartup = false)
    static class Listener {
        static final Logger log = LoggerFactory.getLogger(Listener.class);
        static final String TOPIC = "scenario2";
        private boolean shouldThrow = true;

        Set<Integer> values = new HashSet<>();

        @Topic(Listener.TOPIC)
        public void receive(Integer value) {
            log.info("Received message with value: {}", value);
            if (shouldThrow) {
                shouldThrow = false;
                throw new RuntimeException("Kaboom!");
            }
            values.add(value);
        }
    }
}
