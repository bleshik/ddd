package repository.eventsourcing.example.port.adapter.persistence

import eventstore.api.EventStore
import repository.eventsourcing.EventSourcedRepository
import repository.eventsourcing.example.domain.{House, HouseRepository}

class EventSourcedHouseRepository(override val eventStore: EventStore) extends EventSourcedRepository[House, String](eventStore) with HouseRepository  {

}
