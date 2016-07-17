package eventstore;

import eventstore.util.DbObjectMapper;
import eventstore.util.collection.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

public abstract class AbstractEventStore<T> implements EventStore {

    protected final DbObjectMapper<T> mapper;

    protected AbstractEventStore(DbObjectMapper<T> mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Stream<Event>> streamSince(String streamName, long lastReceivedEvent) {
        Iterator<T> cursor = iteratorSince(streamName, lastReceivedEvent);
        if (!cursor.hasNext()) {
            return version(streamName) > 0 ? Optional.of(Stream.empty()) : Optional.empty();
        } else {
            return Optional.of(Collections.stream(cursor).map(dbObject -> (Event) mapper.mapToObject(dbObject)));
        }
    }

    protected abstract Iterator<T> iteratorSince(String streamName, long lastReceivedEvent);

}
