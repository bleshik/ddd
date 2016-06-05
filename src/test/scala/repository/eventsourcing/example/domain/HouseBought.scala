package repository.eventsourcing.example.domain

import eventstore.api.Event

case class HouseBought(newOwner: String) extends Event {}
