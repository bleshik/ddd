package ddd.repository;

import ddd.repository.TemporalRepository;
import ddd.repository.PersistenceOrientedRepository;
import org.junit.runners.JUnit4;
import org.junit.runner.RunWith;
import org.junit.Test;
import java.util.Optional;
import ddd.repository.example.domain.House;
import ddd.repository.exception.OptimisticLockingException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

@RunWith(JUnit4.class)
@SuppressWarnings("unchecked")
public abstract class AbstractHouseRepositorySpec {
    protected final House awesomeHouse;
    protected final PersistenceOrientedRepository<House, String> houseRepository;

    protected AbstractHouseRepositorySpec(PersistenceOrientedRepository<? extends House, String> houseRepository, House awesomeHouse) {
        this.houseRepository = (PersistenceOrientedRepository<House, String>) houseRepository;
        this.awesomeHouse    = awesomeHouse;
    }

    @Test
    public void remove() {
        TemporalRepository<House, String> temporalHouseRepository = null;
        if (houseRepository instanceof TemporalRepository) {
            temporalHouseRepository = (TemporalRepository<House, String>) houseRepository;
        }
        if (temporalHouseRepository != null) {
            assertFalse(temporalHouseRepository.contained(awesomeHouse.getAddress()));
        }

        houseRepository.save(awesomeHouse);
        assertTrue(houseRepository.remove(awesomeHouse.getAddress()));
        assertEquals(Optional.empty(), houseRepository.get(awesomeHouse.getAddress()));

        if (temporalHouseRepository != null) {
            assertFalse(temporalHouseRepository.contains(awesomeHouse.getAddress()));
            assertTrue(temporalHouseRepository.contained(awesomeHouse.getAddress()));
        }
    }

    @Test
    public void get() {
        House house = houseRepository.save(awesomeHouse);
        assertEquals(1, houseRepository.size());
        assertEquals(house, houseRepository.get(house.getAddress()).get());

        house = house.buy("Stepan Stepanov");
        house = houseRepository.save(house);
        assertEquals("Stepan Stepanov", houseRepository.get(house.getAddress()).get().getOwner());

        house = house.destroy();
        house = houseRepository.save(house);
        assertTrue(houseRepository.get(house.getAddress()).get().isDestroyed());
        assertEquals("Stepan Stepanov", houseRepository.get(house.getAddress()).get().getOwner());
    }

    @Test(expected = OptimisticLockingException.class)
    public void save() {
        houseRepository.save(awesomeHouse.destroy());
        houseRepository.save(awesomeHouse.buy("Stepan Stepanov"));
    }

}
