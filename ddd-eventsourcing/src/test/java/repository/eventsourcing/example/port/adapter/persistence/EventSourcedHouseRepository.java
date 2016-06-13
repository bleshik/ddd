package repository.eventsourcing.example.port.adapter.persistence;

import eventstore.EventStore;
import repository.eventsourcing.EventSourcedRepository;
import repository.eventsourcing.example.domain.HouseRepository;
import repository.eventsourcing.example.domain.House;

public class EventSourcedHouseRepository extends EventSourcedRepository<House, String> implements HouseRepository {
    public EventSourcedHouseRepository(EventStore eventstore) {
        super(eventstore);
    }
}
