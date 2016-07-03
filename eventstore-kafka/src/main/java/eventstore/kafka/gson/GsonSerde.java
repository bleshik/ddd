package eventstore.kafka.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import eventstore.util.RuntimeGeneric;
import java.lang.ClassNotFoundException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

@SuppressWarnings("unchecked")
public class GsonSerde<T> implements Serde<T>, RuntimeGeneric {

    private final Type typeOfT = getTypeArgument(0);
    private final Gson gson = new GsonBuilder().registerTypeHierarchyAdapter(getClassArgument(0), new TypingGsonSerde<T>()).create();
    private final JsonParser parser = new JsonParser();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {}

    @Override
    public void close() {}

    @Override
    public Serializer<T> serializer() {
        return new Serializer<T>() {

            @Override
            public void configure(Map<String, ?> configs, boolean isKey) {}

            @Override
            public byte[] serialize(String topic, T data) {
                return gson.toJson(data).getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public void close() {}

        };
    }

    @Override
    public Deserializer<T> deserializer() {
        return new Deserializer<T>() {

            @Override
            public void configure(Map<String, ?> configs, boolean isKey) {}

            public T deserialize(String topic, byte[] data) {
                if (data == null || data.length == 0) {
                    return null;
                }
                return (T) gson.fromJson(new String(data, StandardCharsets.UTF_8), typeOfT);
            }

            @Override
            public void close() {}

        };
    }

}
class TypingGsonSerde<T> implements JsonSerializer<T>, JsonDeserializer<T> {
    private final Gson gson = new Gson();

    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
    throws JsonParseException {
        JsonObject obj = (JsonObject) json;
        JsonObject data = obj.get("wrapper") != null ? obj.getAsJsonObject("data") : obj;
        try {
            return gson.fromJson(data, (Class<T>) Class.forName(obj.get("type").getAsString()));
        } catch (ClassNotFoundException e) {
            throw new JsonParseException(e);
        }
    }

    @Override
    public JsonElement serialize(T data, Type typeOfSrc, JsonSerializationContext serContext) {
        String type = data.getClass().getCanonicalName();
        JsonElement dataElement = gson.toJsonTree(data);
        if (dataElement instanceof JsonObject) {
            ((JsonObject) dataElement).add("type", new JsonPrimitive(type));
            return dataElement;
        } else {
            JsonObject wrapper =  new JsonObject();
            wrapper.add("wrapper", new JsonPrimitive(true));
            wrapper.add("data", dataElement);
            wrapper.add("type", new JsonPrimitive(type));
            return wrapper;
        }
    }

}
