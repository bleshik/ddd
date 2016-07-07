package eventstore.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteException;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoException;
import eventstore.Event;
import eventstore.EventStore;
import eventstore.EventStoreException;
import eventstore.util.collection.Collections;
import eventstore.util.mongodb.GsonMongoDbObjectMapper;
import eventstore.util.mongodb.Migration;
import eventstore.util.mongodb.MongoDbObjectMapper;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
                new BasicDBObject("_id._streamId", streamName).append("_id._idx", new BasicDBObject("$gt", lastReceivedEvent))
        ).sort(new BasicDBObject("occurredOn", 1));
        if (!cursor.hasNext()) {
            return version(streamName) > 0 ? Optional.of(Stream.empty()) : Optional.empty();
        } else {
            return Optional.of(Collections.stream(cursor).map(mongoObject -> (Event) mapper.mapToObject(mongoObject)));
        }
    }

    private DBObject serialize(Event event) {
        DBObject obj = mapper.mapToDbObject(event);
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
