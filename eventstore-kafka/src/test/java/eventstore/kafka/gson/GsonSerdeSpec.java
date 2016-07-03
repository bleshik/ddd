package eventstore.kafka.gson;

import eventstore.Event;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
public class GsonSerdeSpec {

    private final GsonSerde<Event> serde = new GsonSerde<Event>(){{}};

    @Test
    public void append() {
        final DummyEvent event = new DummyEvent(42);
        assertEquals(event, serde.deserializer().deserialize("Test", serde.serializer().serialize("Test", event)));
    }
}

class DummyEvent extends Event {
    final long payload;

    DummyEvent(long payload) { 
		this.payload = payload;
    }
}
