package ddd.repository.eventsourcing;

import ddd.repository.AbstractRepository;
import ddd.repository.IdentifiedEntity;
import ddd.repository.PersistenceOrientedRepository;
import ddd.repository.TemporalRepository;
import ddd.repository.UnitOfWork;
import ddd.repository.exception.OptimisticLockingException;
import eventstore.Event;
import eventstore.EventStore;
import eventstore.PayloadEvent;
import eventstore.util.DbObjectMapper;
import eventstore.util.RuntimeGeneric;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
public abstract class EventSourcedRepository<T extends EventSourcedEntity<T> & IdentifiedEntity<K>, K, D, DK>
    extends AbstractRepository<T, K, D, DK>
    implements TemporalRepository<T, K>, PersistenceOrientedRepository<T, K>, RuntimeGeneric {

    protected EventStore eventStore;

    public EventSourcedRepository(EventStore eventStore, DbObjectMapper<D> mapper, Optional<Supplier<UnitOfWork>> uow) {
        super(mapper, uow);
        init(eventStore, mapper);
    }

    protected EventSourcedRepository() {
        super(null);
    }

    protected void init(EventStore eventStore, DbObjectMapper<D> mapper) {
        this.eventStore = eventStore;
        this.mapper     = mapper;
    }

    @Override
    public Optional<T> get(K id) {
        return reading(id, () -> get(id, -1));
    }

    private Optional<T> get(K id, long version)  {
        return getByStreamName(streamName(id), version, snapshot(id, version));
    }

    private Optional<T> getByStreamName(String streamName, long version, Optional<T> snapshot) {
        return eventStore.streamSince(streamName, snapshot.map((e) -> e.getUnmutatedVersion()).orElse(-1L))
            .map(e -> e.iterator())
            .flatMap((events) -> {
                if (!events.hasNext()) {
                    return snapshot;
                }
                T entity = (T) (snapshot.isPresent() ? snapshot.get() : initEntity(events.next()));
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

    protected void saveSnapshot(T committed, long unmutatedVersion) {
        doSave(serialize(committed), Optional.of(unmutatedVersion));
    }

    protected boolean removeSnapshot(K id) { return doRemove(toDbId(id)); }

    protected Optional<T> snapshot(K id, long before) {
        return doGet(toDbId(id)).map((i) -> deserialize(i));
    }

    private T initEntity(Event initEvent) {
        try {
            Map<Class, Constructor> constructors = EventSourcedEntity.constructors.get(entityClass);
            if (constructors.containsKey(initEvent.getClass())) {
                return (T) constructors.get(initEvent.getClass()).newInstance(initEvent);
            } else if (initEvent instanceof PayloadEvent) {
                Object payload = ((PayloadEvent) initEvent).payload;
                if (constructors.containsKey(payload.getClass())) {
                    return (T) constructors.get(payload.getClass()).newInstance(payload);
                } else {
                    throw new IllegalArgumentException("The entity does not have a constructor for the payload " + payload);
                }
            } else {
                throw new IllegalArgumentException("The entity does not have a constructor for the event " + initEvent);
            }
        } catch (InstantiationException|IllegalAccessException|InvocationTargetException e) {
            throw new EventSourcingException("Couldn't initiate the entity with event " + initEvent, e);
        }
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
                    while(changesIterator.hasNext()) {
                        Event anotherNewEvent = changesIterator.next();
                        mutatedEntity = mutatedEntity.apply(anotherNewEvent);
                    }
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
            return saving(entity, () -> {
                try {
                    for (Event event : entity.getChanges()) {
                        try {
                            Object actualEvent = event instanceof PayloadEvent ? ((PayloadEvent) event).payload : event;
                            Method handler = EventSourcedEntity.mutatingMethods.get(this.getClass()).get(
                                    actualEvent.getClass()
                            );
                            if (handler != null && handler.getParameterTypes().length == 2) {
                                handler.invoke(this, actualEvent, entity);
                            }
                        } catch (IllegalAccessException e) {
                            throw new AssertionError("This shouldn't happen");
                        } catch (InvocationTargetException e) {
                            throw new EventSourcingException(
                                    String.format("Exception occurred while handling the event %s.", event), e.getCause());
                        }
                    }
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
                    } catch (Exception esException) {
                        throw new OptimisticLockingException("Couldn't resolve the conflict", esException);
                    }
                }
            });
        }
    }

    @Override
    public long size() {
        flush();
        return eventStore.size();
    }

    @Override
    public boolean remove(K id) {
        return removing(id, () -> {
            if (!contains(id)) {
                return false;
            }
            try {
                eventStore.append(streamName(id), new RemovedEvent<K>(id));
                removeSnapshot(id);
                return true;
            } catch (ConcurrentModificationException e) {
                return remove(id);
            }
        });
    }

    /**
     * Whether the repository had the given entity.
     * @param id id of the entity.
     * @return true, if it contained the entity.
     */
    @Override
    public boolean contained(K id) { flush(); return eventStore.contains(streamName(id)); }

    protected String streamName(K id) {
        return this.entityClass.getSimpleName() + id;
    }
}
