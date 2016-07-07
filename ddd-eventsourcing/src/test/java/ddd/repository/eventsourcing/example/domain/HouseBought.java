package ddd.repository.eventsourcing.example.domain;

import eventstore.Event;

public class HouseBought {
	public final String newOwner;
    public HouseBought(String newOwner) { 
		this.newOwner = newOwner;
    }
}
