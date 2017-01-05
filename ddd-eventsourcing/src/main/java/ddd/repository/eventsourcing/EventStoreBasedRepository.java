package ddd.repository.eventsourcing;

import ddd.repository.AbstractRepository;
import ddd.repository.IdentifiedEntity;
import ddd.repository.UnitOfWork;
import ddd.repository.eventsourcing.EventSourcedEntity;
import eventstore.EventStore;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class EventStoreBasedRepository<T extends EventSourcedEntity<T> & IdentifiedEntity<K>, K>
    extends EventSourcedRepository<T, K, Object, Object> {
    public EventStoreBasedRepository(EventStore eventStore, Optional<Supplier<UnitOfWork>> uow) {
        super(eventStore, null, uow);
    }

    protected boolean removeSnapshot(K id) { return false; }

    protected void saveSnapshot(T committed, long unmutatedVersion) { }

    protected Object doSave(Object dbObject, Optional<Long> currentVersion) { return null; }

    protected boolean doRemove(Object id) { return false; }

    protected Optional<Object> doGet(Object id) { return Optional.empty(); }

    protected Object toDbId(Object id) { return null; }
}
