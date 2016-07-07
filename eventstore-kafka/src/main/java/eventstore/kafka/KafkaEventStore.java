package eventstore.kafka;

import eventstore.Event;
import eventstore.EventStore;
import eventstore.EventStoreException;
import eventstore.util.collection.Collections;
import java.lang.InterruptedException;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KStreamBuilder;

import static java.util.Collections.singleton;
import static java.util.Collections.emptyList;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.PARTITIONER_CLASS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.*;

/**
 * Kafka-based event store. It has several implementation strategies of optimistic locking.
 * @see eventstore.kafka.KafkaEventStoreOptimisticLockingStrategy
 */
public class KafkaEventStore implements EventStore {
    public static final int TIMEOUT = 300;
    public static final int CONSUMERS_AMOUNT = 10;
    private final KafkaProducer<String, Event> producer;
    private final BlockingQueue<KafkaConsumer<String, Event>> consumers;
    private final Collection<KafkaConsumer> allConsumers;
    private final KafkaConsumer<String, Long> sizeConsumer;
    private final KafkaStreams streams;
    private final String name;
    private final String applicationId;
    private final String kafkaNodes;
    private final String zookeeperNodes;
    private final String sinkTopic;
    private final String sizeTopic;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicLong size = new AtomicLong();
    private final Partitioner partitioner = new KafkaEventStorePartitioner();
    private final Optional<CuratorFramework> client;
    private final KafkaEventStoreOptimisticLockingStrategy strategy;
    private final ConcurrentMap<String, InterProcessSemaphoreMutex> locks = new ConcurrentHashMap<>();
    private final ConcurrentMap<InterProcessSemaphoreMutex, Thread> acquiredLocks = new ConcurrentHashMap<>();

    /**
     * Creates the Kafka-based event store.
     * @param name name of the event store, which is used for separation from other event stores, also it will be name
     *             of the kafka topic with the appended events
     * @param applicationId id of your application, which is used as Kafka's "application.id" property
     * @param kafkaNodes comma-separated list of kafka nodes, used as Kafka's "bootstrap.servers" property
     * @param zookeeperNodes comma-separated list of zookeeper nodes, used as Kafka's "zookeeper.connect" property
     * @param eventSerde serde for events
     * @param strategy optimistic locking strategy
     */
    public KafkaEventStore(String name,
            String applicationId,
            String kafkaNodes,
            String zookeeperNodes,
            Serde<Event> eventSerde,
            KafkaEventStoreOptimisticLockingStrategy strategy) {
        this.name           = name;
        this.applicationId  = applicationId;
        this.kafkaNodes     = kafkaNodes;
        this.zookeeperNodes = zookeeperNodes;
        this.sinkTopic      = name + "Sink";
        this.sizeTopic      = name + "Size";
        this.strategy       = strategy;
        // "pool" of event consumers
        this.consumers      = new LinkedBlockingQueue<KafkaConsumer<String, Event>>(configureConsumers(eventSerde.deserializer()));
        // all consumers used in the event store, used for closing them
        this.allConsumers   = new ArrayList<KafkaConsumer>(this.consumers);
        this.allConsumers.add(sizeConsumer = configureSizeConsumer());
        if (strategy != KafkaEventStoreOptimisticLockingStrategy.LOCKING) {
            this.allConsumers.add(configureRedirectingConsumer(eventSerde.deserializer()));
        }
        this.producer = configureProducer(eventSerde.serializer());
        this.streams = configureStreams(eventSerde);
        this.client = (strategy == KafkaEventStoreOptimisticLockingStrategy.SINK ?
            Optional.<String>empty() :
            Optional.of(zookeeperNodes)
        ).map((zookeeper) -> CuratorFrameworkFactory.newClient(zookeeper, new ExponentialBackoffRetry(1000, 3)));
        this.client.ifPresent((cl) -> cl.start());
    }

    /**
     * Creates the Kafka-based event store. This one uses same value for the store's name and "application.id" property.
     * @param name name of the event store, which is used for separation from other event stores, also it will be name
     *             of the kafka topic with the appended events, and it will be value Kafka's "application.id" property
     * @param kafkaNodes comma-separated list of kafka nodes, used as Kafka's "bootstrap.servers" property
     * @param zookeeperNodes comma-separated list of zookeeper nodes, used as Kafka's "zookeeper.connect" property
     * @param eventSerde serde for events
     * @param strategy optimistic locking strategy
     */
    public KafkaEventStore(String name,
            String kafkaNodes,
            String zookeeperNodes,
            Serde<Event> eventSerde,
            KafkaEventStoreOptimisticLockingStrategy strategy) {
        this(name, name, kafkaNodes, zookeeperNodes, eventSerde, strategy);
    }

