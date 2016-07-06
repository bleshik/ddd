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
import eventstore.PayloadEvent;
import eventstore.util.RuntimeGeneric;
import java.lang.ClassNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

@SuppressWarnings("unchecked")
public class GsonSerde<T> implements Serde<T>, RuntimeGeneric {

    private final Type typeOfT = getTypeArgument(0);
    private final Gson gson = new GsonBuilder().registerTypeHierarchyAdapter(getClassArgument(0), new TypingGsonSerde()).create();

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
                System.out.println(gson.toJson(data));
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
class TypingGsonSerde implements JsonSerializer, JsonDeserializer {
    private final Gson gson = new Gson();
    private final Field payloadField;

    public TypingGsonSerde() {
        try {
            payloadField = PayloadEvent.class.getDeclaredField("payload");
            payloadField.setAccessible(true);
        } catch (NoSuchFieldException ex) {
            throw new AssertionError("This shouldn't happen");
        }
    }

    @Override
    public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
    throws JsonParseException {
        if (json instanceof JsonObject) {
            JsonObject data = (JsonObject) json;
            try {
                Object obj = gson.fromJson(data, Class.forName(data.get("type").getAsString()));
                if (obj instanceof PayloadEvent) {
                    payloadField.set(obj, gson.fromJson(data.get("payload"), Class.forName(data.get("payloadType").getAsString())));
                }
                return obj;
            } catch (ClassNotFoundException|IllegalAccessException e) {
                throw new JsonParseException(e);
            }
        }
        return gson.fromJson(json, typeOfT);
    }

    @Override
    public JsonElement serialize(Object data, Type typeOfSrc, JsonSerializationContext serContext) {
        JsonElement dataElement = gson.toJsonTree(data);
        String type = data.getClass().getCanonicalName();
        if (dataElement instanceof JsonObject) {
            dataElement.getAsJsonObject().add("type", new JsonPrimitive(type));
            if (data instanceof PayloadEvent) {
                dataElement.getAsJsonObject().add(
                        "payloadType",
                        new JsonPrimitive(((PayloadEvent) data).payload.getClass().getCanonicalName())
                );
            }
        }
        return dataElement;
    }
}
