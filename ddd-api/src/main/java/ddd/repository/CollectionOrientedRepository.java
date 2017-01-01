package ddd.repository;

import java.util.Optional;
import java.util.Set;

/**
 * Auto-saving {@link Repository}. This repository assumes, that the underlying persistence mechanism automatically
 * detects changes and saves the object. For example, implementations of JPA do that.
 * @see Repository
 * @param T type of the stored objects.
 * @param K type of the objects' identifiers.
 */
public interface CollectionOrientedRepository<T> extends Repository<T> {
    /**
     * Adds the element to the repository. It does nothing, if the element already presents in the repository.
     * @param entity element to be added
     * @return <tt>true</tt> if this repository changed as a result of the call
     */
    boolean add(T entity);

    /**
     * Returns <tt>true</tt> if this repository contains the given element.
     * @param T entity
     */
    boolean contains(T entity);
}
