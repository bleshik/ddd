package eventstore.util.dynamodb;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.google.gson.*;
import com.google.gson.stream.*;
import eventstore.util.dynamodb.DynamoDbObjectMapper;
import eventstore.util.json.GsonJsonSerde;
import eventstore.util.json.JsonDbObjectMapper;
import java.io.IOException;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;

/**
 * {@link Gson} based implementation of the mapper for DynamoDB.
 */
public class GsonDynamoDbObjectMapper extends DynamoDbObjectMapper {

    public GsonDynamoDbObjectMapper() {
        this(
                new GsonBuilder()
                // write empty string as null
                .registerTypeAdapter(String.class, new TypeAdapter<String>() {
                    public String read(JsonReader reader) throws IOException {
                        if (reader.peek() == JsonToken.NULL) {
                            reader.nextNull();
                            return null;
                        }
                        return reader.nextString();
                    }
                    public void write(JsonWriter writer, String value) throws IOException {
                        if ("".equals(value)) {
                            writer.nullValue();
                        } else {
                            writer.value(value);
                        }
                    }
                })
                .registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
                .serializeNulls()
                .create()
        );
    }

    public GsonDynamoDbObjectMapper(Gson gson) {
        super(new GsonJsonSerde(gson));
    }

}
