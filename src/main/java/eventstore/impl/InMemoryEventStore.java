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

import eventstore.api.Event;
import eventstore.api.EventStore;

import static java.util.stream.Collectors.toList;

public class InMemoryEventStore implements EventStore {

  private ConcurrentMap<String, List<Event>> streams = new ConcurrentHashMap<>();

  @Override
  public void close() {}

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
          addAll(newEvents.stream().map(e -> e.occurred()).collect(toList()));
      }});
    }
  }

  @Override
  public Set<String> streamNames() { return streams.keySet(); }

  @Override
  public long version(String streamName) {
    return Optional.ofNullable(streams.get(streamName)).map((s) -> (long) s.size()).orElse(0L);
  }
}
