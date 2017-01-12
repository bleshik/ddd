package ddd.repository;

import java.util.*;

public class UnitOfWork implements AutoCloseable {
    private boolean flushing = false;
    // repositories put entities they've read  here
    private final Map<Class<? extends PersistenceOrientedRepository>, Map> cache = new IdentityHashMap<>();
    // put last version of all changed entities (except deleted ones)
    private final Map<Class<? extends PersistenceOrientedRepository>, Map> changed = new IdentityHashMap<>();
    // all deleted entities
    private final Map<Class<? extends PersistenceOrientedRepository>, Set> removed = new IdentityHashMap<>();
    private final Map<Class<? extends PersistenceOrientedRepository>, PersistenceOrientedRepository> repositories = new HashMap<>();

    public void begin() {
        clear();
    }

    public <T extends IdentifiedEntity<K>, K, R extends PersistenceOrientedRepository<T, K>> Optional<T> get(R r, K id) {
        return Optional.ofNullable(cache.get(r.getClass())).flatMap(map -> Optional.ofNullable((T) map.get(id)));
    }

    public <T extends IdentifiedEntity<K>, K, R extends PersistenceOrientedRepository<T, K>> boolean isRemoved(R r, K id) {
        return Optional.ofNullable(removed.get(r.getClass())).map(set -> set.contains(id)).orElse(false);
    }

    public <T extends IdentifiedEntity<K>, K, R extends PersistenceOrientedRepository<T, K>> void register(R r, T entity) {
        Class<? extends PersistenceOrientedRepository> rClass = r.getClass();
        // okay, someone registers an entity, but we know it was changed already and not flushed, seems like a bug
        if (Optional.ofNullable(changed.get(rClass)).map(map -> map.containsKey(entity.getId())).orElse(false)) {
            throw new IllegalStateException("Entity (" + rClass +") " + entity.getId() + " was changed already");
        }
        putToCache(r, entity);
    }

    public <T extends IdentifiedEntity<K>, K, R extends PersistenceOrientedRepository<T, K>> void changed(R r, T entity) {
        if (get(r, entity.getId()).map(e -> e == entity).orElse(false)) {
            return;
        }
        Class<? extends PersistenceOrientedRepository> rClass = r.getClass();
        if (!changed.containsKey(rClass)) {
            changed.put(rClass, new HashMap<>());
        }
        repositories.putIfAbsent(rClass, r);
        changed.get(rClass).put(entity.getId(), entity);
        putToCache(r, entity);
    }

    private <T extends IdentifiedEntity<K>, K, R extends PersistenceOrientedRepository<T, K>> void putToCache(R r, T entity) {
        Class<? extends PersistenceOrientedRepository> rClass = r.getClass();
        if (!cache.containsKey(rClass)) {
            cache.put(rClass, new HashMap<>());
        }
        repositories.putIfAbsent(rClass, r);
        cache.get(rClass).put(entity.getId(), entity);
        Optional.ofNullable(removed.get(rClass)).ifPresent(set -> set.remove(entity.getId()));
    }

    public <T extends IdentifiedEntity<K>, K, R extends PersistenceOrientedRepository<T, K>> void removed(R r, K id) {
        Class<? extends PersistenceOrientedRepository> rClass = r.getClass();
        if (!removed.containsKey(rClass)) {
            removed.put(rClass, new HashSet<>());
        }
        if (removed.get(rClass).contains(id)) {
            throw new IllegalStateException("Entity (" + r +") " + id + " was removed already");
        }
        repositories.putIfAbsent(rClass, r);
        Optional.ofNullable(cache.get(rClass)).ifPresent(map -> map.remove(id));
        Optional.ofNullable(changed.get(rClass)).ifPresent(map -> map.remove(id));
        removed.get(rClass).add(id);
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
            Collection<? extends PersistenceOrientedRepository> repos = r.map(repo ->
                    (Collection<PersistenceOrientedRepository>) Collections.singleton(repo)
            ).orElse(repositories.values());
            for (PersistenceOrientedRepository repository : repos) {
                Class<? extends PersistenceOrientedRepository> rClass = repository.getClass();
                Optional.ofNullable(changed.get(rClass)).map(Map::values).ifPresent(repository::saveAll);
                Optional.ofNullable(removed.get(rClass)).ifPresent(repository::removeAll);
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
            cache.remove(r.get().getClass());
            changed.remove(r.get().getClass());
            removed.remove(r.get().getClass());
            repositories.remove(r.get().getClass());
        } else {
            cache.clear();
            changed.clear();
            removed.clear();
            repositories.clear();
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
