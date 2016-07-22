package ddd.repository.dynamodb;

import ddd.repository.AbstractHouseRepositorySpec;
import ddd.repository.example.domain.ImmutableHouse;
import eventstore.util.dynamodb.LocalAmazonDynamoDbClient;
import java.util.UUID;

public class DynamoDbRepositorySpec extends AbstractHouseRepositorySpec<ImmutableHouse, DynamoDbRepository<ImmutableHouse, String>> {
    public DynamoDbRepositorySpec() {
        super(
                new DynamoDbRepository<ImmutableHouse, String>(
                    new LocalAmazonDynamoDbClient(8080),
                    UUID.randomUUID().toString(),
                    1000,
                    1000
                ){},
                new ImmutableHouse("100500 Awesome str., Chicago, USA", 100500, "Alexey Balchunas")
        );
    }
}
