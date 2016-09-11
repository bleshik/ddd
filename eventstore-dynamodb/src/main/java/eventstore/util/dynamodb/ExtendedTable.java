package eventstore.util.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.internal.InternalUtils;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.Select;
import eventstore.util.collection.Collections;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
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

    /**
     * Handy constructor creating the extended table based on an usual table.
     */
    public ExtendedTable(Table table) {
        this(getClient(table), table.getTableName());
    }

    public Stream<Item> queryStream(QuerySpec query) {
        return Collections.stream(query(query));
    }

    public long count() {
        // TODO: return more up-to-date number
        return describe().getItemCount();
    }

    public Stream<Item> scanStream(ScanSpec s, boolean all) {
        return Collections.stream(
            doGetResult((lastEvaluatedKey) -> client.scan(toRequest(s).withExclusiveStartKey(lastEvaluatedKey)), all)
        );
    }

    public Stream<Item> scanStream(ScanSpec scan) {
        return scanStream(scan, false);
    }

    private ScanRequest toRequest(ScanSpec s) {
        return s.getRequest().withTableName(getTableName());
    }

    private <T> Iterator<Item> doGetResult(Function<Map<String, AttributeValue>, T> fetch, boolean all) {
        return new Iterator<Item>() {

            Iterator<Item> items;
            Map<String, AttributeValue> lastEvaluatedKey;

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
                T fetched = fetch.apply(lastEvaluatedKey);
                if (fetched instanceof ScanResult) {
                    ScanResult scanResult = (ScanResult) fetched;
                    items = InternalUtils.toItemList(scanResult.getItems()).iterator();
                    lastEvaluatedKey = scanResult.getLastEvaluatedKey();
                } else {
                    QueryResult queryResult = (QueryResult) fetched;
                    items = InternalUtils.toItemList(queryResult.getItems()).iterator();
                    lastEvaluatedKey = queryResult.getLastEvaluatedKey();
                }
                return items.hasNext();
            }

        };
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
