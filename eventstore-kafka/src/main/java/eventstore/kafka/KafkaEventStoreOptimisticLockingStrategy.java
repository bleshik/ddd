package eventstore.kafka;

public enum KafkaEventStoreOptimisticLockingStrategy {
    /**
     * This will try to acquire locks on partitions using ZooKeeper. In the case of failure the operation will fail.
     * This will not use the additional "sink" topic.
     */
    LOCKING,
    /**
     * This will try to acquire locks on partitions using ZooKeeper. It will use the additional "sink" topic, so that in
     * the cases, when the producer is not able to acquire the lock, it will fallback to the SINK optimistic locking
     * algorithm. Thus, this will reduce the amount of conflicts, but still will send the events to 2 topics.
     */
    MIXED,
    /**
     * This will not use any locks on ZooKeeper. It will only use the additional "sink" topic. Clients send events with
     * last read version of the corresponding stream, the event store uses partitions offsets as the version. The event
     * store will simply append these events without any additional checks to the "sink" helper topic. After the append
     * it will check if the offsets of appended events. If version of an event equals to its offset, then everything is
     * fine, otherwise the optimistic lock fails. Finally, if the version is okay, the message will be send to other
     * topic, where all "good" events are stored.
     */
    SINK;
}
