package ddd.repository.eventsourcing.example.domain;

import ddd.repository.PersistenceOrientedRepository;
import ddd.repository.TemporalRepository;

public interface HouseRepository extends TemporalRepository<House, String>, PersistenceOrientedRepository<House, String> { }
