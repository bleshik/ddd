package eventstore.util.json;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import eventstore.EventStoreException;
import eventstore.PayloadEvent;
import java.lang.reflect.Field;

/**
 * {@link Gson} based implementation of the serde interface.
 */
public class GsonJsonSerde implements JsonSerde {

    public final Gson gson;
    private final JsonParser parser = new JsonParser();
    private final Field payloadField;

    public GsonJsonSerde() {
        this(new Gson());
    }

    public GsonJsonSerde(Gson gson) {
        this.gson = gson;
        try {
            payloadField = PayloadEvent.class.getDeclaredField("payload");
            payloadField.setAccessible(true);
        } catch (NoSuchFieldException ex) {
            throw new AssertionError("This shouldn't happen");
        }
    }

    @Override
    public Object deserialize(String json) {
        JsonElement element = parser.parse(json);
        if (element instanceof JsonNull) {
            return null;
        }
        JsonObject data = (JsonObject) parser.parse(json);
        try {
            Object obj = gson.fromJson(data, Class.forName(data.get("type").getAsString()));
            if (obj instanceof PayloadEvent) {
                payloadField.set(obj, gson.fromJson(data.get("payload"), Class.forName(data.get("payloadType").getAsString())));
            }
            return obj;
        } catch (ClassNotFoundException|IllegalAccessException e) {
            throw new EventStoreException(String.format("Failed to deserialize %s", json), e);
        }
    }

    @Override
    public String serialize(Object obj) {
        JsonObject dataElement = gson.toJsonTree(obj).getAsJsonObject();
        dataElement.add("type", new JsonPrimitive(obj.getClass().getCanonicalName()));
        if (obj instanceof PayloadEvent) {
            dataElement.add("payloadType",new JsonPrimitive(((PayloadEvent) obj).payload.getClass().getCanonicalName()));
        }
        return gson.toJson(dataElement);
    }
}
