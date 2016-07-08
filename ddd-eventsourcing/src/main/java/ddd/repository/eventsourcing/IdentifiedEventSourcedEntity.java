package ddd.repository.eventsourcing;

import ddd.repository.IdentifiedEntity;
import eventstore.Event;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * {@link EventSourcedEntity} with identifier.
 */
public abstract class IdentifiedEventSourcedEntity<T extends EventSourcedEntity<T>, K> extends EventSourcedEntity<T>
    implements IdentifiedEntity<K> {

    private K id;

    public IdentifiedEventSourcedEntity(K id, Event initialEvent) {
        super(initialEvent);
        this.id = id;
    }

    public IdentifiedEventSourcedEntity(K id, Object initialEvent) {
        super(initialEvent);
        this.id = id;
    }


    @Override
    public K getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        return this.getId().equals(((IdentifiedEntity) obj).getId());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
