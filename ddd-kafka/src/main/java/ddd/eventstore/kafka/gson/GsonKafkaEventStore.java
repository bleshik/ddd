package ddd.eventstore.kafka.gson;

import ddd.eventstore.Event;
import ddd.eventstore.kafka.KafkaEventStore;
import ddd.eventstore.kafka.KafkaEventStoreOptimisticLockingStrategy;
import java.util.Map;

public class GsonKafkaEventStore extends KafkaEventStore {
    public GsonKafkaEventStore(String name,
            String applicationId,
            String kafkaNodes,
            String zookeeperNodes,
            KafkaEventStoreOptimisticLockingStrategy strategy) {
        super(name, applicationId, kafkaNodes, zookeeperNodes, new GsonSerde<Event>(){{}}, strategy);
    }

    public GsonKafkaEventStore(String name,
            String kafkaNodes,
            String zookeeperNodes,
            KafkaEventStoreOptimisticLockingStrategy strategy) {
        this(name, name, kafkaNodes, zookeeperNodes, strategy);
    }
}
