package ddd.repository.eventsourcing;

import ddd.repository.AbstractRepository;
import ddd.repository.IdentifiedEntity;
import ddd.repository.eventsourcing.EventSourcedEntity;
import ddd.repository.UnitOfWork;
import eventstore.EventStore;
import java.util.Optional;

public abstract class EventStoreBasedRepository<T extends EventSourcedEntity<T> & IdentifiedEntity<K>, K>
    extends EventSourcedRepository<T, K, Object, Object> {
    public EventStoreBasedRepository(EventStore eventStore, Optional<UnitOfWork> uow) {
        super(eventStore, null, uow);
    }

    protected boolean removeSnapshot(K id) { return false; }

    protected void saveSnapshot(T committed, long unmutatedVersion) { }

    protected Object doSave(Object dbObject, Optional<Long> currentVersion) { return null; }

    protected boolean doRemove(Object id) { return false; }

    protected Optional<Object> doGet(Object id) { return Optional.empty(); }

    protected Object toDbId(Object id) { return null; }
}
