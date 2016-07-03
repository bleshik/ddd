package ddd.repository.example.domain;

import ddd.repository.AbstractIdentifiedEntity;

public class ImmutableHouse extends AbstractIdentifiedEntity<String> implements House {

    public final String address;
    public final long priceInCents;
    public final String owner;
    public final boolean destroyed;
    // the version field is required only when you need Optimistic Locking.
    private final long version;

    public ImmutableHouse(String address, long priceInCents, String owner) { 
        this(address, priceInCents, owner, false, 0);
    }

    private ImmutableHouse(String address, long priceInCents, String owner, boolean destroyed, long version) { 
        super(address);
        this.address      = address;
        this.priceInCents = priceInCents;
        this.owner        = owner;
        this.destroyed    = destroyed;
        this.version      = version;
    }

    @Override
    public House buy(String newOwner) {
        return new ImmutableHouse(address, priceInCents, newOwner, destroyed, version);
    }

    @Override
    public House destroy() {
        if (destroyed) {
            throw new IllegalStateException("You cannot buy a destroyed house");
        }
        return new ImmutableHouse(address, priceInCents, owner, true, version);
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
}
