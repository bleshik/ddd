package repository.eventsourcing.example.domain;

import repository.eventsourcing.IdentifiedEventSourcedEntity;

public class House extends IdentifiedEventSourcedEntity<House, String> {

    public final String address;
    public final long priceInCents;
    public final String owner;
    public final boolean destroyed;

    public House(String address, long priceInCents, String owner) { 
        this(address, priceInCents, owner, false);
    }

    private House(String address, long priceInCents, String owner, boolean destroyed) { 
        super(address, new HouseBuilt(address, priceInCents, owner));
        this.address      = address;
        this.priceInCents = priceInCents;
        this.owner        = owner;
        this.destroyed    = destroyed;
    }

    public House buy(String newOwner) {
        return apply(new HouseBought(newOwner));
    }

    public House destroy() {
        return apply(new HouseDestroyed());
    }

    protected House when(HouseBought event) {
        if (destroyed) {
            throw new IllegalStateException("You cannot buy a destroyed house");
        }
        return new House(address, priceInCents, event.newOwner, destroyed);
    }

    protected House when(HouseDestroyed event) {
        return new House(address, priceInCents, owner, true);
    }
}
