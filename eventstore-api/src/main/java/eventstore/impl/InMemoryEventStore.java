package eventstore.impl;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.ConcurrentModificationException;

import eventstore.Event;
import eventstore.EventStore;

import static java.util.stream.Collectors.toList;

/**
 * In-memory storage of events.
 */
public class InMemoryEventStore implements EventStore {

    private final ConcurrentMap<String, List<Event>> streams;

    public InMemoryEventStore() {
        this(new ConcurrentHashMap<>());
    }

    public InMemoryEventStore(ConcurrentMap<String, List<Event>> streams) {
        this.streams = streams;
    }

    @Override
    public Optional<Stream<Event>> streamSince(String streamName, long lastReceivedEvent) {
        List<Event> allEvents = Optional.ofNullable(streams.get(streamName)).orElse(Collections.emptyList());
        if (lastReceivedEvent > allEvents.size()) {
            throw new IllegalArgumentException("Invalid version " + 
                    lastReceivedEvent + " of stream " + streamName + "  " +
                    allEvents
                    );
        }
        if (!allEvents.iterator().hasNext()) {
            return Optional.empty();
        } else {
            List<Event> newEvents = lastReceivedEvent >= 0 && lastReceivedEvent <= allEvents.size() ?
                allEvents.subList((int) lastReceivedEvent, allEvents.size()) :
                allEvents;
            return Optional.of(newEvents.stream());
        }
    }

    @Override
    public void append(String streamName, long currentVersion, List<? extends Event> newEvents) {
        // one global lock is enough for tests
        synchronized(streams) {
            List<Event> curEvents = Optional.ofNullable(streams.get(streamName)).orElse(Collections.emptyList());
            if (version(streamName) != currentVersion) {
                throw new ConcurrentModificationException();
            }
            streams.put(streamName, new ArrayList<Event>() {{
                addAll(curEvents);
                for (int i = 0, n = newEvents.size(); i < n; ++i) {
                    add(newEvents.get(i).occurred(currentVersion + i + 1));
                }
            }});
        }
    }

    @Override
    public long size() {
        return streams.size();
    }

    @Override
    public long version(String streamName) {
        return Optional.ofNullable(streams.get(streamName)).map((s) -> (long) s.size()).orElse(0L);
    }
}
