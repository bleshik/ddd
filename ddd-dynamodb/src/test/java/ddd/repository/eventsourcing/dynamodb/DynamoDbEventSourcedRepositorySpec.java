package ddd.repository.eventsourcing.dynamodb;

import ddd.repository.AbstractHouseRepositorySpec;
import ddd.repository.eventsourcing.example.domain.EventSourcedHouse;
import ddd.repository.example.domain.House;
import eventstore.util.dynamodb.LocalAmazonDynamoDbClient;

import java.util.Optional;
import java.util.UUID;

public class DynamoDbEventSourcedRepositorySpec
    extends AbstractHouseRepositorySpec<EventSourcedHouse, DynamoDbEventSourcedRepository<EventSourcedHouse, String>> {
    public DynamoDbEventSourcedRepositorySpec() {
        super(
                new DynamoDbEventSourcedRepository<EventSourcedHouse, String>(
                    new LocalAmazonDynamoDbClient(9823),
                    UUID.randomUUID().toString(),
                    1000,
                    1000,
                    Optional.empty()
                ){},
                new EventSourcedHouse("100500 Awesome str., Chicago, USA", 100500, "Alexey Balchunas")
        );
    }
}
