package ddd.eventstore.impl;

import ddd.eventstore.Event;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryEventStoreSpec extends AbstractEventStoreSpec {
    public InMemoryEventStoreSpec() { super(withObject(new ConcurrentHashMap<String, List<Event>>(), (streams) -> (() -> new InMemoryEventStore(streams)))); }
}
