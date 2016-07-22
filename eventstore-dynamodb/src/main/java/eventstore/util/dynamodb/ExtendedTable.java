package eventstore.util.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.Select;
import eventstore.util.collection.Collections;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Same {@link Table}, but with additional utility methods.
 */
public class ExtendedTable extends Table {

    public ExtendedTable(AmazonDynamoDB client, String tableName) { 
        super(client, tableName);
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

    public Stream<Item> scanStream(ScanSpec scan) {
        return Collections.stream(scan(scan));
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
