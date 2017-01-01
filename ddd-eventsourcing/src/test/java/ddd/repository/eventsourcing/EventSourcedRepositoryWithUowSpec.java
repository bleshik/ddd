package ddd.repository.eventsourcing;

import eventstore.impl.InMemoryEventStore;
import ddd.repository.AbstractHouseRepositorySpec;
import ddd.repository.eventsourcing.example.domain.EventSourcedHouse;
import ddd.repository.UnitOfWork;
import java.util.Optional;

public class EventSourcedRepositoryWithUowSpec
    extends AbstractHouseRepositorySpec<EventSourcedHouse, EventStoreBasedRepository<EventSourcedHouse, String>> {
    public EventSourcedRepositoryWithUowSpec() {
        super(
                new EventStoreBasedRepository<EventSourcedHouse, String>(new InMemoryEventStore(), Optional.of(new UnitOfWork())) {},
                new EventSourcedHouse("100500 Awesome str., Chicago, USA", 100500, "Alexey Balchunas")
        );
    }
}
