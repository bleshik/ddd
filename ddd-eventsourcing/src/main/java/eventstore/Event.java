package eventstore;

import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import util.CloneWith;

public abstract class Event<T extends Event> implements CloneWith<T> {
    protected long occurredOn = -1L;

    public long getOccurredOn() { return occurredOn; }

    public T occurred() {
        return cloneWith(e -> e.occurredOn = System.currentTimeMillis());
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, "occurredOn");
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, "occurredOn");
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
