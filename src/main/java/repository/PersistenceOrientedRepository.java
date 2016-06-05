package repository;

import java.util.Optional;
import java.util.Set;

public interface PersistenceOrientedRepository<T, K> {
  Optional<T> get(K id);
  default boolean contains(K id) { return get(id).isPresent(); }
  T save(T entity);
  boolean remove(K id);
  long size();
  Set<T> all();
}
