package eventstore.util.mongodb;

import com.mongodb.DBObject;

/**
 * Mapper converting from {@link DBObject} to a POJO, and backwards.
 */
public interface MongoDbObjectMapper {
    /**
     * Maps from {@link DBObject} to a POJO of the given class.
     * @param mongoObject a mongo object is to be converted to the given type
     */
    Object mapToObject(DBObject mongoObject);

    /**
     * Maps to {@link DBObject} from a POJO.
     * @param obj an object is to be converted to {@link DBObject}
     */
    DBObject mapToDbObject(Object obj);

}
