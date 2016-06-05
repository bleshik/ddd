package repository.eventsourcing.example.domain

import eventstore.api.Event

case class HouseDestroyed() extends Event
