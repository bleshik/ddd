package ddd.repository.dynamodb;

import ddd.repository.AbstractHouseRepositorySpec;
import ddd.repository.UnitOfWork;
import ddd.repository.example.domain.ImmutableHouse;
import eventstore.util.dynamodb.LocalAmazonDynamoDbClient;
import java.util.*;

public class DynamoDbRepositorySpec extends AbstractHouseRepositorySpec<ImmutableHouse, DynamoDbRepository<ImmutableHouse, String>> {
    public DynamoDbRepositorySpec() {
        super(
                new DynamoDbRepository<ImmutableHouse, String>(
                    new LocalAmazonDynamoDbClient(9823),
                    UUID.randomUUID().toString(),
                    Optional.empty() 
                ){},
                new ImmutableHouse("100500 Awesome str., Chicago, USA", 100500, "Alexey Balchunas")
        );
    }
}
