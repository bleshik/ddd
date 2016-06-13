package ddd.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

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
public interface CloneWith<T extends CloneWith> extends Cloneable {
    default T cloneWith(Function<T, ?> mutate) {
        T cloned = CloneWithHelper.<T>invokeClone((T) this);
        mutate.apply(cloned);
        return cloned;
    }
}

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
