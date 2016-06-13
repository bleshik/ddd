package repository.eventsourcing.example.port.adapter.persistence;

import eventstore.impl.InMemoryEventStore;
import repository.eventsourcing.example.domain.HouseRepository;

public class InMemoryHouseRepositorySpec extends AbstractHouseRepositorySpec {
    public InMemoryHouseRepositorySpec() { super(new EventSourcedHouseRepository(new InMemoryEventStore())); }
}
