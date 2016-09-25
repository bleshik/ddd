package ddd.repository;

import ddd.repository.IdentifiedEntity;
import ddd.repository.PersistenceOrientedRepository;
import ddd.repository.eventsourcing.EventSourcedEntity;
import eventstore.util.DbObjectMapper;
import eventstore.util.RuntimeGeneric;
import eventstore.util.collection.Collections;
import java.lang.reflect.Field;
import java.util.Optional;

@SuppressWarnings("unchecked")
public abstract class AbstractRepository<T extends IdentifiedEntity<K>, K, D, DK> implements PersistenceOrientedRepository<T, K>, RuntimeGeneric {

    protected DbObjectMapper<D> mapper;
    protected Class<T> entityClass;
    public final ClassValue<Optional<Field>> versionField = new ClassValue<Optional<Field>>() {
        @Override
        protected Optional<Field> computeValue(Class<?> type) {
            try {
                Field versionField = EventSourcedEntity.class.isAssignableFrom(entityClass) ?
                    EventSourcedEntity.class.getDeclaredField("_version") :
                    entityClass.getDeclaredField("version");
                versionField.setAccessible(true);
                return Optional.ofNullable(versionField);
            } catch (NoSuchFieldException e) {
                return Optional.empty();
            }
        }
    };

    public AbstractRepository(DbObjectMapper<D> mapper) {
        this.mapper = mapper;
        this.entityClass = (Class<T>) getClassArgument(0);
    }

    @Override
    public Optional<T> get(K id) {
        return doGet(toDbId(id)).map((e) -> deserialize(e));
    }

    @Override
    public T save(T entity) {
        return deserialize(doSave(serialize(entity), version(entity)));
    }

    @Override
    public boolean remove(K id) {
        return doRemove(toDbId(id));
    }

    protected abstract D doSave(D dbObject, Optional<Long> currentVersion);

    protected abstract boolean doRemove(DK id);

    protected abstract Optional<D> doGet(DK id);

    protected abstract DK toDbId(K id);

    protected T deserialize(D dbObject) {
        return (T) mapper.mapToObject(dbObject);
    }

    protected D serialize(T entity) {
        return mapper.mapToDbObject(entity);
    }

    private Optional<Long> version(T entity) {
        return versionField.get(entityClass).map((field) -> {
            try {
                return (Long) field.get(entity);
            } catch(IllegalAccessException e) {
                throw new AssertionError("This shouldn't happen", e);
            }
        });
    }
}
