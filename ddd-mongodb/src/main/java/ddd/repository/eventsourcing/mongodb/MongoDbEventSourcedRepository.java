package ddd.repository.eventsourcing.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoCommandException;
import ddd.repository.IdentifiedEntity;
import ddd.repository.eventsourcing.EventSourcedEntity;
import ddd.repository.eventsourcing.EventSourcedRepository;
import eventstore.EventStore;
import eventstore.mongodb.MongoDbEventStore;
import eventstore.util.collection.Collections;
import eventstore.util.mongodb.GsonMongoDbObjectMapper;
import eventstore.util.mongodb.Migration;
import eventstore.util.DbObjectMapper;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * MongoDB-based event sourced repository. This stores events and snapshots in the same MongoDB database. Optionally,
 * you can specify other {@link EventStore}.
 */
public abstract class MongoDbEventSourcedRepository<T extends EventSourcedEntity<T> & IdentifiedEntity<K>, K> extends EventSourcedRepository<T, K> {

    protected DBCollection snapshots;
    private DbObjectMapper<DBObject> mapper;

    public MongoDbEventSourcedRepository(DBCollection snapshots, EventStore eventStore, DbObjectMapper<DBObject> mapper) {
        init(snapshots, eventStore, mapper);
    }

    public MongoDbEventSourcedRepository(DB db, EventStore eventStore) {
        init(db.getCollection(entityClass().getSimpleName()), eventStore, new GsonMongoDbObjectMapper());
    }

    public MongoDbEventSourcedRepository(DB db) {
        init(db.getCollection(entityClass().getSimpleName()), new MongoDbEventStore(db.getCollection(entityClass().getSimpleName() + "Events")), new GsonMongoDbObjectMapper());
    }

    protected void init(DBCollection snapshots, EventStore eventStore, DbObjectMapper<DBObject> mapper) {
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
                    mapper.mapToDbObject(committed),
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

    protected Optional<T> snapshot(K id, long before) {
        DBObject dbObject = snapshots.findOne(
                new BasicDBObject("_version", new BasicDBObject("$lte", before < 0 ? Long.MAX_VALUE : before)).append("id", id)
        );
        return Optional.ofNullable((T) mapper.mapToObject(dbObject));
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
        return Collections.stream(
                (Iterator<DBObject>) snapshots.find(query).sort(orderBy).hint(hint).skip(offset).limit(limit)
        ).map(e -> (T) mapper.mapToObject(e));
    }

    protected Optional<T> findOne(DBObject query) {
        return Optional.ofNullable(snapshots.findOne(query)).map((e) -> (T) mapper.mapToObject(e));
    }
}
