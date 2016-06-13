package util.mongodb;

import com.mongodb.DBObject;

public interface MongoDbObjectMapper {
    <T> T mapToObject(DBObject mongoObject, Class<T> c);
    <T> DBObject mapToDbObject(T obj);
}
