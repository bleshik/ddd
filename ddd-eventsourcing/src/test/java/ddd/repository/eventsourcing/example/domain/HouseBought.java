package ddd.repository.eventsourcing.example.domain;

import ddd.eventstore.Event;

public class HouseBought extends Event {
	public final String newOwner;
    public HouseBought(String newOwner) { 
		this.newOwner = newOwner;
    }
}
