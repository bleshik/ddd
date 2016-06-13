package ddd.repository.eventsourcing.example.domain;

import ddd.repository.eventsourcing.TemporalPersistenceOrientedRepository;

public interface HouseRepository extends TemporalPersistenceOrientedRepository<House, String> { }
