package ddd.repository;

public abstract class VersionedIdentifiedEntity<K> extends AbstractIdentifiedEntity<K> {
    protected final long version;
    public VersionedIdentifiedEntity(K id) {
        super(id);
        version = 0;
    }
}
