package eventstore.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * A handy interface adding a Scala-like "copy" method. For example:
 * <pre>
 * <code>
 * class House implements CloneWith<House> {
 *      protected String owner;
 *      // this returns a copy of "this" including the changes applied in the given function.
 *      House buy(String newOwner) {
 *          return this.cloneWith(h -> h.owner = newOwner);
 *      }
 * }
 * </code>
 * </pre>
 */
@SuppressWarnings("unchecked")
public interface CloneWith<T extends CloneWith> extends Cloneable {

    default T cloneWith(Consumer<T> mutate) {
        T cloned = CloneWithHelper.<T>invokeClone((T) this);
        mutate.accept(cloned);
        return cloned;
    }

    default <U> T transformFor(Iterable<U> items, BiFunction<T, U, T> mutateFor) {
        T mutated = (T) this;
        for (U item : items) {
            mutated = mutateFor.apply(mutated, item);
        }
        return mutated;
    }

    default <U> T transformFor(Optional<U> item, BiFunction<T, U, T> mutateFor) {
        return item.map(i -> mutateFor.apply((T) this, i)).orElse((T) this);
    }

}

@SuppressWarnings("unchecked")
final class CloneWithHelper {
    private CloneWithHelper() {}
    static final Method cloneMethod;
    static {
        try {
            cloneMethod = Object.class.getDeclaredMethod("clone");
            cloneMethod.setAccessible(true);
        } catch (NoSuchMethodException e) { throw new AssertionError("This shouldn't happen"); }
    } 
    static <T> T invokeClone(T obj) {
        try {
            return (T) cloneMethod.invoke(obj);
        } catch (IllegalAccessException|InvocationTargetException e) { throw new AssertionError("This shouldn't happen"); }
    }
}
