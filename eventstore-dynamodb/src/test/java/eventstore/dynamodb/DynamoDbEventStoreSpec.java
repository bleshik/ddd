package eventstore.dynamodb;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import eventstore.impl.AbstractEventStoreSpec;
import java.sql.DriverManager;
import java.util.UUID;

public class DynamoDbEventStoreSpec extends AbstractEventStoreSpec {
    public DynamoDbEventStoreSpec() {
        super(withObject(
                    new LocalAmazonDynamoDbClient(8000),
                    (client) -> {
                        String eventStoreTable = "Events" + UUID.randomUUID();
                        return () -> new DynamoDbEventStore(client, eventStoreTable, 1000, 1000);
                    }
        ));
    }
}

class LocalAmazonDynamoDbClient extends AmazonDynamoDBClient {
    LocalAmazonDynamoDbClient(int port) {
        super(new BasicAWSCredentials("dummy", "dummy"));
        this.setEndpoint("http://localhost:" + port);
        this.setSignerRegionOverride("local");
    }
}
