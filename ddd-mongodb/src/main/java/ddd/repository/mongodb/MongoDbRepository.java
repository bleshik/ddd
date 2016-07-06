package ddd.repository.mongodb;

import ddd.repository.exception.OptimisticLockingException;
import eventstore.util.RuntimeGeneric;
import ddd.repository.PersistenceOrientedRepository;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Iterator;
import com.mongodb.MongoCommandException;
import java.util.stream.Stream;
import eventstore.EventStore;
import java.util.stream.StreamSupport;
import java.util.Optional;
import java.lang.reflect.Field;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DB;
import com.mongodb.DuplicateKeyException;
import ddd.repository.IdentifiedEntity;
import ddd.repository.eventsourcing.EventSourcedRepository;
import ddd.repository.eventsourcing.EventSourcedEntity;
import eventstore.mongodb.MongoDbEventStore;
import eventstore.util.mongodb.Migration;
import eventstore.util.mongodb.MongoDbObjectMapper;
import eventstore.util.mongodb.GsonMongoDbObjectMapper;
import eventstore.util.collection.Collections;

/**
 * Simple MongoDB based repository. It just does the POJO mapping and puts it into the DB.
 */
@SuppressWarnings("unchecked")
public abstract class MongoDbRepository<T extends IdentifiedEntity<K>, K> implements PersistenceOrientedRepository<T, K>, RuntimeGeneric {

    protected DBCollection entityCollection;
    private MongoDbObjectMapper mapper;
    protected Class<T> entityClass;

    public MongoDbRepository(DBCollection entityCollection, MongoDbObjectMapper mapper) {
        init(entityCollection, mapper);
    }

    public MongoDbRepository(DB db, MongoDbObjectMapper mapper) {
        init(db.getCollection(((Class<T>) getClassArgument(0)).getSimpleName()), mapper);
    }

    public MongoDbRepository(DB db) {
        this(db, new GsonMongoDbObjectMapper());
    }

    protected void init(DBCollection entityCollection, MongoDbObjectMapper mapper) {
        this.entityCollection = entityCollection;
        this.mapper           = mapper;
        this.entityClass      = (Class<T>) getClassArgument(0);

        Migration.migrate(() -> {
            entityCollection.createIndex(new BasicDBObject("id", 1), new BasicDBObject("unique", true));
            migrate();
        });
    }

    @Override
    public Optional<T> get(K id) {
        return Collections.stream(entityCollection.find(new BasicDBObject("id", id)).iterator()).findFirst().map((e) -> deserialize(e));
    }

    @Override
    public long size() { return entityCollection.count(); }

    @Override
    public T save(T entity) {
        try {
            DBObject dbObject = serialize(entity);
            Optional<Long> version = Optional.ofNullable((Long) dbObject.get("version"));
            entityCollection.findAndModify(
                    version.map(
                        (v) -> new BasicDBObject("version", v - 1).append("id", entity.getId())
                    ).orElse(
                        new BasicDBObject("id", entity.getId())
                    ),
                    null,
                    null,
                    false,
                    dbObject,
                    false,
                    version.map((v) -> v == 1).orElse(true)
            );
            return deserialize(dbObject);
        } catch(DuplicateKeyException e) {
            if (e.getErrorCode() == 11000) {
                throw new OptimisticLockingException("The document " + entity + " was already changed.", e);
            } else {
                throw e;
            }
        }
    }

    @Override
    public boolean remove(K id) {
        return entityCollection.remove(new BasicDBObject("id", id)).getN() > 0;
    }

    protected DBObject serialize(T entity) {
        DBObject dbObject = mapper.mapToDbObject(entity);
        dbObject.put("id", entity.getId());
        version(entity).ifPresent((v) -> dbObject.put("version", v + 1));
        dbObject.put("updateDate", System.currentTimeMillis());
        return dbObject;
    }

    protected T deserialize(DBObject dbObject) {
        if (dbObject == null) {
            return null;
        }
        return (T) mapper.mapToObject(dbObject);
    }

    protected void migrate() {}

    private Optional<Long> version(T entity) {
        try {
            Field versionField = entityClass.getDeclaredField("version");
            versionField.setAccessible(true);
            return Optional.ofNullable((Long) versionField.get(entity));
        } catch (NoSuchFieldException|IllegalAccessException e) {
            return Optional.empty();
        }
    }
}
