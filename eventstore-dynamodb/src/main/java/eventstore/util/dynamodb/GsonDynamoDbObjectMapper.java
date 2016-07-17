package eventstore.util.dynamodb;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.google.gson.Gson;
import eventstore.util.json.GsonJsonSerde;
import eventstore.util.json.JsonDbObjectMapper;

public class GsonDynamoDbObjectMapper extends JsonDbObjectMapper<Item> {

    public GsonDynamoDbObjectMapper() {
        this(new Gson());
    }

    public GsonDynamoDbObjectMapper(Gson gson) {
        super(new GsonJsonSerde(gson), (dbObject) -> dbObject.toJSON(), (json) -> Item.fromJSON(json));
    }

}
