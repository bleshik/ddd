package ddd.repository;

import java.util.*;

public class UnitOfWork implements AutoCloseable {
    private boolean flushing = false;
    // repositories put entities they've read  here
    private final Map<PersistenceOrientedRepository, Map> cache = new IdentityHashMap<>();
    // put last version of all changed entities (except deleted ones)
    private final Map<PersistenceOrientedRepository, Map> changed = new IdentityHashMap<>();
    // all deleted entities
    private final Map<PersistenceOrientedRepository, Set> removed = new IdentityHashMap<>();

    public void begin() {
        clear();
    }

    public <T extends IdentifiedEntity<K>, K, R extends PersistenceOrientedRepository<T, K>> Optional<T> get(R r, K id) {
        return Optional.ofNullable(cache.get(r)).flatMap(map -> Optional.ofNullable((T) map.get(id)));
    }

    public <T extends IdentifiedEntity<K>, K, R extends PersistenceOrientedRepository<T, K>> boolean isRemoved(R r, K id) {
        return Optional.ofNullable(removed.get(r)).map(set -> set.contains(id)).orElse(false);
    }

    public <T extends IdentifiedEntity<K>, K, R extends PersistenceOrientedRepository<T, K>> void register(R r, T entity) {
        // okay, someone registers an entity, but we know it was changed already and not flushed, seems like a bug
        if (Optional.ofNullable(changed.get(r)).map(map -> map.containsKey(entity.getId())).orElse(false)) {
            throw new IllegalStateException("Entity (" + r +") " + entity.getId() + " was changed already");
        }
        putToCache(r, entity);
    }

    public <T extends IdentifiedEntity<K>, K, R extends PersistenceOrientedRepository<T, K>> void changed(R r, T entity) {
        if (!changed.containsKey(r)) {
            changed.put(r, new HashMap<>());
        }
        changed.get(r).put(entity.getId(), entity);
        putToCache(r, entity);
    }

    private <T extends IdentifiedEntity<K>, K, R extends PersistenceOrientedRepository<T, K>> void putToCache(R r, T entity) {
        if (!cache.containsKey(r)) {
            cache.put(r, new HashMap<>());
        }
        cache.get(r).put(entity.getId(), entity);
        Optional.ofNullable(removed.get(r)).ifPresent(set -> set.remove(entity.getId()));
    }

    public <T extends IdentifiedEntity<K>, K, R extends PersistenceOrientedRepository<T, K>> void removed(R r, K id) {
        if (!removed.containsKey(r)) {
            removed.put(r, new HashSet<>());
        }
        if (removed.get(r).contains(id)) {
            throw new IllegalStateException("Entity (" + r +") " + id + " was removed already");
        }
        Optional.ofNullable(cache.get(r)).ifPresent(map -> map.remove(id));
        Optional.ofNullable(changed.get(r)).ifPresent(map -> map.remove(id));
        removed.get(r).add(id);
    }

    public boolean isFlushing() {
        return flushing;
    }

    public void flush() {
        doFlush(Optional.empty());
    }

    public <T extends IdentifiedEntity<K>, K, R extends PersistenceOrientedRepository<T, K>> void flush(R r) {
        doFlush(Optional.of(r));
    }

    private void doFlush(Optional<? extends PersistenceOrientedRepository> r) {
        try {
            flushing = true;
            for (PersistenceOrientedRepository repository : r.map(Collections::singleton).orElse(changed.keySet())) {
                Optional.ofNullable(changed.get(repository)).map(Map::values).ifPresent(repository::saveAll);
            }
            for (PersistenceOrientedRepository repository : r.map(Collections::singleton).orElse(removed.keySet())) {
                Optional.ofNullable(removed.get(repository)).ifPresent(repository::removeAll);
            }
            clear(r);
        } finally {
            flushing = false;
        }
    }

    public void end() {
        flush();
        clear();
    }

    private void clear(Optional<? extends PersistenceOrientedRepository> r) {
        if (r.isPresent()) {
            cache.remove(r.get());
            changed.remove(r.get());
            removed.remove(r.get());
        } else {
            cache.clear();
            changed.clear();
            removed.clear();
        }
    }

    private void clear() {
        clear(Optional.empty());
    }

    @Override
    public void close() {
        end();
    }
}
