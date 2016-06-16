package ddd.repository.eventsourcing.mongodb;

import com.github.fakemongo.Fongo;
import ddd.repository.example.domain.House;
import ddd.repository.eventsourcing.example.domain.EventSourcedHouse;
import ddd.repository.AbstractHouseRepositorySpec;

public class MongoDbEventSourcedRepositorySpec extends AbstractHouseRepositorySpec {
    public MongoDbEventSourcedRepositorySpec() {
        super(
                new MongoDbEventSourcedRepository<EventSourcedHouse, String>(new Fongo("Mongo").getDB("Mongo")){},
                new EventSourcedHouse("100500 Awesome str., Chicago, USA", 100500, "Alexey Balchunas")
        );
    }
}
