package ddd.repository.eventsourcing.example.port.adapter.persistence;

import ddd.eventstore.EventStore;
import ddd.repository.eventsourcing.EventSourcedRepository;
import ddd.repository.eventsourcing.example.domain.HouseRepository;
import ddd.repository.eventsourcing.example.domain.House;

public class EventSourcedHouseRepository extends EventSourcedRepository<House, String> implements HouseRepository {
    public EventSourcedHouseRepository(EventStore eventstore) {
        super(eventstore);
    }
}
