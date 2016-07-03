package ddd.eventstore;

import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import ddd.util.CloneWith;

/**
 * Base class for all events.
 */
public abstract class Event<T extends Event> implements CloneWith<T> {
    protected long occurredOn = -1L;
    protected long streamVersion = -1L;

    public long getOccurredOn() { return occurredOn; }

    public long getStreamVersion() { return streamVersion; }

    /**
     * Return same event, but with the version of the corresponding stream set.
     * This method is intended to be used by the event store implementations.
     * @param streamVersion version of the corresponding event stream 
     * @return same event, but with the version of the corresponding stream set
     */
    public T occurred(long streamVersion) {
        return cloneWith(e -> {
            e.occurredOn    = System.currentTimeMillis();
            e.streamVersion = streamVersion;
        });
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, "occurredOn", "streamVersion");
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, "occurredOn", "streamVersion");
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
