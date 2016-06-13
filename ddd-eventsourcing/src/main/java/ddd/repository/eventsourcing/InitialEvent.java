package ddd.repository.eventsourcing;

import ddd.eventstore.Event;

public abstract class InitialEvent<T> extends Event {
  public abstract T initializedObject();
}
