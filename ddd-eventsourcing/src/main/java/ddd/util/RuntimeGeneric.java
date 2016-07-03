package ddd.util;

import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;

/**
 * Interface with utility methods handling generic classes.
 */
public interface RuntimeGeneric {
    default Class getClassArgument(int i) {
        Type type = getTypeArgument(i);
        if (type instanceof ParameterizedType) {
            return (Class) ((ParameterizedType) type).getRawType();
        }
        return (Class) type;
    }
    default Type getTypeArgument(int i) {
        return ((ParameterizedType) this.getClass()
                .getGenericSuperclass())
                .getActualTypeArguments()[i];
    }
}
