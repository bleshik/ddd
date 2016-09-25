package ddd.repository.eventsourcing;

import eventstore.impl.InMemoryEventStore;
import ddd.repository.AbstractHouseRepositorySpec;
import ddd.repository.eventsourcing.example.domain.EventSourcedHouse;
import ddd.repository.AbstractHouseRepositorySpec;

public class EventSourcedRepositorySpec
    extends AbstractHouseRepositorySpec<EventSourcedHouse, EventStoreBasedRepository<EventSourcedHouse, String>> {
    public EventSourcedRepositorySpec() {
        super(
                new EventStoreBasedRepository<EventSourcedHouse, String>(new InMemoryEventStore()) {},
                new EventSourcedHouse("100500 Awesome str., Chicago, USA", 100500, "Alexey Balchunas")
        );
    }
}
