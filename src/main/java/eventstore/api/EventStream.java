package eventstore.api;

import java.util.stream.Stream;
import java.util.List;
import java.util.Collections;

public class EventStream {
  public final long version;
  public final Stream<Event> events;

  public EventStream(long version, Stream<Event> events) {
    this.version = version;
    this.events  = events;
  }

  public static EventStream EMPTY = new EventStream(0, Stream.empty());
}
