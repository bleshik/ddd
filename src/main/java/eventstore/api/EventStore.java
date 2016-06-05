package eventstore.api;

import java.util.Collections;
import java.util.Optional;
import java.util.List;
import java.util.Set;

public interface EventStore {
    void close();
    Optional<EventStream> streamSince(String streamName, long lastReceivedEvent);
    default Optional<EventStream> stream(String streamName) { return streamSince(streamName, 0); }

    void append(String streamName, long currentVersion, List<? extends Event> events);
    default void append(String streamName, List<? extends Event> events) { append(streamName, version(streamName), events); }

    default void append(String streamName, long currentVersion, Event event) { append(streamName, currentVersion, Collections.singletonList(event)); }
    default void append(String streamName, Event event) { append(streamName, version(streamName), event); }

    long version(String streamName);
    default boolean contains(String streamName) { return stream(streamName).map((s) -> s.events.iterator().hasNext()).orElse(false); }
    default long size() { return streamNames().size(); }
    Set<String> streamNames();
}
