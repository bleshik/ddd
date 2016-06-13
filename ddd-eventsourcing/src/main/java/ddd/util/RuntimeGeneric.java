package ddd.util;

import java.lang.reflect.ParameterizedType;

/**
 * Interface with utility methods handling generic classes.
 */
public interface RuntimeGeneric {
    default Class getTypeArgument(int i) {
        return (Class) ((ParameterizedType) this.getClass()
                .getGenericSuperclass())
                .getActualTypeArguments()[i];
    }
}
