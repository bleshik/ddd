package ddd.repository;

/**
 * Temporal Repository. This repository contains temporal methods, i.e. operations based on time.
 */
public interface TemporalRepository<T extends IdentifiedEntity<K>, K> extends PersistenceOrientedRepository<T, K> {

    /**
     * Returns <tt>true</tt> if this repository has ever contained the element with the specified identifier.
     * @param id identifier of the element whose presence in this collection is to be tested
     */
    boolean contained(K id);

    /**
     * Returns <tt>true</tt> if this repository has contained the element with the specified identifier, but does not
     * containt it anymore.
     * @param id identifier of the element whose presence in this collection is to be tested
     */
    default boolean removed(K id) { return !contains(id) && contained(id); }

}
