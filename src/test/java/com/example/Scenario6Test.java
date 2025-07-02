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
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
@Property(name = "scenario", value = "6")
// 1. Auto startup disabled
// 2. ConsumerStateBatch consumer
// 3. Error strategy + no error during processing
public class Scenario6Test {
    @Inject
    Listener listener;

    @Test
    void onErrorInFirstBatchAutoStartupIsIgnored() {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertEquals(3, listener.values.size()));
    }

    @Requires(property = "scenario", value = "6")
    @KafkaListener(
            groupId = Listener.TOPIC,
            offsetReset = OffsetReset.EARLIEST,
            pollTimeout = "2s",
            properties = {
                    @Property(name = "max.poll.records", value = "3")
            },
            errorStrategy = @ErrorStrategy(value = ErrorStrategyValue.RETRY_ON_ERROR, retryCount = 2, retryDelay = "0.1s"),
            autoStartup = false,
            batch = true)
    static class Listener {
        static final Logger log = LoggerFactory.getLogger(Listener.class);
        static final String TOPIC = "scenario6";

        Set<Integer> values = new HashSet<>();

        @Topic(Listener.TOPIC)
        public void receive(List<Integer> values) {
            log.info("Received message with values: {}", values);
            this.values.addAll(values);
        }
    }
}
