package ddd.repository;

import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.builder.ToStringBuilder;
import ddd.repository.IdentifiedEntity;

public abstract class AbstractIdentifiedEntity<K> implements IdentifiedEntity<K> {

    private K id;

    public AbstractIdentifiedEntity(K id) {
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
