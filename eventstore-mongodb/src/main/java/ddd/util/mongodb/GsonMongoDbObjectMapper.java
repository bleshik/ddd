package eventstore.util.mongodb;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import eventstore.EventStoreException;
import eventstore.PayloadEvent;
import java.lang.reflect.Field;

/**
 * {@link Gson} based implementation of the mapper.
 */
public class GsonMongoDbObjectMapper implements MongoDbObjectMapper {

    public final Gson gson;
    private final JsonParser parser = new JsonParser();
    private final Field payloadField;

    public GsonMongoDbObjectMapper() {
        this(new Gson());
    }

    public GsonMongoDbObjectMapper(Gson gson) {
        this.gson = gson;
        try {
            payloadField = PayloadEvent.class.getDeclaredField("payload");
            payloadField.setAccessible(true);
        } catch (NoSuchFieldException ex) {
            throw new AssertionError("This shouldn't happen");
        }
    }

    @Override
    public Object mapToObject(DBObject mongoObject) {
        JsonObject data = (JsonObject) parser.parse(JSON.serialize(mongoObject));
        try {
            Object obj = gson.fromJson(data, Class.forName(data.get("type").getAsString()));
            if (obj instanceof PayloadEvent) {
                payloadField.set(obj, gson.fromJson(data.get("payload"), Class.forName(data.get("payloadType").getAsString())));
            }
            return obj;
        } catch (ClassNotFoundException|IllegalAccessException e) {
            throw new EventStoreException(String.format("Failed to deserialize %s", mongoObject), e);
        }
    }

    @Override
    public DBObject mapToDbObject(Object obj) {
        JsonObject dataElement = gson.toJsonTree(obj).getAsJsonObject();
        dataElement.add("type", new JsonPrimitive(obj.getClass().getCanonicalName()));
        if (obj instanceof PayloadEvent) {
            dataElement.add("payloadType",new JsonPrimitive(((PayloadEvent) obj).payload.getClass().getCanonicalName()));
        }
        return (DBObject) JSON.parse(gson.toJson(dataElement));
    }
}
