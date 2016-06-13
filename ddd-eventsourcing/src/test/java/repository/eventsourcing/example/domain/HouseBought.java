package repository.eventsourcing.example.domain;

import eventstore.Event;

public class HouseBought extends Event {
	public final String newOwner;
    public HouseBought(String newOwner) { 
		this.newOwner = newOwner;
    }
}
