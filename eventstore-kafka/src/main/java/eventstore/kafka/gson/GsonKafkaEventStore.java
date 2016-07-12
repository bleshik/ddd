package eventstore.kafka.gson;

import eventstore.Event;
import eventstore.kafka.KafkaEventStore;
import eventstore.kafka.KafkaEventStoreOptimisticLockingStrategy;
import java.util.Map;

public class GsonKafkaEventStore extends KafkaEventStore {
    public GsonKafkaEventStore(String name,
            String applicationId,
            String kafkaNodes,
            String zookeeperNodes,
            KafkaEventStoreOptimisticLockingStrategy strategy) {
        super(name, applicationId, kafkaNodes, zookeeperNodes, new KafkaGsonSerde<Event>(){{}}, strategy);
    }

    public GsonKafkaEventStore(String name,
            String kafkaNodes,
            String zookeeperNodes,
            KafkaEventStoreOptimisticLockingStrategy strategy) {
        this(name, name, kafkaNodes, zookeeperNodes, strategy);
    }
}
