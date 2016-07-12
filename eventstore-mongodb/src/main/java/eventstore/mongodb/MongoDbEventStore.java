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
import eventstore.util.DbObjectMapper;
import eventstore.util.collection.Collections;
import eventstore.util.mongodb.GsonMongoDbObjectMapper;
import eventstore.util.mongodb.Migration;
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
import org.apache.commons.codec.digest.DigestUtils;

/**
 * MongoDB-based event store. Stores all streams in a single db collection. The idea is that there a collection per
 * entity.
 * Note, that all entries will have "_id" with two fields: "_idx" and "_streamId". "_streamId" is a hashed value of
 * a stream name. (_id._streamId, _id._idx) should be a good candidate for shard key. MongoDB has support for
 * hash indexes, but they cannot be unique, so we hash it in order to make a good (uniformly distributed) range shard
 * key.
 * @see <a href="https://jira.mongodb.org/browse/SERVER-5878">Allow hashed indexes to be unique</a>
 */
@SuppressWarnings("unchecked")
public class MongoDbEventStore implements EventStore {

    private final DBCollection dbCollection;
    private final DbObjectMapper<DBObject> mapper;

    public MongoDbEventStore(DBCollection dbCollection) { 
        this(dbCollection, new GsonMongoDbObjectMapper());
    }

    public MongoDbEventStore(DBCollection dbCollection, DbObjectMapper<DBObject> mapper) { 
        this.dbCollection = dbCollection;
        this.mapper       = mapper;
        Migration.migrate(() -> {
            dbCollection.createIndex(new BasicDBObject("occurredOn", 1));
        });
    }

    public String hashedStreamName(String streamName) {
        return DigestUtils.md5Hex(streamName) + streamName;
    }

    @Override
    public Optional<Stream<Event>> streamSince(String streamName, long lastReceivedEvent) {
        Iterator<DBObject> cursor = dbCollection.find(
                new BasicDBObject("_id._streamId", hashedStreamName(streamName))
                    .append("_id._idx", new BasicDBObject("$gt", lastReceivedEvent))
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
                dbObject.put("_id", new BasicDBObject("_streamId", hashedStreamName(streamName))
                        .append("_idx", dbObject.get("streamVersion")));
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
            add(new BasicDBObject("$match", new BasicDBObject("_id._streamId", hashedStreamName(streamName))));
            add(new BasicDBObject("$sort", new BasicDBObject("_id._idx", -1)));
            add(new BasicDBObject("$limit", 1));
        }}).results()).map(e -> (long) ((DBObject) e.get("_id")).get("_idx")).findAny().orElse(0L);
    }
}
