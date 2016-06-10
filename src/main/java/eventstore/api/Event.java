package eventstore.api;

import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public abstract class Event {
    private long _occurredOn = -1L;

    public long occurredOn() { return _occurredOn; }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, "_occurredOn");
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, "_occurredOn");
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
