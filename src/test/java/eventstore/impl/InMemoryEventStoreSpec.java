package eventstore.impl;

import org.junit.runners.JUnit4;
import org.junit.runner.RunWith;
import org.junit.Test;

@RunWith(JUnit4.class)
public class InMemoryEventStoreSpec extends AbstractEventStoreSpec {
    @Test
    public void threadSafe() {
        stressTest(new InMemoryEventStore(), 10, 100);
    }
}
