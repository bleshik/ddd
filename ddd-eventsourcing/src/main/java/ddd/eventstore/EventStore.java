package ddd.eventstore;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Storage for events. It consists of event streams, you can create a stream by simply appending events to it.
 */
public interface EventStore extends AutoCloseable {
    /**
     * Returns an {@link Optional} describing the event stream after the specified event number,
     * or an empty {@code Optional} if the stream does not exists.
     * @param streamName name of the stream whose events are to be retrieved
     * @param after event number after which the stream starts from
     * @return an {@link Optional} describing the event stream after the specified event number,
     * or an empty {@code Optional} if the stream does not exists
     */
    Optional<Stream<Event>> streamSince(String streamName, long after);
    /**
     * Returns an {@link Optional} describing the stream with all its events,
     * or an empty {@code Optional} if the stream does not exists.
     * @param streamName name of the stream whose events are to be retrieved
     * @return an {@link Optional} describing the stream with all its events,
     * or an empty {@code Optional} if the stream does not exists.
     */
    default Optional<Stream<Event>> stream(String streamName) { return streamSince(streamName, 0); }
    /**
     * Atomically appends new events, checking the current version the caller passed. If the caller's version != the
     * actual stream version, {@link ConcurrentModificationException} will be thrown.
     * @param streamName name of the stream is to be appended to
     * @param currentVersion current caller's version
     * @param events list of events is to be appended
     * @throws ConcurrentModificationException if the caller's version != the actual stream version
     */
    void append(String streamName, long currentVersion, List<? extends Event> events);
    /**
     * Atomically appends new events.
     * @param streamName name of the stream is to be appended to
     * @param events list of events is to be appended
     */
    default void append(String streamName, List<? extends Event> events) {
        while (true) {
            try {
                append(streamName, version(streamName), events);
                break;
            } catch (ConcurrentModificationException e) { /*ignore*/ }
        }
    }
    /**
     * Atomically appends a new event, checking the current version the caller passed. If the caller's version != the
     * actual stream version, {@link ConcurrentModificationException} will be thrown.
     * @param streamName name of the stream is to be appended to
     * @param currentVersion current caller's version
     * @param event event is to be appended
     * @throws ConcurrentModificationException if the caller's version != the actual stream version
     */
    default void append(String streamName, long currentVersion, Event event) { append(streamName, currentVersion, Collections.singletonList(event)); }
    /**
     * Atomically appends a new event.
     * @param streamName name of the stream is to be appended to
     * @param event event is to be appended
     */
    default void append(String streamName, Event event) { append(streamName, Collections.singletonList(event)); }
    /**
     * Returns the current stream version/event number/size.
     * @param streamName name of the stream whose version is to be retrieved
     */
    long version(String streamName);
    /**
     * Returns <tt>true</tt> if this event store contains the stream with the specified name.
     * @param streamName name of the stream whose presence in this collection is to be tested
     */
    default boolean contains(String streamName) { return stream(streamName).map((s) -> s.iterator().hasNext()).orElse(false); }
    /**
     * Returns the number of streams in this event store.
     * @return the number of streams in this event store 
     */
    long size();
    /**
     * Frees all the store's resources.
     */
    default void close() {}
}
