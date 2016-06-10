package repository.eventsourcing.example.port.adapter.persistence;

import org.junit.runners.JUnit4;
import org.junit.runner.RunWith;
import org.junit.Test;
import java.util.Optional;
import eventstore.impl.InMemoryEventStore;
import repository.eventsourcing.example.domain.House;
import repository.eventsourcing.example.domain.HouseRepository;
import repository.eventsourcing.EventSourcingConflictException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

@RunWith(JUnit4.class)
public class HouseRepositorySpec {
    final House awesomeHouse = new House("100500 Awesome str., Chicago, USA", 100500, "Alexey Balchunas");

    @Test
    public void remove() {
        removeHouseScenario(new EventSourcedHouseRepository(new InMemoryEventStore()));
    }

    @Test
    public void get() {
        successHouseScenario(new EventSourcedHouseRepository(new InMemoryEventStore()));
    }

    @Test(expected = EventSourcingConflictException.class)
    public void save() {
        failedHouseScenario(new EventSourcedHouseRepository(new InMemoryEventStore()));
    }

    private void removeHouseScenario(HouseRepository houseRepository) {
        assertFalse(houseRepository.contained(awesomeHouse.address));
        houseRepository.save(awesomeHouse);
        assertTrue(houseRepository.remove(awesomeHouse.address));
        assertEquals(Optional.empty(), houseRepository.get(awesomeHouse.address));
        assertFalse(houseRepository.contains(awesomeHouse.address));
        assertTrue(houseRepository.contained(awesomeHouse.address));
    }

    private void successHouseScenario(HouseRepository houseRepository) {
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

    private void failedHouseScenario(HouseRepository  houseRepository) {
        houseRepository.save(awesomeHouse.destroy());
        houseRepository.save(awesomeHouse.buy("Stepan Stepanov"));
    }

}
