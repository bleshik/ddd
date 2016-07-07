package eventstore;

import org.apache.commons.lang3.builder.EqualsBuilder;
import eventstore.Event;

/**
 * Simple event holding a single data (payload) object. This is used for wrapping any POJO into an {@link Event}.
 */
public class PayloadEvent<T> extends Event<PayloadEvent> {
    public static final ClassValue<Boolean> isEqualsDeclared = new ClassValue<Boolean>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod("equals", Object.class).getDeclaringClass() != Object.class;
            } catch (NoSuchMethodException e) {
                throw new AssertionError("This shouldn't happen");
            }
        }
    };
	public final T payload;
    public PayloadEvent(T payload) { 
		this.payload = payload;
    }
    @Override
    public boolean equals(Object obj) {
        return obj != null &&
            getClass() == obj.getClass() &&
            // if you do not define equals for the payload, use the default field-by-field one
            (isEqualsDeclared.get(payload.getClass()) &&
             new EqualsBuilder().append(this.payload, ((PayloadEvent) obj).payload).isEquals() ||
             !isEqualsDeclared.get(payload.getClass()) &&
             EqualsBuilder.reflectionEquals(this.payload, ((PayloadEvent) obj).payload));
    }
}
