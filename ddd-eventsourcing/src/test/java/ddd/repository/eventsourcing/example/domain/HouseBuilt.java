package ddd.repository.eventsourcing.example.domain;

public class HouseBuilt {

    public final String address;
    public final long priceInCents;
    public final String owner;

    public HouseBuilt(String address, long priceInCents, String owner) { 
        this.address = address;
        this.priceInCents = priceInCents;
        this.owner = owner;
    }

}
