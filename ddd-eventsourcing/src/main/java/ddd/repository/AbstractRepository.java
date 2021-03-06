package ddd.repository;

import ddd.repository.IdentifiedEntity;
import ddd.repository.PersistenceOrientedRepository;
import ddd.repository.eventsourcing.EventSourcedEntity;
import eventstore.util.DbObjectMapper;
import eventstore.util.RuntimeGeneric;
import eventstore.util.collection.Collections;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public abstract class AbstractRepository<T extends IdentifiedEntity<K>, K, D, DK> implements PersistenceOrientedRepository<T, K>, RuntimeGeneric {

    protected Optional<Supplier<UnitOfWork>> unitOfWork = Optional.empty();
    protected DbObjectMapper<D> mapper;
    protected Class<T> entityClass;

    public static final ClassValue<Optional<Field>> versionField = new ClassValue<Optional<Field>>() {
        @Override
        protected Optional<Field> computeValue(Class<?> type) {
            try {
                Field versionField = EventSourcedEntity.class.isAssignableFrom(type) ?
                    EventSourcedEntity.class.getDeclaredField("_version") :
                    type.getDeclaredField("version");
                versionField.setAccessible(true);
                return Optional.ofNullable(versionField);
            } catch (NoSuchFieldException e) {
                return Optional.empty();
            }
        }
    };

    public AbstractRepository(DbObjectMapper<D> mapper, Optional<Supplier<UnitOfWork>> unitOfWork) {
        this.mapper = mapper;
        this.entityClass = (Class<T>) getClassArgument(0);
        this.unitOfWork = unitOfWork;
    }

    public AbstractRepository(DbObjectMapper<D> mapper) {
        this(mapper, Optional.empty());
    }

    public AbstractRepository(DbObjectMapper<D> mapper, Supplier<UnitOfWork> unitOfWork) {
        this(mapper, Optional.of(unitOfWork));
    }

    @Override
    public Optional<T> get(K id) {
        return reading(id, () -> doGet(toDbId(id)).map((e) -> deserialize(e)));
    }

    @Override
    public T save(T entity) {
        return saving(entity, () -> deserialize(doSave(serialize(entity), version(entity))));
    }

    @Override
    public boolean remove(K id) {
        return removing(id, () -> doRemove(toDbId(id)));
    }

    protected abstract D doSave(D dbObject, Optional<Long> currentVersion);

    protected abstract boolean doRemove(DK id);

    protected abstract Optional<D> doGet(DK id);

    protected abstract DK toDbId(K id);

    protected T deserialize(D dbObject) {
        T entity = (T) mapper.mapToObject(dbObject);
        // put in the cache everything we deserialize
        if (!isFlushing()) {
            register(entity);
        }
        return entity;
    }

    protected D serialize(T entity) {
        return mapper.mapToDbObject(entity);
    }

    private Optional<Long> version(T entity) {
        return versionField.get(entityClass).map((field) -> {
            try {
                return (Long) field.get(entity);
            } catch (IllegalAccessException e) {
                throw new AssertionError("This shouldn't happen", e);
            }
        });
    }

    public void flush() {
        withUow(uow -> uow.flush(this));
    }

    protected boolean isFlushing() {
        return unitOfWork.map(uow -> uow.get().isFlushing()).orElse(true);
    }

    protected <V> Optional<V> ifFlushing(Callable<V> c) {
        if (isFlushing()) {
            try {
                return Optional.of(c.call());
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RepositoryException("Failed to flush changes", e);
            }
        }
        return Optional.empty();
    }

    protected Optional<T> reading(K id, Callable<Optional<T>> c) {
        if (unitOfWork.map(uow -> uow.get().isRemoved(this, id)).orElse(false)) {
            return Optional.empty();
        }
        Optional<T> cached = unitOfWork.flatMap(uow -> uow.get().get(this, id));
        if (cached.isPresent()) {
            return cached;
        } else {
           return reading(c);
        }
    }

    protected Optional<T> reading(Callable<Optional<T>> c) {
        try {
            return c.call().map(entity -> {
                register(entity);
                return entity;
            });
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RepositoryException("Read failure", e);
        }
    }

    protected T saving(T entity, Callable<T> c) {
        return ifFlushing(c).orElseGet(() -> {
            changed(entity);
            return entity;
        });
    }

    protected boolean removing(K id, Callable<Boolean> c) {
        return ifFlushing(c).orElseGet(() -> {
            removed(id);
            return true;
        });
    }

    private void register(T entity) {
        withUow(uow -> uow.register(this, entity));
    }

    private void changed(T entity) {
        withUow(uow -> uow.changed(this, entity));
    }

    private void removed(K id) {
        withUow(uow -> uow.removed(this, id));
    }

    protected void withUow(Consumer<UnitOfWork> c) {
        unitOfWork.ifPresent(uow -> c.accept(uow.get()));
    }

    /*
    @Override
    public Collection<T> findAllByIds(Iterable<K> ids) {
        return reading(() ->
            stream(ids).flatMap(id ->
                doGet(toDbId(id)).map((e) -> deserialize(e)).map(Stream::of).orElse(Stream.empty())
            ).collect(toSet())
        );
    }

    @Override
    public void saveAll(Iterable<T> entities) {
        saving(entities, () -> {
            entities.forEach((entity) -> deserialize(doSave(serialize(entity), version(entity))));
        });
    }

    protected void removing(Iterable<K> ids, Runnable r) {
        if (isFlushing()) {
            r.run();
        } else {
            removed(ids);
        }
    }

    protected Collection<T> reading(Callable<Collection<T>> c) {
        return c.call().map(entities -> {
            register(entities);
            return entities;
        });
    }

    protected void saving(Iterable<T> entities, Runnable r) {
        if (isFlushing()) {
            r.run();
        } else {
            changed(entities);
        }
    }

    private void register(Iterable<T> entities) {
        unitOfWork.ifPresent(uow -> entities.forEach(e -> uow.get().register(this, e)));
    }

    private void changed(Iterable<T> entities) {
        unitOfWork.ifPresent(uow -> entities.forEach(e -> uow.get().changed(this, e)));
    }

    private void removed(Iterable<K> ids) {
        unitOfWork.ifPresent(uow -> ids.forEach(id -> uow.get().removed(this, id)));
    }
    */
}
