package ddd.repository.eventsourcing;

import java.util.Iterator;
import java.util.stream.Stream;
import java.util.Set;
import java.util.Optional;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.util.ConcurrentModificationException;

import eventstore.util.RuntimeGeneric;
import eventstore.Event;
import eventstore.EventStore;
import ddd.repository.IdentifiedEntity;
import ddd.repository.TemporalRepository;
import ddd.repository.PersistenceOrientedRepository;
import ddd.repository.exception.OptimisticLockingException;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toList;

/**
 * Event Sourcing based Repository. Implements a simple variation of command query responsibility segregation (CQRS) based
 * on events. Basically, it stores entity mutating events in one storage (for commands), and the current version of the
 * entity (snapshots) in another separate storage (for queries).
 * Handling of the events is handled by the given {@link EventStore}. For every entity in the repository there is a
 * stream in the given {@link EventStore}.
 * Note that the repository works only with a specific type of entities: {@link EventSourcedEntity}.
 * @param T type of the stored objects.
 * @param K type of the objects' identifiers.
 */
@SuppressWarnings("unchecked")
public abstract class EventSourcedRepository<T extends EventSourcedEntity<T> & IdentifiedEntity<K>, K>
    implements TemporalRepository<T, K>, PersistenceOrientedRepository<T, K>, RuntimeGeneric {

    protected EventStore eventStore;

    public EventSourcedRepository(EventStore eventStore) { init(eventStore); }
    protected EventSourcedRepository() {}

    protected void init(EventStore eventStore) {
        this.eventStore  = eventStore;
    }

    private Class<T> _entityClass;
    public Class<T> entityClass() { return _entityClass != null ? _entityClass : (_entityClass = (Class<T>) getClassArgument(0)); }

    @Override
    public Optional<T> get(K id) {
        return get(id, -1);
    }

    private Optional<T> get(K id, long version)  {
        return getByStreamName(streamName(id), version, snapshot(id, version));
    }

    private Optional<T> getByStreamName(String streamName, long version) {
        return getByStreamName(streamName, version, Optional.empty());
    }

    private Optional<T> getByStreamName(String streamName, long version, Optional<T> snapshot) {
        return eventStore.streamSince(streamName, snapshot.map((e) -> e.getUnmutatedVersion()).orElse(-1L))
            .map(e -> e.iterator())
            .flatMap((events) -> {
            if (!events.hasNext()) {
                return snapshot;
            }
            T entity = (T) (snapshot.isPresent() ? snapshot.get() : initEntity((InitialEvent) events.next()));
            while (entity.getMutatedVersion() != version && events.hasNext()) {
                Event event = events.next();
                if (event instanceof RemovedEvent) {
                    return Optional.empty();
                }
                entity = entity.apply(event);
            }
            return Optional.of(entity.commitChanges());
        });
    }

    protected void saveSnapshot(T committed, long unmutatedVersion) {}

    protected boolean removeSnapshot(K id) { return false; }

    protected Optional<T> snapshot(K id, long before) { return Optional.empty(); }

    private T initEntity(Event initEvent) {
        return ((InitialEvent<T>) initEvent).initializedObject();
    }

    private Optional<T> getAndApply(K id, long after, Stream<Event> changes) {
        if (after <= 0) {
            return getAndApply(id, 1, changes.skip(1));
        } else {
            return get(id, after).flatMap((entity) -> {
                return eventStore.streamSince(streamName(id), after).map((e) -> e.iterator()).map((events) -> {
                    T mutatedEntity = entity;
                    int i = 0;
                    Iterator<Event> changesIterator = changes.iterator();
                    Event newEvent = null;
                    Event savedEvent = null;
                    while(events.hasNext()) {
                        mutatedEntity = mutatedEntity.apply(savedEvent = events.next());
                        // Trying to find the event, where the stream starts to differ from the saved one.
                        if (newEvent == null) {
                            newEvent = changesIterator.next();
                            if (newEvent.equals(savedEvent)) {
                                newEvent = null;
                            }
                        }
                    }
                    mutatedEntity = mutatedEntity.commitChanges();
                    // Do not forget to apply the first different event we found,
                    // since it's already read from the iterator.
                    if (newEvent != null) {
                        mutatedEntity = mutatedEntity.apply(newEvent);
                    }
                    while(changesIterator.hasNext()) { mutatedEntity = mutatedEntity.apply(changesIterator.next()); }
                    return mutatedEntity;
                });
            });
        }
    }

    @Override
    public T save(T entity) {
        if (entity.getChanges().isEmpty()){
            return entity;
        } else {
            try {
                eventStore.append(streamName(entity.getId()), entity.getUnmutatedVersion(), entity.getChanges());
                T committed = entity.commitChanges();
                saveSnapshot(committed, entity.getUnmutatedVersion());
                return committed;
            } catch (ConcurrentModificationException e) {
                try {
                    T freshEntity = getAndApply(entity.getId(), entity.getUnmutatedVersion(), entity.getChanges().stream()).get();
                    if (freshEntity.getUnmutatedVersion() == entity.getUnmutatedVersion()) {
                        throw new IllegalStateException(
                                String.format(
                                    "Couldn't resolve the conflict, the saved entity %s (%s, %s), but got fresh %s (%s).",
                                    entity,
                                    entity.getUnmutatedVersion(),
                                    entity.getChanges(),
                                    freshEntity,
                                    eventStore.version(streamName(entity.getId()))), e);
                    }
                    return save(freshEntity);
                } catch (EventSourcingException esException) {
                    throw new OptimisticLockingException("Couldn't resolve the conflict", esException);
                }
            }
        }
    }

    @Override
    public long size() {
        return eventStore.size();
    }

    @Override
    public boolean remove(K id) {
        if (!contains(id)) {
            return false;
        }
        try {
            eventStore.append(streamName(id), new RemovedEvent<K>(id));
            removeSnapshot(id);
            return true;
        } catch(ConcurrentModificationException e) {
            return remove(id);
        }
    }

    /**
     * Whether the repository had the given entity.
     * @param id id of the entity.
     * @return true, if it contained the entity.
     */
    @Override
    public boolean contained(K id) { return eventStore.contains(streamName(id)); }

    protected String streamName(K id) {
        return this.entityClass().getSimpleName() + id;
    }
}
