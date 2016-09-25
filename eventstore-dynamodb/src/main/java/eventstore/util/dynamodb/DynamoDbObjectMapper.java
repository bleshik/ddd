package eventstore.util.dynamodb;

import com.amazonaws.services.dynamodbv2.document.Item;
import eventstore.util.json.JsonDbObjectMapper;
import eventstore.util.json.JsonSerde;

public class DynamoDbObjectMapper extends JsonDbObjectMapper<Item> {
    public DynamoDbObjectMapper(JsonSerde jsonSerde) {
        super(jsonSerde, (dbObject) -> dbObject.toJSON(), (json) -> Item.fromJSON(json));
    }

}
