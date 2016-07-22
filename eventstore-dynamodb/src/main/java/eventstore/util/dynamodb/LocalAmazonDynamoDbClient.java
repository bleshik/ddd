package eventstore.util.dynamodb;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

/**
 * A utility class for having an easy way of creating a client for the DynamoDBLocal.
 */
public class LocalAmazonDynamoDbClient extends AmazonDynamoDBClient {

    public LocalAmazonDynamoDbClient() {
        this(8000);
    }

    public LocalAmazonDynamoDbClient(int port) {
        super(new BasicAWSCredentials("dummy", "dummy"));
        this.setEndpoint("http://localhost:" + port);
        this.setSignerRegionOverride("local");
    }

}