    /**
     * Creates and starts a thread with a redirecting not-failed-with-optimistic-locking-events consumer.
     */
    private KafkaConsumer<String, Event> configureRedirectingConsumer(Deserializer<Event> eventDeserializer) {
        KafkaConsumer<String, Event> consumer = new KafkaConsumer<String, Event>(
                getConsumerConfig(name + "RedirectingConsumer"),
                new StringDeserializer(),
                eventDeserializer
        );
        consume(consumer, (record) -> {
            if (record.offset() == record.value().getStreamVersion()) {
                producer.send(new ProducerRecord<>(name, record.key(), record.value()));
            }
        });
        return consumer;
    }

    /**
     * Creates events consumers, which are used for reading the appeneded events. We need several, because consumers are
     * not thread safe.
     */
    private Collection<KafkaConsumer<String, Event>> configureConsumers(Deserializer<Event> eventDeserializer) {
        String uid = UUID.randomUUID().toString();
        return IntStream.rangeClosed(1, CONSUMERS_AMOUNT).boxed().map((id) ->
            new KafkaConsumer<String, Event>(
                getConsumerConfig(name + uid + "Consumer" + id),
                new StringDeserializer(),
                eventDeserializer
            )
        ).collect(Collectors.toList());
    }

    /**
     * Creates an event producer, which is used for the appending operations. We need only a single one, because
     * producers are thread safe.
     */
    private KafkaProducer<String, Event> configureProducer(Serializer<Event> eventSerializer) {
        Map<String, Object> producerConfig = new HashMap<String, Object>();
        producerConfig.put(BOOTSTRAP_SERVERS_CONFIG, kafkaNodes);
        // Set some defaults
        if (!producerConfig.containsKey("num.partitions")) {
            withConsumer((consumer) ->
                producerConfig.put("num.partitions", consumer.partitionsFor(mainTopic()).size())
            );
        }
        // we use stream name as the partitioning key, so we tell the producer to use an our partitioner, so that we
        // know exactly for a given stream where corresponding events are stored
        producerConfig.put(PARTITIONER_CLASS_CONFIG, KafkaEventStorePartitioner.class);
        partitioner.configure(producerConfig);
        return new KafkaProducer<String, Event>(producerConfig, new StringSerializer(), eventSerializer);
    }

    /**
     * We use the Kafka Streams to calculate the size of the store. This will produce a topic with the size changes.
     */
    private KafkaStreams configureStreams(Serde<Event> eventSerde) {
        Properties streamsProperties = new Properties() {{
            put(ZOOKEEPER_CONNECT_CONFIG, zookeeperNodes);
            put(BOOTSTRAP_SERVERS_CONFIG, kafkaNodes);
            put(AUTO_OFFSET_RESET_CONFIG, "earliest");
            put(APPLICATION_ID_CONFIG, applicationId + "Streams");
        }};

        Serdes.StringSerde stringSerde = new Serdes.StringSerde();
        Serdes.LongSerde longSerde = new Serdes.LongSerde();
        KafkaStreams streams = new KafkaStreams(
            new KStreamBuilder() {{
                stream(stringSerde, eventSerde, mainTopic())
                    .map((key, value) -> new KeyValue<String, String>(key, key))
                    .reduceByKey(
                        (v0, v1) -> v0,
                        stringSerde,
                        stringSerde,
                        name + "Names"
                    )
                    .groupBy((key, value) -> new KeyValue<String, String>("size", key), stringSerde, stringSerde)
                    .count("size")
                    .to(stringSerde, longSerde, sizeTopic);
            }},
            streamsProperties
        );
        streams.start();
        return streams;
    }

    /**
     * Creates and starts a consumer, reading the most actual size of the event store, which's used for {@link #size()}.
     */
    private KafkaConsumer<String, Long> configureSizeConsumer() {
        KafkaConsumer<String, Long> consumer = new KafkaConsumer<String, Long>(
                // the consumer should be per-event-store, so use a random uid here
                getConsumerConfig(name + "SizeConsumer" + UUID.randomUUID()),
                new StringDeserializer(),
                new LongDeserializer()
        );
        // go right to the last event right away, because there is no point of reading old values
        List<TopicPartition> partitions = consumer.partitionsFor(sizeTopic).stream().map((p) ->
                new TopicPartition(sizeTopic, p.partition())
        ).collect(Collectors.toList());
        consumer.assign(partitions);
        for (TopicPartition topicPartition : partitions) {
            consumer.seekToEnd(Arrays.asList(topicPartition));
            long currentPosition = consumer.position(topicPartition);
            if (currentPosition == 0) {
                consumer.seekToBeginning(Arrays.asList(topicPartition));
            } else {
                consumer.seek(topicPartition, currentPosition - 1);
            }
        }
        consume(consumer, (record) -> size.set(record.value()));
        return consumer;
    }


