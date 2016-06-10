package repository.eventsourcing.example.domain;

import repository.eventsourcing.TemporalPersistenceOrientedRepository;

public interface HouseRepository extends TemporalPersistenceOrientedRepository<House, String> { }
