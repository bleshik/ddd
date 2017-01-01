package ddd.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;

/**
 * "save" based Repository. This repository does not automatically detect changes, so there is the corresponding "save"
 * method. NoSQL databases are often a good fit for such repositories, where they simply replace the object with a new
 * vesion without the need of tracking the changes.
 * @param T type of the stored objects.
 * @param K type of the objects' identifiers.
 */
public interface PersistenceOrientedRepository<T extends IdentifiedEntity<K>, K> extends Repository<T> {
    /**
     * Adds the element to the repository, or replaces, if the element already presents in the repository.
     * Note, that the returned object may differ from the given one, because the internal implementation may
     * add or modify any info of the object. 
     * @param entity element to be added or replaced
     * @return the saved element
     */
    T save(T entity);

    /**
     * Adds the elements to the repository, or replaces, if some elements already presents in the repository.
     * @param entities elements to be added or replaced
     */
    default void saveAll(Iterable<T> entities) { entities.forEach(this::save); }

    /**
     * Adds the element to the repository, or throws {@link AlreadyExistsException}, if the element already presents
     * in the repository.
     * Note, that the returned object may differ from the given one, because the internal implementation may
     * add or modify any info of the object.
     * @param entity element to be added
     * @return the created element
     * @throws AlreadyExistsException if there is an entity with same id in the repository
     */
    default T add(T entity) {
        if (contains(entity.getId())) {
            throw new AlreadyExistsException();
        }
        return save(entity);
    }

    /**
     * Returns an {@link Optional} describing the element corresponding to the specified identifier of the repository,
     * or an empty {@code Optional} if the element does not present.
     * @param id identifier of the element which whould be retrieved
     * @return an {@link Optional} describing the element corresponding to the specified identifier of the repository,
     * or an empty {@code Optional} if the element does not present
     */
    Optional<T> get(K id);

    /**
     * Returns <tt>true</tt> if this repository contains the element with the specified identifier.
     * @param id identifier of the element whose presence in this collection is to be tested
     */
    default boolean contains(K id) { return get(id).isPresent(); }

    /**
     * Deletes the element with the given identifier.
     * @param id identifier of the element is to be removed
     * @return true if the repository contained the element
     */
    boolean remove(K id);

    /**
     * Returns <tt>true</tt> if this repository contains the element with the specified identifier.
     * @param ids identifiers of the elements are to be removed
     */
    default void removeAll(Iterable<K> ids) {
        ids.forEach(this::remove);
    }

    /**
     * Returns collection of entities with the given identifiers.
     * @return collection of entities with the given identifiers
     */
    default Collection<T> findAllByIds(Iterable<K> ids) {
        return stream(ids.spliterator(), false)
            .flatMap(id -> get(id).map(Stream::of).orElse(Stream.empty()))
            .collect(toSet());
    }

    /**
     * Returns an element corresponding to the specified identifier of the repository,
     * or {@link NotFoundException} if the element does not present.
     * @param id identifier of the element which whould be retrieved
     * @return an element corresponding to the specified identifier of the repository
     * @throws NotFoundException if the element does not present.
     */
    default T getOrFail(K id) throws NotFoundException {
        return get(id).orElseGet(() -> { throw new IllegalArgumentException("NOT_FOUND"); });
    }

    /**
     * Flushes all changes to the underlying store, if any.
     */
    void flush();
}