    @Override
    public void append(String streamName, long currentVersion, List<? extends Event> events) {
        for (int i = 0, n = events.size(); i < n; ++i) {
            append(streamName, currentVersion + i, events.get(i));
        }
    }

    @Override
    public void append(String streamName, long currentVersion, Event event) {
        String lockName = "EventStore" + name + partitionNumber(streamName);
        // use locks only if the client is configured
        Optional<InterProcessSemaphoreMutex> lock = client.map((c) -> {
            // reuse the locks
            if (!locks.containsKey(lockName)) {
                // do not use reentrant locks, so that only one thread within a process is able to hold it
                locks.putIfAbsent(lockName, new InterProcessSemaphoreMutex(c, "/" + lockName));
            }
            return locks.get(lockName);
        });
        try {
            Event occurredEvent = event.occurred(currentVersion + 1);
            try {
                // if not SINK, we use locking, so we need to check the version before apending the event
                if (strategy != KafkaEventStoreOptimisticLockingStrategy.SINK) {
                    boolean acquired = false;
                    Exception cause = null;
                    try {
                        acquired = lock.get().acquire(1000, TimeUnit.MILLISECONDS);
                        if (acquired) {
                            acquiredLocks.put(lock.get(), Thread.currentThread());
                        }
                    } catch (Exception e) {
                        cause = e;
                    }
                    // if the lock is not acquired, fail in the case of LOCKING strategy.
                    // it is not critical in the case of other strategies, because we will still use the sink topic.
                    if (!acquired && strategy == KafkaEventStoreOptimisticLockingStrategy.LOCKING) {
                        throw new EventStoreException("Failed to acquire the lock " + lockName, cause);
                    }
                    long actualVersion = version(streamName);
                    // check if the caller's current version is actual
                    if (currentVersion != actualVersion) {
                        throw new ConcurrentModificationException();
                    }
                }

                // in the case of LOCKING we can safely send the event to the topic with "good" events
                if (strategy == KafkaEventStoreOptimisticLockingStrategy.LOCKING) {
                    producer.send(new ProducerRecord<>(name, streamName, occurredEvent));
                } else {
                    // otherwise, we use the "sink" topic to check the version afterwards,
                    // "good" events are redirected to the "good" topic in a separate thread
                    long actualOffset = producer.send(new ProducerRecord<>(sinkTopic, streamName, occurredEvent)).get().offset();
                    if (currentVersion != actualOffset) {
                        throw new ConcurrentModificationException();
                    }
                }
                // we should send the event before releasing the lock
                producer.flush();
            } finally {
                // release the locks
                if (lock.isPresent()) {
                    try {
                        if (acquiredLocks.containsKey(lock.get())) {
                            acquiredLocks.remove(lock.get());
                            lock.get().release();
                        }
                    } catch (Exception e) {
                        throw new EventStoreException(String.format("Failed to release the lock %s", lockName), e);
                    }
                }
            }
        } catch (InterruptedException|ExecutionException e) {
            throw new EventStoreException(String.format("Failed to append %s event to %s stream", event, streamName), e);
        }
    }

    @Override
    public void close() {
        if (closed.get()) {
            throw new EventStoreException("The event store is already closed");
        }
        closed.set(true);
        try {
            producer.close();
        } finally {
            try {
                RuntimeException lastException = null;
                for (KafkaConsumer consumer : allConsumers) {
                    try {
                        consumer.wakeup();
                    } catch (RuntimeException e) {
                        lastException = e;
                    }
                }
                for (KafkaConsumer consumer : consumers) {
                    try {
                        consumer.close();
                    } catch (RuntimeException e) {
                        lastException = e;
                    }
                }
                if (lastException != null) {
                    throw new EventStoreException("Failed to close a consumer", lastException);
                }
            } finally {
                streams.close();
            }
        }
    }

    @Override
    public long version(String streamName) {
        return partition(streamName).map((topicPartition) -> 
            withConsumer((consumer) -> {
                consumer.assign(Arrays.asList(topicPartition));
                consumer.seekToEnd(Arrays.asList(topicPartition));
                return consumer.position(topicPartition);
            })
        ).orElse(0L);
    }

