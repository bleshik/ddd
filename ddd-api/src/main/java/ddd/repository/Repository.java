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
 */
public interface Repository<T> {

    /**
     * Returns the number of elements in this collection.
     * @return the number of elements in this collection
     */
    long size();

}
