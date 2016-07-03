package ddd.eventstore.kafka;

import java.lang.Math;
import java.util.Map;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;

public class KafkaEventStorePartitioner implements Partitioner {

    private int partitions;

    @Override
    public void close() {}

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        return (Math.abs(key.hashCode()) % partitions);
    }
    
    @Override
    public void configure(Map<String, ?> configs) {
        partitions = (Integer) configs.get("num.partitions");
    }
}
