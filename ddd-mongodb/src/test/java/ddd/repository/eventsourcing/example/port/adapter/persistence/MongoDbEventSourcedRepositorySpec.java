package ddd.repository.eventsourcing.example.port.adapter.persistence;

import com.github.fakemongo.Fongo;
import ddd.repository.eventsourcing.example.domain.House;
import ddd.repository.eventsourcing.example.domain.HouseRepository;
import ddd.repository.eventsourcing.mongodb.MongoDbEventSourcedRepository;

public class MongoDbEventSourcedRepositorySpec extends AbstractHouseRepositorySpec {
    public MongoDbEventSourcedRepositorySpec() {
        super(new MongoDbEventSourcedRepository<House, String>(new Fongo("Mongo").getDB("Mongo")){});
    }
}
