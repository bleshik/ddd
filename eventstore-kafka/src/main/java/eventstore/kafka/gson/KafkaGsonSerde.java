package eventstore.kafka.gson;

import eventstore.PayloadEvent;
import eventstore.util.RuntimeGeneric;
import eventstore.util.json.GsonJsonSerde;
import eventstore.util.json.JsonSerde;
import java.lang.ClassNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

@SuppressWarnings("unchecked")
public class KafkaGsonSerde<T> implements Serde<T>, RuntimeGeneric {

    private final Type typeOfT = getTypeArgument(0);
    private final eventstore.util.json.JsonSerde jsonSerde;

    public KafkaGsonSerde() {
        this.jsonSerde = new GsonJsonSerde();
    }

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
                return jsonSerde.serialize(data).getBytes(StandardCharsets.UTF_8);
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
                return (T) jsonSerde.deserialize(new String(data, StandardCharsets.UTF_8));
            }

            @Override
            public void close() {}

        };
    }

}
