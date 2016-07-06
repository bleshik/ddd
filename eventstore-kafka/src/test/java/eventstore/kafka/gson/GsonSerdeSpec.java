package eventstore.kafka.gson;

import com.google.gson.reflect.TypeToken;
import eventstore.Event;
import eventstore.PayloadEvent;
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
        final PayloadEvent event = new PayloadEvent(42);
        assertEquals(event, serde.deserializer().deserialize("Test", serde.serializer().serialize("Test", event)));
    }
}
