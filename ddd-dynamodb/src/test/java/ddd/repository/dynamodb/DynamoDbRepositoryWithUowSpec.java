package ddd.repository.dynamodb;

import ddd.repository.AbstractHouseRepositorySpec;
import ddd.repository.UnitOfWork;
import ddd.repository.example.domain.ImmutableHouse;
import eventstore.util.dynamodb.LocalAmazonDynamoDbClient;
import java.util.*;

public class DynamoDbRepositoryWithUowSpec extends AbstractHouseRepositorySpec<ImmutableHouse, DynamoDbRepository<ImmutableHouse, String>> {
    public DynamoDbRepositoryWithUowSpec() {
        super(
                new DynamoDbRepository<ImmutableHouse, String>(
                    new LocalAmazonDynamoDbClient(9823),
                    UUID.randomUUID().toString(),
                    Optional.of(new UnitOfWork())
                ){},
                new ImmutableHouse("100500 Awesome str., Chicago, USA", 100500, "Alexey Balchunas")
        );
    }
}
