package ddd.repository;

import java.util.Optional;
import java.util.Set;

/**
 * "save" based Repository. This repository does not automatically detect changes, so there is the corresponding "save"
 * method. NoSQL databases are often a good fit for such repositories, where they simply replace the object with a new
 * vesion without the need of tracking the changes.
 * @param T type of the stored objects.
 * @param K type of the objects' identifiers.
 */
public interface PersistenceOrientedRepository<T, K> extends Repository<T, K> {
    /**
     * Adds the element to the repository. It does nothing, if the element already presents in the repository.
     * Note, that the returned object may differ from the given one, because the internal implementation may
     * add or modify any info of the object. 
     * @param entity element to be added or replaced
     * @return the saved element
     */
    T save(T entity);
}
