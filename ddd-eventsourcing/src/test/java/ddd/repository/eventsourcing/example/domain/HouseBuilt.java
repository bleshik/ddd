package ddd.repository.eventsourcing.example.domain;

import ddd.repository.eventsourcing.InitialEvent;

public class HouseBuilt extends InitialEvent<EventSourcedHouse> {

    public final String address;
    public final long priceInCents;
    public final String owner;

    public HouseBuilt(String address, long priceInCents, String owner) { 
        this.address = address;
        this.priceInCents = priceInCents;
        this.owner = owner;
    }

    public EventSourcedHouse initializedObject() { return new EventSourcedHouse(address, priceInCents, owner); }
}
