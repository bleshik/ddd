package ddd.repository.eventsourcing.example.port.adapter.persistence;

import ddd.eventstore.impl.InMemoryEventStore;
import ddd.repository.eventsourcing.example.domain.HouseRepository;

public class InMemoryHouseRepositorySpec extends AbstractHouseRepositorySpec {
    public InMemoryHouseRepositorySpec() { super(new EventSourcedHouseRepository(new InMemoryEventStore())); }
}
