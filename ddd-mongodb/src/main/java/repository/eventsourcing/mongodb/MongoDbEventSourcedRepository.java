package repository.eventsourcing.mongodb;

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
import repository.IdentifiedEntity;
import repository.eventsourcing.EventSourcedRepository;
import repository.eventsourcing.EventSourcedEntity;
import eventstore.mongodb.MongoDbEventStore;
import util.mongodb.Migration;
import util.mongodb.MongoDbObjectMapper;
import util.mongodb.GsonMongoDbObjectMapper;
import util.collection.Collections;

public abstract class MongoDbEventSourcedRepository<T extends EventSourcedEntity<T> & IdentifiedEntity<K>, K> extends EventSourcedRepository<T, K> {

    protected DBCollection snapshots;
    private MongoDbObjectMapper mapper;

    public MongoDbEventSourcedRepository(DBCollection snapshots, EventStore eventStore, MongoDbObjectMapper mapper) {
        init(snapshots, eventStore, mapper);
    }

    public MongoDbEventSourcedRepository(DB db, EventStore eventStore) {
        init(db.getCollection(entityClass().getSimpleName()), eventStore, new GsonMongoDbObjectMapper());
    }

    public MongoDbEventSourcedRepository(DB db) {
        init(db.getCollection(entityClass().getSimpleName()), new MongoDbEventStore(db.getCollection(entityClass().getSimpleName() + "Events")), new GsonMongoDbObjectMapper());
    }

    protected void init(DBCollection snapshots, EventStore eventStore, MongoDbObjectMapper mapper) {
        init(eventStore);
        this.snapshots = snapshots;
        this.mapper    = mapper;
        Migration.migrate(() -> {
            snapshots.createIndex(new BasicDBObject("id", 1), new BasicDBObject("unique", true));
            migrate();
        });
    }

    @Override
    protected void saveSnapshot(T committed, long unmutatedVersion) {
        try {
            snapshots.findAndModify(
                    new BasicDBObject("_version", unmutatedVersion).append("id", committed.getId()),
                    null,
                    null,
                    false,
                    serialize(committed),
                    false,
                    unmutatedVersion == 0
            );
        } catch(DuplicateKeyException e) {
            if (e.getErrorCode() == 11000) {
                // ignore duplicates, because this means that there is already a saved snapshot with higher version
            } else {
                throw e;
            }
        }
    }

    protected boolean removeSnapshot(K id) {
        return snapshots.remove(new BasicDBObject("id", id)).getN() > 0;
    }

    protected DBObject serialize(T entity) {
        DBObject dbObject = mapper.mapToDbObject(entity);
        dbObject.put("id", entity.getId());
        dbObject.put("_version", entity.getMutatedVersion());
        dbObject.put("_updateDate", System.currentTimeMillis());
        return dbObject;
    }

    protected T deserialize(DBObject dbObject) {
        try {
            if (dbObject == null) {
                return null;
            }
            T entity = mapper.mapToObject(dbObject, entityClass());

            if (dbObject.get("_version") != null) {
                Field versionField = EventSourcedEntity.class.getDeclaredField("_version");
                versionField.setAccessible(true);
                versionField.set(entity, (long) dbObject.get("_version"));
            }

            Field updateDateField = EventSourcedEntity.class.getDeclaredField("_updateDate");
            updateDateField.setAccessible(true);
            updateDateField.set(entity, Optional.ofNullable((Long) dbObject.get("_updateDate")).orElse(-1L));

            return entity;
        } catch(NoSuchFieldException|IllegalAccessException e) { throw new AssertionError("This shouldn't happen"); }
    }

    protected Optional<T> snapshot(K id, long before) {
        DBObject dbObject = snapshots.findOne(
                new BasicDBObject("_version", new BasicDBObject("$lte", before < 0 ? Long.MAX_VALUE : before)).append("id", id)
        );
        return Optional.ofNullable(deserialize(dbObject));
    }

    protected void migrate() {}

    protected Stream<T> find(DBObject query) {
        return find(query, new BasicDBObject(), 100, 0, new BasicDBObject());
    }

    protected Stream<T> find(DBObject query, DBObject orderBy) {
        return find(query, orderBy, 100, 0, new BasicDBObject());
    }

    protected Stream<T> find(DBObject query, DBObject orderBy, int limit) {
        return find(query, orderBy, limit, 0, new BasicDBObject());
    }

    protected Stream<T> find(DBObject query, DBObject orderBy, int limit, int offset) {
        return find(query, orderBy, limit, offset, new BasicDBObject());
    }

    protected Stream<T> find(DBObject query, DBObject orderBy, int limit, int offset, DBObject hint) {
        return Collections.stream((Iterator<DBObject>) snapshots.find(query).sort(orderBy).hint(hint).skip(offset).limit(limit)).map(e -> deserialize(e));
    }

    protected Optional<T> findOne(DBObject query) {
        return Optional.ofNullable(snapshots.findOne(query)).map(this::deserialize);
    }
}
