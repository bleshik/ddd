package ddd.repository.example.domain;

import ddd.repository.IdentifiedEntity;

public interface House extends IdentifiedEntity<String> {
    default String getId() { return getAddress(); }
    String getAddress();
    long getPriceInCents();
    String getOwner();
    boolean isDestroyed();
    House buy(String newOwner);
    House destroy();
}
