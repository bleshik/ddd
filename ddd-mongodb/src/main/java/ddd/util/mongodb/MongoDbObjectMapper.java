package ddd.util.mongodb;

import com.mongodb.DBObject;

/**
 * Mapper converting from {@link DBObject} to a POJO, and backwards.
 */
public interface MongoDbObjectMapper {
    /**
     * Maps from {@link DBObject} to a POJO of the given class.
     * @param T type of the object to convert to
     * @param mongoObject a mongo object is to be converted to the given type
     */
    <T> T mapToObject(DBObject mongoObject, Class<T> c);

    /**
     * Maps to {@link DBObject} from a POJO.
     * @param T type of the object to convert from
     * @param obj an object is to be converted to {@link DBObject}
     */
    <T> DBObject mapToDbObject(T obj);

}
