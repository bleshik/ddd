package eventstore.util.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.AttributeUpdate;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.internal.InternalUtils;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateTableSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputDescription;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.Select;
import eventstore.util.collection.Collections;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Same {@link Table}, but with additional utility methods.
 */
public class ExtendedTable extends Table {

    protected final AmazonDynamoDB client;

    public ExtendedTable(AmazonDynamoDB client, String tableName) { 
        super(client, tableName);
        this.client = client;
    }

    public ExtendedTable(
            AmazonDynamoDB client,
            String tableName,
            String idName,
            Class<?> idClass,
            long readCapacityUnits,
            long writeCapacityUnits) {
        this(client, tableName, idName, idClass, new ProvisionedThroughput(readCapacityUnits, writeCapacityUnits));
    }

    public ExtendedTable(AmazonDynamoDB client, String tableName, Class<?> idClass, ProvisionedThroughput t) {
        this(client, tableName, "id", idClass, t);
    }

    public ExtendedTable(
            AmazonDynamoDB client,
            String tableName,
            String idName,
            Class<?> idClass,
            ProvisionedThroughput t) {
        this(client, tableName);
        if (!createIfNotExists(idName, idClass, t)) {
            updateTable(t);
        }
    }

    public ExtendedTable(
            AmazonDynamoDB client,
            String tableName,
            List<AttributeDefinition> attributes,
            List<KeySchemaElement> key,
            ProvisionedThroughput t) {
        this(client, tableName);
        if (!createIfNotExists(attributes, key, t)) {
            updateTable(t);
        }
    }

    /**
     * Handy constructor creating the extended table based on an usual table.
     */
    public ExtendedTable(Table table) {
        this(getClient(table), table.getTableName());
    }

    public boolean createIfNotExists(
            List<AttributeDefinition> attributes,
            List<KeySchemaElement> key,
            ProvisionedThroughput t) {
        if (!exists()) {
            client.createTable(new CreateTableRequest(attributes, getTableName(), key, t));
            return true;
        }
        return false;
    }

    public boolean createIfNotExists(String idName, Class<?> idClass, ProvisionedThroughput t) {
        return createIfNotExists(
            Arrays.asList(new AttributeDefinition(idName, Number.class.isAssignableFrom(idClass) ? "N" : "S")),
            Arrays.asList(new KeySchemaElement(idName, KeyType.HASH)),
            t
        );
    }

    public UpdateItemOutcome put(PrimaryKey key, String attribute, Object value) {
        return updateItem(key, new AttributeUpdate(attribute).put(value));
    }

    public UpdateItemOutcome add(PrimaryKey key, String attribute, Object... values) {
        return updateItem(key, new AttributeUpdate(attribute).addElements(values));
    }

    public UpdateItemOutcome add(PrimaryKey key, String attribute, Collection values) {
        return add(key, attribute, values.toArray());
    }

    public Stream<Item> queryStream(QuerySpec q, boolean all) {
        return Collections.stream(
            doGetResult((lastEvaluatedKey) -> query(q.withExclusiveStartKey(lastEvaluatedKey)), all)
        );
    }

    public Stream<Item> queryStream(QuerySpec query) {
        return queryStream(query, false);
    }

    public long count() {
        // TODO: return more up-to-date number
        return describe().getItemCount();
    }

    public Stream<Item> scanStream(ScanSpec s, boolean all) {
        return Collections.stream(
            doGetResult((lastEvaluatedKey) -> scan(s.withExclusiveStartKey(lastEvaluatedKey)), all)
        );
    }

    public Stream<Item> scanStream(ScanSpec scan) {
        return scanStream(scan, false);
    }

    private <T> Iterator<Item> doGetResult(Function<KeyAttribute[], ItemCollection<T>> fetch, boolean all) {
        return new Iterator<Item>() {

            Iterator<Item> items;
            KeyAttribute[] lastEvaluatedKey;

            public boolean hasNext() {
                return items != null ? items.hasNext() : fetch();
            }

            public Item next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return items.next();
            }

            public boolean fetch() {
                ItemCollection<T> fetched = fetch.apply(lastEvaluatedKey);
                items = fetched.iterator();
                if (fetched.getLastLowLevelResult() instanceof ScanOutcome) {
                    lastEvaluatedKey = toKeyAttributes(
                            ((ScanOutcome) fetched.getLastLowLevelResult()).getScanResult().getLastEvaluatedKey());
                } else if (fetched.getLastLowLevelResult() instanceof QueryOutcome) {
                    lastEvaluatedKey = toKeyAttributes(
                        ((QueryOutcome) fetched.getLastLowLevelResult()).getQueryResult().getLastEvaluatedKey());
                }
                return items.hasNext();
            }

        };
    }

    private KeyAttribute[] toKeyAttributes(Map<String, AttributeValue> values) {
        return values.entrySet()
                .stream()
                .map((i) -> new KeyAttribute(i.getKey(), i.getValue())).toArray(KeyAttribute[]::new);
    }

    public boolean deleteAndCheck(PrimaryKey id) {
        return deleteItem(new DeleteItemSpec().withPrimaryKey(id).withReturnValues(ReturnValue.ALL_OLD))
            .getDeleteItemResult()
            .getAttributes() != null;
    }

    public boolean exists() {
        try {
            describe();
            return true;
        } catch(ResourceNotFoundException e) {
            return false;
        } 
    }

    public ProvisionedThroughput getProvisionedThroughput() {
        ProvisionedThroughputDescription description = describe().getProvisionedThroughput();
        return new ProvisionedThroughput(description.getReadCapacityUnits(), description.getWriteCapacityUnits());
    }

    private static final Field clientField;

    static {
        try {
            clientField = Table.class.getDeclaredField("client");
            clientField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new AssertionError("This shouldn't happen", e);
        }
    }

    private static AmazonDynamoDB getClient(Table table) {
        try {
            return (AmazonDynamoDB) clientField.get(table);
        } catch (IllegalAccessException e) {
            throw new AssertionError("This shouldn't happen", e);
        }
    }
}
