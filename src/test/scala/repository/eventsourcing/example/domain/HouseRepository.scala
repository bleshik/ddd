package repository.eventsourcing.example.domain

import repository.eventsourcing.TemporalPersistenceOrientedRepository

trait HouseRepository extends TemporalPersistenceOrientedRepository[House, String] {

}
