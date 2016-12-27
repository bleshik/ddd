package eventstore.util.collection;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Stream.concat;

/**
 * Utility collections related methods.
 */
public final class Collections {
    private Collections() {}

    public static <T> Stream<T> stream(Iterator<T> it) {
        Iterable<T> iterable = () -> it;
        return stream(iterable);
    }

    public static <T> Stream<T> stream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

}
