package eventstore.mongodb;

import java.util.stream.Collectors;
import eventstore.EventStoreException;
import com.mongodb.DuplicateKeyException;
import java.util.stream.StreamSupport;
import java.util.Iterator;
import com.mongodb.DBObject;
import java.util.Set;
import java.util.stream.Stream;
import java.util.Optional;
import com.mongodb.BulkWriteOperation;
import com.mongodb.BulkWriteException;
import com.mongodb.MongoException;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.ConcurrentModificationException;
import eventstore.util.collection.Collections;

import eventstore.Event;
import eventstore.EventStore;
import eventstore.util.mongodb.Migration;
import eventstore.util.mongodb.MongoDbObjectMapper;
import eventstore.util.mongodb.GsonMongoDbObjectMapper;

/**
 * MongoDB-based event store. Stores all streams in a single db collection. The idea is that there a collection per
 * entity.
 */
@SuppressWarnings("unchecked")
public class MongoDbEventStore implements EventStore {

    private final DBCollection dbCollection;
    private final MongoDbObjectMapper mapper;

    public MongoDbEventStore(DBCollection dbCollection) { 
        this(dbCollection, new GsonMongoDbObjectMapper());
    }

    public MongoDbEventStore(DBCollection dbCollection, MongoDbObjectMapper mapper) { 
        this.dbCollection = dbCollection;
        this.mapper       = mapper;
        Migration.migrate( () -> {
            dbCollection.createIndex(new BasicDBObject("occurredOn", 1));
        });
    }

    @Override
    public Optional<Stream<Event>> streamSince(String streamName, long lastReceivedEvent) {
        Iterator<DBObject> cursor = dbCollection.find(
                new BasicDBObject("_id._streamId", streamName).append("_id._idx", new BasicDBObject("$gt", lastReceivedEvent - 1))
        ).sort(new BasicDBObject("occurredOn", 1));
        if (!cursor.hasNext()) {
            return Optional.empty();
        } else {
            return Optional.of(Collections.stream(cursor).map(mongoObject -> deserialize(mongoObject)));
        }
    }

    private Event deserialize(DBObject mongoObject) {
        try {
            return (Event) mapper.mapToObject(mongoObject, (Class<? extends Event>) Class.forName(mongoObject.get("_type").toString()));
        } catch(ClassNotFoundException e) {
            throw new EventStoreException(String.format("Failed to deserialize %s", mongoObject), e);
        }
    }

    private DBObject serialize(Event event) {
        DBObject obj = mapper.mapToDbObject(event);
        obj.put("_type", event.getClass().getCanonicalName());
        obj.put("streamVersion", event.getStreamVersion());
        obj.put("occurredOn", System.currentTimeMillis());
        return obj;
    }

    @Override
    public void append(String streamName, long currentVersion, List<? extends Event> newEvents) {
        try {
            BulkWriteOperation operation = dbCollection.initializeOrderedBulkOperation();
            long nextEventIndex = currentVersion;
            for (Event event : newEvents) {
                DBObject dbObject = serialize(event.occurred(++nextEventIndex));
                dbObject.put("_id", new BasicDBObject("_idx", dbObject.get("streamVersion")).append("_streamId", streamName));
                operation.insert(dbObject);
            };
            operation.execute();
        } catch(BulkWriteException e) {
            if (e.getWriteErrors().stream().anyMatch(event -> event.getCode() == 11000)) {
                throw new ConcurrentModificationException(e);
            } else {
                throw e;
            }
        } catch(DuplicateKeyException e) {
            throw new ConcurrentModificationException(e);
        }
    }

    @Override
    public long size() {
        return Collections.stream(dbCollection.aggregate(new ArrayList<DBObject>(){{
            add(new BasicDBObject("$group", new BasicDBObject("_id", "$_id._streamId")));
            add(new BasicDBObject("$group", new BasicDBObject("_id", 1).append("count", new BasicDBObject("$sum", 1L))));
        }}).results()).map(e -> (long) e.get("count")).findAny().orElse(0L);
    }

    @Override
    public long version(String streamName) {
        return Collections.stream(dbCollection.aggregate(new ArrayList<DBObject>(){{
            add(new BasicDBObject("$match", new BasicDBObject("_id._streamId", streamName)));
            add(new BasicDBObject("$sort", new BasicDBObject("_id._idx", -1)));
            add(new BasicDBObject("$limit", 1));
        }}).results()).map(e -> (long) ((DBObject) e.get("_id")).get("_idx")).findAny().orElse(0L);
    }
}
