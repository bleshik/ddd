package eventstore.mongodb;

import com.github.fakemongo.Fongo;
import eventstore.impl.AbstractEventStoreSpec;

public class MongoDbEventStoreSpec extends AbstractEventStoreSpec {
    public MongoDbEventStoreSpec() {
        super(new MongoDbEventStore(new Fongo("Mongo").getDB("Mongo").getCollection("Events")));
    }
}
