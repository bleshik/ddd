package ddd.repository.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoCommandException;
import ddd.repository.AbstractRepository;
import ddd.repository.IdentifiedEntity;
import ddd.repository.PersistenceOrientedRepository;
import ddd.repository.UnitOfWork;
import ddd.repository.exception.OptimisticLockingException;
import eventstore.util.DbObjectMapper;
import eventstore.util.RuntimeGeneric;
import eventstore.util.collection.Collections;
import eventstore.util.mongodb.GsonMongoDbObjectMapper;
import eventstore.util.mongodb.Migration;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Simple MongoDB based repository. It just does the POJO mapping and puts it into the DB.
 */
@SuppressWarnings("unchecked")
public abstract class MongoDbRepository<T extends IdentifiedEntity<K>, K>
    extends AbstractRepository<T, K, DBObject, Object> {

    protected DBCollection entityCollection;

    public MongoDbRepository(DBCollection entityCollection, DbObjectMapper<DBObject> mapper, Optional<UnitOfWork> uow) {
        super(mapper, uow);
        init(entityCollection, mapper);
    }

    public MongoDbRepository(DB db, DbObjectMapper<DBObject> mapper, Optional<UnitOfWork> uow) {
        super(mapper, uow);
        init(db.getCollection(((Class<T>) getClassArgument(0)).getSimpleName()), mapper);
    }

    public MongoDbRepository(DB db, Optional<UnitOfWork> uow) {
        this(db, new GsonMongoDbObjectMapper(), uow);
    }

    protected void init(DBCollection entityCollection, DbObjectMapper<DBObject> mapper) {
        this.entityCollection = entityCollection;

        Migration.migrate(() -> {
            entityCollection.createIndex(new BasicDBObject("id", 1), new BasicDBObject("unique", true));
            migrate();
        });
    }

    @Override
    protected Optional<DBObject> doGet(Object id) {
        return Collections.stream(entityCollection.find(new BasicDBObject("id", id)).iterator()).findFirst();
    }

    @Override
    protected Object toDbId(K id) { return id; }

    @Override
    public long size() { flush(); return entityCollection.count(); }

    @Override
    protected DBObject doSave(DBObject dbObject, Optional<Long> currentVersion) {
        try {
            final Object id = dbObject.get("id");
            currentVersion.ifPresent((version) -> dbObject.put("version", version + 1));
            entityCollection.findAndModify(
                    currentVersion.map(
                        (v) -> new BasicDBObject("version", v).append("id", id)
                    ).orElse(
                        new BasicDBObject("id", id)
                    ),
                    null,
                    null,
                    false,
                    dbObject,
                    false,
                    true
            );
            return dbObject;
        } catch(DuplicateKeyException e) {
            if (e.getErrorCode() == 11000) {
                throw new OptimisticLockingException("The document " + dbObject + " was already changed.", e);
            } else {
                throw e;
            }
        }
    }

    @Override
    protected boolean doRemove(Object id) {
        return entityCollection.remove(new BasicDBObject("id", id)).getN() > 0;
    }

    protected void migrate() {}
}
