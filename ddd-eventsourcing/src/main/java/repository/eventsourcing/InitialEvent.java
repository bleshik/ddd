package repository.eventsourcing;

import eventstore.Event;

public abstract class InitialEvent<T> extends Event {
  public abstract T initializedObject();
}
