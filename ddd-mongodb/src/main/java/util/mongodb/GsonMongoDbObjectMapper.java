package util.mongodb;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.google.gson.Gson;

public class GsonMongoDbObjectMapper implements MongoDbObjectMapper {

    public final Gson gson;

    public GsonMongoDbObjectMapper() {
        this(new Gson());
    }

    public GsonMongoDbObjectMapper(Gson gson) {
        this.gson = gson;
    }

    @Override
    public <T> T mapToObject(DBObject mongoObject, Class<T> c) {
        return gson.fromJson(JSON.serialize(mongoObject), c);
    }

    @Override
    public <T> DBObject mapToDbObject(T obj) {
        return (DBObject) JSON.parse(gson.toJson(obj));
    }
}
