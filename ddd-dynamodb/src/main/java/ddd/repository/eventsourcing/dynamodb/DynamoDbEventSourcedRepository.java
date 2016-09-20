package ddd.repository.eventsourcing.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.Expected;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateTableSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import ddd.repository.IdentifiedEntity;
import ddd.repository.eventsourcing.EventSourcedEntity;
import ddd.repository.eventsourcing.EventSourcedRepository;
import eventstore.EventStore;
import eventstore.dynamodb.DynamoDbEventStore;
import eventstore.util.DbObjectMapper;
import eventstore.util.collection.Collections;
import eventstore.util.dynamodb.ExtendedTable;
import eventstore.util.dynamodb.GsonDynamoDbObjectMapper;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * DynamoDB-based event sourced repository. This stores events and snapshots in DynamoDB. Optionally,
 * you can specify other {@link EventStore}.
 */
@SuppressWarnings("unchecked")
public abstract class DynamoDbEventSourcedRepository<T extends EventSourcedEntity<T> & IdentifiedEntity<K>, K> extends EventSourcedRepository<T, K> {

    protected final ExtendedTable table;
    protected final String tableName;
    protected final DbObjectMapper<Item> mapper;
    protected final ProvisionedThroughput provisionedThroughput;

    public DynamoDbEventSourcedRepository(
            AmazonDynamoDB client,
            long readCapacityUnits,
            long writeCapacityUnits) {
        this(client, null, readCapacityUnits, writeCapacityUnits);
    }

    public DynamoDbEventSourcedRepository(
            AmazonDynamoDB client,
            String tableName,
            long readCapacityUnits,
            long writeCapacityUnits) {
        this.tableName = tableName != null ? tableName : getClassArgument(0).getSimpleName();
        this.provisionedThroughput = new ProvisionedThroughput(readCapacityUnits, writeCapacityUnits);
        this.table = new ExtendedTable(
            client,
            this.tableName,
            getAttributes(),
            getKey(),
            provisionedThroughput
        );
        this.mapper = new GsonDynamoDbObjectMapper();
        init(new DynamoDbEventStore(client, this.tableName + "Events", readCapacityUnits, writeCapacityUnits));
    }

    public DynamoDbEventSourcedRepository(EventStore eventStore, Table table, DbObjectMapper<Item> mapper) {
        super(eventStore);
        this.table     = new ExtendedTable(table);
        this.tableName = table.getTableName();
        this.mapper    = mapper;
        this.provisionedThroughput = this.table.exists() ?
            this.table.getProvisionedThroughput() :
            new ProvisionedThroughput(25L, 25L);
    }

    protected List<AttributeDefinition> getAttributes() {
        return Arrays.asList(
            new AttributeDefinition("id", Number.class.isAssignableFrom(getClassArgument(1)) ? "N" : "S")
        );
    }

    protected List<KeySchemaElement> getKey() {
        return Arrays.asList(
            new KeySchemaElement("id", KeyType.HASH)
        );
    }

    protected T deserialize(Item dbObject) {
        return (T) mapper.mapToObject(dbObject);
    }

    protected Item serialize(T entity) {
        return mapper.mapToDbObject(entity);
    }

    @Override
    protected void saveSnapshot(T committed, long unmutatedVersion) {
        try {
            table.putItem(
                new PutItemSpec()
                    .withItem(serialize(committed))
                    .withExpected(unmutatedVersion == 0 ? 
                        new Expected("id").notExist() :
                        new Expected("version").eq(unmutatedVersion)
                    )
            );
        } catch (ConditionalCheckFailedException e) {
            // ignore, because this means that there is already a saved snapshot with higher version
        }
    }

    @Override
    protected boolean removeSnapshot(K id) {
        return table.deleteAndCheck(new PrimaryKey("id", id));
    }

    @Override
    protected Optional<T> snapshot(K id, long before) {
        return Optional.ofNullable(table.getItem(new PrimaryKey("id", id))).map((item) -> deserialize(item));
    }
}
