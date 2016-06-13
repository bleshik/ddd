package ddd.repository.eventsourcing.example.port.adapter.persistence;

import ddd.repository.eventsourcing.EventSourcedRepository;
import org.junit.runners.JUnit4;
import org.junit.runner.RunWith;
import org.junit.Test;
import java.util.Optional;
import ddd.eventstore.impl.InMemoryEventStore;
import ddd.repository.eventsourcing.example.domain.House;
import ddd.repository.eventsourcing.example.domain.HouseRepository;
import ddd.repository.eventsourcing.EventSourcingConflictException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

@RunWith(JUnit4.class)
public abstract class AbstractHouseRepositorySpec {
    protected final House awesomeHouse = new House("100500 Awesome str., Chicago, USA", 100500, "Alexey Balchunas");
    protected final EventSourcedRepository<House, String> houseRepository;

    protected AbstractHouseRepositorySpec(EventSourcedRepository<House, String> houseRepository) {
        this.houseRepository = houseRepository;
    }

    @Test
    public void remove() {
        assertFalse(houseRepository.contained(awesomeHouse.address));
        houseRepository.save(awesomeHouse);
        assertTrue(houseRepository.remove(awesomeHouse.address));
        assertEquals(Optional.empty(), houseRepository.get(awesomeHouse.address));
        assertFalse(houseRepository.contains(awesomeHouse.address));
        assertTrue(houseRepository.contained(awesomeHouse.address));
    }

    @Test
    public void get() {
        House house = awesomeHouse;
        houseRepository.save(house);
        assertEquals(1, houseRepository.size());
        assertEquals(house, houseRepository.get(house.address).get());

        house = house.buy("Stepan Stepanov");
        houseRepository.save(house);
        assertEquals("Stepan Stepanov", houseRepository.get(house.address).get().owner);

        house = house.destroy();
        houseRepository.save(house);
        assertTrue(houseRepository.get(house.address).get().destroyed);
        assertEquals("Stepan Stepanov", houseRepository.get(house.address).get().owner);
    }

    @Test(expected = EventSourcingConflictException.class)
    public void save() {
        houseRepository.save(awesomeHouse.destroy());
        houseRepository.save(awesomeHouse.buy("Stepan Stepanov"));
    }

}
