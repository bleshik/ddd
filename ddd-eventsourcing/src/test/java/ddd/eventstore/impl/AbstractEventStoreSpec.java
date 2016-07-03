package ddd.eventstore.impl;

import ddd.eventstore.Event;
import ddd.eventstore.EventStore;
import java.lang.Runnable;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
@SuppressWarnings("unchecked")
public abstract class AbstractEventStoreSpec {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Collection<Supplier<? extends EventStore>> eventStoreSuppliers;
    @Rule public TestName name = new TestName();
    protected static final ThreadLocal<String> currentTest = new ThreadLocal<>();

    protected AbstractEventStoreSpec(Collection<Supplier<? extends EventStore>> eventStoreSuppliers) {
        this.eventStoreSuppliers = eventStoreSuppliers;
    }

    protected AbstractEventStoreSpec(Supplier<? extends EventStore> eventStoreSupplier) { 
        this(Collections.singletonList(eventStoreSupplier));
    }

    @Test
    public void threadSafe() {
        currentTest.set(name.getMethodName());
        eventStoreSuppliers.stream().forEach((eventStoreSupplier) -> {
            try (EventStore eventStore = eventStoreSupplier.get()) {
                stressTest(eventStore, 5, 100);
            }
        });
    }

    @Test
    public void append() {
        currentTest.set(name.getMethodName());
        eventStoreSuppliers.stream().forEach((eventStoreSupplier) -> {
            try (EventStore eventStore = eventStoreSupplier.get()) {
                eventStore.append("stream0", new DummyEvent(41L));
                assertEquals(1L, eventStore.version("stream0"));
                eventStore.append("stream1", new DummyEvent(42L));
                assertEquals(new DummyEvent(41L), eventStore.stream("stream0").get().findAny().get());
                assertEquals(new DummyEvent(42L), eventStore.stream("stream1").get().findAny().get());
            }
        });
    }

    @Test
    public void size() throws Exception {
        currentTest.set(name.getMethodName());
        eventStoreSuppliers.forEach((eventStoreSupplier) -> {
            try(EventStore eventStore0 = eventStoreSupplier.get();
                EventStore eventStore1 = eventStoreSupplier.get()) {
                eventStore0.append("stream0", new DummyEvent(1L));
                waitFor(5000, (() -> assertEquals(1, eventStore0.size())));
                waitFor(5000, (() -> assertEquals(1, eventStore1.size())));
                eventStore1.append("stream1", new DummyEvent(2L));
                waitFor(5000, (() -> assertEquals(2, eventStore0.size())));
                waitFor(5000, (() -> assertEquals(2, eventStore1.size())));
            }
        });
    }

    protected void waitFor(long timeout, Runnable assertion) {
        long timeExpired = 0;
        while ((timeExpired += 100) <= timeout) {
            boolean success = true;
            try {
                assertion.run();
            } catch (Throwable t) {
                success = false;
            }
            if (success) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
        assertion.run();
    }

    protected static <T, V> T withObject(V obj, Function<V,T> fn) {
        return fn.apply(obj);
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
                            eventStore.append("stream", eventStore.version("stream"), new DummyEvent(42L));
                            success = true;
                        } catch (ConcurrentModificationException e) {
                            ++exceptionAmount;
                        } catch (RuntimeException e) {
                            logger.error("Failed to append message", e);
                            throw e;
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
                    if (totalExceptionsAmount == 0) {
                        fail("ConcurrentModificationException occurred 0 times, which is not realistic");
                    } else {
                        assertEquals(concurrencyLevel * eventsPerThread, eventStore.stream("stream").get().count());
                    }
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
class DummyEvent extends Event {
    final long payload;
    public DummyEvent(long payload) { 
		this.payload = payload;
    }
}
