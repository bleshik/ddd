package repository.eventsourcing;

import eventstore.api.Event;

class RemovedEvent<K> extends Event {

  private K id;

  public RemovedEvent(K id) { 
		this.id = id;
  }

  private RemovedEvent() {}

  public K getId() { return id; }
}
