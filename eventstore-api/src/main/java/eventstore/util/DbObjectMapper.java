package eventstore.util;

/**
 * Mapper converting from a db object to a POJO, and backwards.
 * @param T db object type
 */
public interface DbObjectMapper<T> {
    /**
     * Maps from a db object to a POJO.
     * @param dbObject a db object is to be converted
     */
    Object mapToObject(T dbObject);

    /**
     * Maps to a db object from a POJO.
     * @param obj an object is to be converted to a db object
     */
    T mapToDbObject(Object obj);

}
