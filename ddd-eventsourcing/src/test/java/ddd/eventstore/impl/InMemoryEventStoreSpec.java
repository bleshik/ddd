package ddd.eventstore.impl;

public class InMemoryEventStoreSpec extends AbstractEventStoreSpec {
    public InMemoryEventStoreSpec() { super(new InMemoryEventStore()); }
}
