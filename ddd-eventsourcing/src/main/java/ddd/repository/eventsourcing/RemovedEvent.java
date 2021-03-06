package ddd.repository.eventsourcing;

import eventstore.Event;

/**
 * Removing event. This event means, that the object no longer exists.
 */
class RemovedEvent<K> extends Event {

    private K id;

    public RemovedEvent(K id) { 
        this.id = id;
    }

    private RemovedEvent() {}

    public K getId() { return id; }
}
