package eventstore.mongodb;

import com.github.fakemongo.Fongo;
import com.mongodb.DBCollection;
import eventstore.impl.AbstractEventStoreSpec;
import java.util.function.Supplier;

public class MongoDbEventStoreSpec extends AbstractEventStoreSpec {
    public MongoDbEventStoreSpec() {
        super(withObject(new Fongo("Mongo").getDB("Mongo").getCollection("Events"), (dbCollection) -> (() -> new MongoDbEventStore(dbCollection))));
    }
}
