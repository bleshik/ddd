package eventstore.util.mongodb;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import eventstore.EventStoreException;
import eventstore.PayloadEvent;
import eventstore.util.json.GsonJsonSerde;
import eventstore.util.json.JsonDbObjectMapper;
import java.lang.reflect.Field;

/**
 * {@link Gson} based implementation of the mapper for MongoDB.
 */
public class GsonMongoDbObjectMapper extends JsonDbObjectMapper<DBObject> {

    public GsonMongoDbObjectMapper() {
        this(new Gson());
    }

    public GsonMongoDbObjectMapper(Gson gson) {
        super(new GsonJsonSerde(gson), (dbObject) -> JSON.serialize(dbObject), (json) -> (DBObject) JSON.parse(json));
    }

}
