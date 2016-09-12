package ddd.repository.dynamodb;

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
import ddd.repository.AbstractRepository;
import ddd.repository.IdentifiedEntity;
import ddd.repository.PersistenceOrientedRepository;
import ddd.repository.exception.OptimisticLockingException;
import eventstore.util.DbObjectMapper;
import eventstore.util.RuntimeGeneric;
import eventstore.util.collection.Collections;
import eventstore.util.dynamodb.ExtendedTable;
import eventstore.util.dynamodb.GsonDynamoDbObjectMapper;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * Simple DynamoDB based repository. It just does the POJO mapping and puts it into the DB.
 */
@SuppressWarnings("unchecked")
public abstract class DynamoDbRepository<T extends IdentifiedEntity<K>, K>
    extends AbstractRepository<T, K, Item, PrimaryKey> {

    protected final ExtendedTable table;
    protected final String tableName;

    public DynamoDbRepository(
            AmazonDynamoDB client,
            long readCapacityUnits,
            long writeCapacityUnits) {
        super(new GsonDynamoDbObjectMapper());
        this.tableName = getClassArgument(0).getSimpleName();
        this.table     = new ExtendedTable(client, tableName, "id", getClassArgument(1), readCapacityUnits, writeCapacityUnits);
    }

    public DynamoDbRepository(
            AmazonDynamoDB client,
            String tableName,
            long readCapacityUnits,
            long writeCapacityUnits) {
        super(new GsonDynamoDbObjectMapper());
        this.table     = new ExtendedTable(client, tableName, "id", getClassArgument(1), readCapacityUnits, writeCapacityUnits);
        this.tableName = tableName;
    }

    public DynamoDbRepository(Table table, DbObjectMapper<Item> mapper) {
        super(mapper);
        this.table     = new ExtendedTable(table);
        this.tableName = table.getTableName();
    }

    @Override
    protected PrimaryKey toDbId(K id) { return new PrimaryKey("id", id); }

    @Override
    protected Optional<Item> doGet(PrimaryKey id) {
        return Optional.ofNullable(table.getItemOutcome(id).getItem());
    }

    @Override
    public long size() { return table.count(); }

    @Override
    protected Item doSave(Item dbObject, Optional<Long> currentVersion) {
        try {
            currentVersion.ifPresent((version) -> dbObject.withLong("version", version + 1));
            PutItemSpec spec = new PutItemSpec().withItem(dbObject);
            table.putItem(
                currentVersion.map(
                    (v) -> v == 0 ?
                        spec.withExpected(new Expected("id").notExist()) :
                        spec.withExpected(new Expected("version").eq(v))
                ).orElse(spec)
            );
            return dbObject;
        } catch (ConditionalCheckFailedException e) {
            throw new OptimisticLockingException(
                    "Failed to put item " + dbObject + " into the table " + tableName,
                    e
            );
        }
    }

    @Override
    protected boolean doRemove(PrimaryKey id) {
        return table.deleteAndCheck(id);
    }
}
