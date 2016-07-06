package eventstore;

import eventstore.Event;

/**
 * Simple event holding a single data (payload) object. This is used for wrapping any POJO into an {@link Event}.
 */
public class PayloadEvent<T> extends Event<PayloadEvent> {
	public final T payload;
    public PayloadEvent(T payload) { 
		this.payload = payload;
    }
}
