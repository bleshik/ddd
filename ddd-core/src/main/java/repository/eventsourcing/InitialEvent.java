package repository.eventsourcing;

import eventstore.api.Event;

public abstract class InitialEvent<T> extends Event {
  public abstract T initializedObject();
}
