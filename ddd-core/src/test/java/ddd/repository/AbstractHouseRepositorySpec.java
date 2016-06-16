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
public abstract class AbstractHouseRepositorySpec<T extends House, R extends PersistenceOrientedRepository<T, String>> {
    protected final T awesomeHouse;
    protected final R houseRepository;

    protected AbstractHouseRepositorySpec(R houseRepository, T awesomeHouse) {
        this.houseRepository = houseRepository;
        this.awesomeHouse    = awesomeHouse;
    }

    @Test
    public void remove() {
        TemporalRepository<T, String> temporalHouseRepository = null;
        if (houseRepository instanceof TemporalRepository) {
            temporalHouseRepository = (TemporalRepository<T, String>) houseRepository;
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
        T house = houseRepository.save(awesomeHouse);
        assertEquals(1, houseRepository.size());
        assertEquals(house, houseRepository.get(house.getAddress()).get());

        house = (T) house.buy("Stepan Stepanov");
        house = houseRepository.save(house);
        assertEquals("Stepan Stepanov", houseRepository.get(house.getAddress()).get().getOwner());

        house = (T) house.destroy();
        house = houseRepository.save(house);
        assertTrue(houseRepository.get(house.getAddress()).get().isDestroyed());
        assertEquals("Stepan Stepanov", houseRepository.get(house.getAddress()).get().getOwner());
    }

    @Test(expected = OptimisticLockingException.class)
    public void save() {
        houseRepository.save((T) awesomeHouse.destroy());
        houseRepository.save((T) awesomeHouse.buy("Stepan Stepanov"));
    }

}
