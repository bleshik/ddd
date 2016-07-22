package eventstore.dynamodb;

import eventstore.impl.AbstractEventStoreSpec;
import eventstore.util.dynamodb.LocalAmazonDynamoDbClient;
import java.util.UUID;

public class DynamoDbEventStoreSpec extends AbstractEventStoreSpec {
    public DynamoDbEventStoreSpec() {
        super(withObject(
                    new LocalAmazonDynamoDbClient(),
                    (client) -> {
                        String eventStoreTable = "Events" + UUID.randomUUID();
                        return () -> new DynamoDbEventStore(client, eventStoreTable, 1000, 1000);
                    }
        ));
    }
}
