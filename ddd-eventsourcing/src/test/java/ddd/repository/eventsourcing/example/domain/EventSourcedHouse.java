package ddd.repository.eventsourcing.example.domain;

import ddd.repository.example.domain.House;
import ddd.repository.eventsourcing.IdentifiedEventSourcedEntity;

public class EventSourcedHouse extends IdentifiedEventSourcedEntity<EventSourcedHouse, String> implements House {

    public final String address;
    public final long priceInCents;
    public final String owner;
    public final boolean destroyed;

    public EventSourcedHouse(String address, long priceInCents, String owner) { 
        this(address, priceInCents, owner, false);
    }

    public EventSourcedHouse(HouseBuilt houseBuilt) {
        this(houseBuilt.address, houseBuilt.priceInCents, houseBuilt.owner, false);
    }

    private EventSourcedHouse(String address, long priceInCents, String owner, boolean destroyed) { 
        super(address, new HouseBuilt(address, priceInCents, owner));
        this.address      = address;
        this.priceInCents = priceInCents;
        this.owner        = owner;
        this.destroyed    = destroyed;
    }

    @Override
    public House buy(String newOwner) {
        return apply(new HouseBought(newOwner));
    }

    @Override
    public House destroy() {
        return apply(new HouseDestroyed());
    }

    // Getters are only used to have a common abstract spec based on the interface
    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public long getPriceInCents() {
        return priceInCents;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    protected House when(HouseBought event) {
        if (destroyed) {
            throw new IllegalStateException("You cannot buy a destroyed house");
        }
        return new EventSourcedHouse(address, priceInCents, event.newOwner, destroyed);
    }

    protected House when(HouseDestroyed event) {
        return new EventSourcedHouse(address, priceInCents, owner, true);
    }
}