    private Optional<TopicPartition> partition(String streamName) {
        // if the strategy is LOCKING, it uses only one topic
        String topic = mainTopic();
        // the partitioner uses nothing, but the key
        int partition = partitionNumber(streamName);
        return withConsumer((consumer) -> consumer.partitionsFor(topic))
                .stream()
                .filter((p) -> p.partition() == partition)
                .findAny()
                .map((p) -> new TopicPartition(topic, partition));
    }

    private String mainTopic() {
        return strategy == KafkaEventStoreOptimisticLockingStrategy.LOCKING ? name : sinkTopic;
    }

    private int partitionNumber(String streamName) {
        return partitioner.partition(null, streamName, null, null, null, null);
    }

    @Override
    public Optional<Stream<Event>> streamSince(String streamName, long after) {
        // wrap the consuming process into an iterator
        Iterator<ConsumerRecord<String, Event>> it = new Iterator<ConsumerRecord<String, Event>>() {
            private boolean closed = false;
            private KafkaConsumer<String, Event> consumer = takeConsumer(); 
            private Iterator<ConsumerRecord<String, Event>> records;
            {
                try {
                    partition(streamName).ifPresent((topicPartition) -> {
                        consumer.assign(Arrays.asList(topicPartition));
                        consumer.seek(topicPartition, after);
                    });
                } catch (WakeupException e) {
                    if (KafkaEventStore.this.closed.get()) {
                        consumer.close();
                    } else {
                        closed = true;
                        consumers.add(consumer);
                    }
                }
            }

            @Override
            public boolean hasNext() {
                try {
                    if (closed) {
                        return false;
                    }
                    if (records != null && !records.hasNext()) {
                        consumer.commitAsync();
                    }
                    if (records == null || !records.hasNext()) {
                        records = consumer.poll(TIMEOUT).iterator();
                    }
                    if (!records.hasNext()) {
                        closed = true;
                        consumers.add(consumer);
                    }
                    return records.hasNext();
                } catch (WakeupException e) {
                    if (KafkaEventStore.this.closed.get()) {
                        consumer.close();
                        return false;
                    } else {
                        closed = true;
                        consumers.add(consumer);
                        throw e;
                    }

                } catch (Exception e) {
                    closed = true;
                    consumers.add(consumer);
                    return false;
                }
            }

            @Override
            public ConsumerRecord<String, Event> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return records.next();
            }
        };
        return it.hasNext() ? Optional.of(
                Collections.stream(it).filter((e) ->
                    e.key().equals(streamName) && e.value().getStreamVersion() - 1 == e.offset()
                ).map((e) -> e.value())
        ) : (version(streamName) > 0 ? Optional.of(Stream.empty()) : Optional.empty());
    }

    @Override
    public long size() {
        return size.get();
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * An util method for consuming messages.
     */
    private <K, V> void consume(KafkaConsumer<K, V> consumer, Consumer<ConsumerRecord<K, V>> fn) {
        new Thread() {
            @Override
            public void run() {
                try {
                    while(!closed.get()) {
                        for (ConsumerRecord<K, V> record : consumer.poll(TIMEOUT)) {
                            fn.accept(record);
                        }
                    }
                } catch (WakeupException e) {
                    if (!closed.get()) throw e;
                } finally {
                    consumer.close();
                }
            }
        }.start();
    }

    /**
     * Acquires an events consumer from the "pool". Note that if you use the method, it is your responsobility to put
     * the consumer back to the "pool".
     */
    private KafkaConsumer<String, Event> takeConsumer() {
        try {
            return consumers.take();
        } catch (InterruptedException e) {
            throw new EventStoreException("Failed to take a consumer", e);
        }
    }

    /**
     * Acquires an events consumer from the "pool", applies the function, and then puts the consumer back to the "pool".
     */
    private <T> T withConsumer(Function<KafkaConsumer<String, Event>, T> fn) {
        KafkaConsumer<String, Event> consumer = takeConsumer();
        try {
            return fn.apply(consumer);
        } finally {
            if (closed.get()) {
                consumer.close();
            } else {
                consumers.add(consumer);
            }
        }
    }

    private Map<String, Object> getConsumerConfig(String groupId) {
        return new HashMap<String, Object>() {{
            put(BOOTSTRAP_SERVERS_CONFIG, kafkaNodes);
            put(GROUP_ID_CONFIG, groupId);
        }};
    }

}
