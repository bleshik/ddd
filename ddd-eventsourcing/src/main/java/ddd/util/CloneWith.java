package ddd.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

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
