package repository.eventsourcing;

import repository.PersistenceOrientedRepository;

public interface TemporalPersistenceOrientedRepository<T, K> extends PersistenceOrientedRepository<T, K> {
  boolean contained(K id);
  default boolean removed(K id) { return !contains(id) && contained(id); }
}
