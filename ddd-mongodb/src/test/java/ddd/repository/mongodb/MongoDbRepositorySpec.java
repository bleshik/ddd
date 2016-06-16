package ddd.repository.mongodb;

import com.github.fakemongo.Fongo;
import ddd.repository.example.domain.ImmutableHouse;
import ddd.repository.AbstractHouseRepositorySpec;

public class MongoDbRepositorySpec extends AbstractHouseRepositorySpec<ImmutableHouse, MongoDbRepository<ImmutableHouse, String>> {
    public MongoDbRepositorySpec() {
        super(
                new MongoDbRepository<ImmutableHouse, String>(new Fongo("Mongo").getDB("Mongo")){},
                new ImmutableHouse("100500 Awesome str., Chicago, USA", 100500, "Alexey Balchunas")
        );
    }
}
