package ddd.repository.eventsourcing;

import eventstore.Event;

/**
 * Abstract event representing the very first event of an entity, i.e. event constructing the entity.
 */
public abstract class InitialEvent<T> extends Event {
    public abstract T initializedObject();
}
