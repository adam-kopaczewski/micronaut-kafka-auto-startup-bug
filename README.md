6 scenarios in which Micronaut Kafka listeners consume one batch of messages (the result of first poll) or ignores `autoStartup=false` and consume messages.

# Steps to reproduce

## Boot Kafka broker

```shell
docker-compose up -d
```

## Prepare test data
```shell
./gradlew sendTestMessages --rerun
```

It's crucial to produce test data earlier. I tried to produce test data in a unit test,
but somtimes the bug was reproduced and sometimes wasn't. If data are prepared before running tests, 
the bug is always reproduced (at least so far).

## Test scenarios

### Scenario 1

- `autoStartup=false`
- single message consumer (`batch=false`)
- no error strategy and no uncaught error during message processing

The consumer consumes the first batch of messages and then waits.
```shell
./gradlew test --tests com.example.Scenario1Test --rerun
```
Output:
```shell
12:03:23.206 INFO  c.e.Scenario1Test$Listener - Received message with value: 0
12:03:23.207 INFO  c.e.Scenario1Test$Listener - Received message with value: 1
12:03:23.207 INFO  c.e.Scenario1Test$Listener - Received message with value: 2
```

### Scenario 2

- `autoStartup=false`
- single message consumer (`batch=false`)
- retry error strategy and uncaught error during first message processing

The consumer consumes the first batch of messages, an error occurs, then successful retry happens and finally the consumer consumes further messages (until it reaches its topic head).

```shell
./gradlew test --tests com.example.Scenario2Test --rerun
```

Output:
```shell
12:04:21.954 INFO  c.e.Scenario2Test$Listener - Received message with value: 0
12:04:23.971 INFO  c.e.Scenario2Test$Listener - Received message with value: 0
12:04:23.971 INFO  c.e.Scenario2Test$Listener - Received message with value: 1
...
12:04:23.979 INFO  c.e.Scenario2Test$Listener - Received message with value: 98
12:04:23.980 INFO  c.e.Scenario2Test$Listener - Received message with value: 99
```

### Scenario 3

- `autoStartup=false`
- single message consumer (`batch=false`)
- retry error strategy and no uncaught error during message processing

The consumer consumes the first batch of messages and then waits.

```shell
./gradlew test --tests com.example.Scenario3Test --rerun
```
Output:
```shell
12:06:00.144 INFO  c.e.Scenario3Test$Listener - Received message with value: 0
12:06:00.145 INFO  c.e.Scenario3Test$Listener - Received message with value: 1
12:06:00.145 INFO  c.e.Scenario3Test$Listener - Received message with value: 2
12:06:00.146 INFO  c.e.Scenario3Test$Listener - Received message with value: 3
12:06:00.146 INFO  c.e.Scenario3Test$Listener - Received message with value: 4
```

### Scenario 4

- `autoStartup=false`
- batch consumer (`batch=true`)
- no error strategy and no uncaught error during message processing

The consumer consumes the first batch of messages and then waits.

```shell
./gradlew test --tests com.example.Scenario4Test --rerun
```
Output:
```shell
12:06:43.518 INFO  c.e.Scenario4Test$Listener - Received message with values: [0, 1, 2]
```

### Scenario 5

- `autoStartup=false`
- batch consumer (`batch=true`)
- retry error strategy and uncaught error during first message processing

The consumer consumes the first batch of messages, an error occurs and then [the bug](https://github.com/micronaut-projects/micronaut-kafka/issues/1004) happens and consumer fails. According to my code analysis, if there was no bug, there would have been a retry and then further consumption of the message.
```shell
./gradlew test --tests com.example.Scenario5Test --rerun
```
Output:
```shell
12:07:16.978 INFO  c.e.Scenario5Test$Listener - Received message with values: [0, 1, 2]
12:07:16.980 ERROR i.m.c.k.e.KafkaListenerExceptionHandler - Kafka consumer [com.example.Scenario5Test$Listener@52dfcbfa] produced error: Cannot invoke "org.apache.kafka.clients.consumer.OffsetAndMetadata.offset()" because the return value of "java.util.Map.get(Object)" is null
java.lang.NullPointerException: Cannot invoke "org.apache.kafka.clients.consumer.OffsetAndMetadata.offset()" because the return value of "java.util.Map.get(Object)" is null
    at io.micronaut.configuration.kafka.processor.ConsumerStateBatch.lambda$getCurrentRetryCount$3(ConsumerStateBatch.java:173)
    at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
    at java.base/java.util.HashMap$KeySpliterator.forEachRemaining(HashMap.java:1715)
    at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
    at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
    at java.base/java.util.stream.ReduceOps$ReduceOp.evaluateSequential(ReduceOps.java:921)
    at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234)
    at java.base/java.util.stream.IntPipeline.reduce(IntPipeline.java:520)
    at java.base/java.util.stream.IntPipeline.max(IntPipeline.java:483)
    at io.micronaut.configuration.kafka.processor.ConsumerStateBatch.getCurrentRetryCount(ConsumerStateBatch.java:175)
    at io.micronaut.configuration.kafka.processor.ConsumerStateBatch.resolveWithErrorStrategy(ConsumerStateBatch.java:149)
    at io.micronaut.configuration.kafka.processor.ConsumerStateBatch.processRecords(ConsumerStateBatch.java:93)
    at io.micronaut.configuration.kafka.processor.ConsumerState.pollAndProcessRecords(ConsumerState.java:212)
    at io.micronaut.configuration.kafka.processor.ConsumerState.refreshAssignmentsPollAndProcessRecords(ConsumerState.java:164)
    at io.micronaut.configuration.kafka.processor.ConsumerState.threadPollLoop(ConsumerState.java:154)
    at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:572)
    at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:317)
    at java.base/java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:304)
    at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)
    at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)
    at java.base/java.lang.Thread.run(Thread.java:1583)
```

### Scenario 6

- `autoStartup=false`
- batch consumer (`batch=true`)
- retry error strategy and no uncaught error during message processing

The consumer consumes the first batch of messages and then waits.
```shell
./gradlew test --tests com.example.Scenario6Test --rerun
```
Output:
```shell
12:07:48.927 INFO  c.e.Scenario6Test$Listener - Received message with values: [0, 1, 2]
```
## Clean up Kafka broker
```shell
docker-compose rm -sf
```