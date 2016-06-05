package repository.eventsourcing;

import repository.IdentifiedEntity;

public abstract class IdentifiedEventSourcedEntity<T extends EventSourcedEntity<T>, K> extends EventSourcedEntity<T>
  implements IdentifiedEntity<K> {

  private K id;

  public IdentifiedEventSourcedEntity(K id, InitialEvent<T> initialEvent) {
    super(initialEvent);
    this.id = id;
  }

  @Override
  public K getId() {
    return id;
  }

  @Override
  public int hashCode() {
    return getId().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj.getClass() != getClass()) {
      return false;
    }
    return this.getId().equals(((IdentifiedEntity) obj).getId());
  }

}
