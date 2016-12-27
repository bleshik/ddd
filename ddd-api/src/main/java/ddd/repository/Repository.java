package ddd.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

/**
 * A collecion-like DAO. This interface tries to abstract away from persistence mechanisms, and mimics the plain old
 * {@link java.util.Collection}. Note, that it does not implement the {@link java.util.Collection}, because
 * there are methods, which are not really appropriate for the real world (e.g. you will not want to copy the whole
 * data from the storage to an in-memory array using {@link java.util.Collection#toArray()}).
 * Also, there is {@link java.util.Optional} instead of references, and some methods use the objects' identifiers instead
 * of objects themselves.
 * @param T type of the stored objects.
 * @param K type of the objects' identifiers.
 */
public interface Repository<T, K> {

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
     * Returns <tt>true</tt> if this repository contains the element with the specified identifier.
     * @param id identifier of the element is to be removed
     */
    boolean remove(K id);

    /**
     * Returns the number of elements in this collection.
     * @return the number of elements in this collection
     */
    long size();

    /**
     * Returns collection of entities with the given identifiers.
     * @return collection of entities with the given identifiers
     */
    default Set<T> findAllByIds(Collection<K> ids) {
        return ids.stream()
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

}
