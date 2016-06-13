package ddd.repository;

/**
 * An entity with identifier.
 */
public interface IdentifiedEntity<K> {
  public K getId();
}
