package util;

import java.lang.reflect.ParameterizedType;

public interface RuntimeGeneric {
    default Class getTypeArgument(int i) {
        return (Class) ((ParameterizedType) this.getClass()
                .getGenericSuperclass())
                .getActualTypeArguments()[i];
    }
}
