package ddd.repository.mongodb;

import com.github.fakemongo.Fongo;
import ddd.repository.AbstractHouseRepositorySpec;
import ddd.repository.UnitOfWork;
import ddd.repository.example.domain.ImmutableHouse;
import java.util.Optional;

public class MongoDbRepositoryWithUowSpec extends AbstractHouseRepositorySpec<ImmutableHouse, MongoDbRepository<ImmutableHouse, String>> {
    public MongoDbRepositoryWithUowSpec() {
        super(
                new MongoDbRepository<ImmutableHouse, String>(new Fongo("Mongo").getDB("Mongo"), Optional.of(uowSupplier())){},
                new ImmutableHouse("100500 Awesome str., Chicago, USA", 100500, "Alexey Balchunas")
        );
    }
}
