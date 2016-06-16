package ddd.repository.eventsourcing;

import ddd.eventstore.impl.InMemoryEventStore;
import ddd.repository.AbstractHouseRepositorySpec;
import ddd.repository.eventsourcing.example.domain.EventSourcedHouse;
import ddd.repository.AbstractHouseRepositorySpec;

public class EventSourcedRepositorySpec extends AbstractHouseRepositorySpec {
    public EventSourcedRepositorySpec() {
        super(
                new EventSourcedRepository<EventSourcedHouse, String>(new InMemoryEventStore()) {},
                new EventSourcedHouse("100500 Awesome str., Chicago, USA", 100500, "Alexey Balchunas")
        );
    }
}
