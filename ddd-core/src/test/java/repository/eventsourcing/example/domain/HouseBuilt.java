package repository.eventsourcing.example.domain;

import repository.eventsourcing.InitialEvent;

public class HouseBuilt extends InitialEvent<House> {

    public final String address;
    public final long priceInCents;
    public final String owner;

    public HouseBuilt(String address, long priceInCents, String owner) { 
        this.address = address;
        this.priceInCents = priceInCents;
        this.owner = owner;
    }

    public House initializedObject() { return new House(address, priceInCents, owner); }
}
