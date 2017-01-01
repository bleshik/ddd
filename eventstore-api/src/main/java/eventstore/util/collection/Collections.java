package eventstore.util.collection;

import java.util.*;
import java.util.stream.*;

import static java.util.stream.Collectors.toList;

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

    public static <T> Stream<List<T>> batched(Iterable<T> iterable, int size) {
        return iterable instanceof List ?
                batched((List<T>) iterable, size) :
                batched(stream(iterable).collect(toList()), size);
    }

    public static <T> Stream<List<T>> batched(List<T> list, int size) {
        return IntStream.range(0, (list.size()+size-1)/size)
            .mapToObj(i -> list.subList(i*size, Math.min(list.size(), (i+1)*size)));
    }

}
