package eventstore.impl;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.Test;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ConcurrentModificationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import eventstore.Event;
import eventstore.EventStore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
public abstract class AbstractEventStoreSpec {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final EventStore eventStore;

    protected AbstractEventStoreSpec(EventStore eventStore) { 
		this.eventStore = eventStore;
    }

    @Test
    public void threadSafe() {
        stressTest(new InMemoryEventStore(), 10, 100);
    }

    @Test
    public void append() {
        eventStore.append("Test", new DummyEvent());
        assertEquals(1L, eventStore.version("Test"));
        assertEquals(new DummyEvent(), eventStore.stream("Test").get().findAny().get());
    }

    protected void stressTest(EventStore eventStore, int concurrencyLevel, int eventsPerThread) {
        try {
            ExecutorService pool = Executors.newFixedThreadPool(concurrencyLevel);
            Collection callables = IntStream.rangeClosed(1, concurrencyLevel).boxed().map((i) -> {
                return (Callable) (() -> (int) IntStream.rangeClosed(1, eventsPerThread).map((j) -> {
                    boolean success = false;
                    int exceptionAmount = 0;
                    while(!success) {
                        try {
                            eventStore.append("stream", new DummyEvent());
                            success = true;
                        } catch (ConcurrentModificationException e) {
                            ++exceptionAmount;
                        }
                    }
                    return exceptionAmount;
                }).sum());
            }).collect(Collectors.toList());
            int totalExceptionsAmount = 0;
            for (Future f : pool.invokeAll((Collection<Callable<Integer>>)callables)) {
                totalExceptionsAmount += (Integer) f.get();
            }
            pool.shutdown();
            try {
                if (pool.awaitTermination(30000, TimeUnit.MILLISECONDS)) {
                    assertEquals(eventStore.version("stream"), concurrencyLevel * eventsPerThread);
                } else {
                    fail();
                }
            } finally {
                logger.info("ConcurrentModificationException occurred " + totalExceptionsAmount + " times");
            }
        } catch (InterruptedException|ExecutionException e) {
            fail();
        }
    }
}
class DummyEvent extends Event {}
