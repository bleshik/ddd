package repository.eventsourcing.example.port.adapter.persistence;

import com.github.fakemongo.Fongo;
import repository.eventsourcing.example.domain.House;
import repository.eventsourcing.example.domain.HouseRepository;
import repository.eventsourcing.mongodb.MongoDbEventSourcedRepository;

public class MongoDbEventSourcedRepositorySpec extends AbstractHouseRepositorySpec {
    public MongoDbEventSourcedRepositorySpec() {
        super(new MongoDbEventSourcedRepository<House, String>(new Fongo("Mongo").getDB("Mongo")){});
    }
}
