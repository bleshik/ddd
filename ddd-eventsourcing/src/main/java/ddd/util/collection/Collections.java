package ddd.util.collection;

import java.util.Iterator;
import java.lang.Iterable;
import java.util.stream.StreamSupport;
import java.util.stream.Stream;

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
