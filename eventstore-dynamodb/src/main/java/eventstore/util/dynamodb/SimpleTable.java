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
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Same {@link Table}, but with additional utility methods.
 */
public class SimpleTable<T> extends ExtendedTable {

    protected String idName;

    public SimpleTable(
            AmazonDynamoDB client,
            String tableName,
            String idName,
            Class<T> idClass,
            long readCapacityUnits,
            long writeCapacityUnits) {
        super(client, tableName, idName, idClass, readCapacityUnits, writeCapacityUnits);
        this.idName = idName;
    }

    public SimpleTable(AmazonDynamoDB client, String tableName, Class<T> idClass, ProvisionedThroughput t) {
        super(client, tableName, idClass, t);
        this.idName = idName;
    }

    public SimpleTable(
            AmazonDynamoDB client,
            String tableName,
            String idName,
            Class<T> idClass,
            ProvisionedThroughput t) {
        super(client, tableName, idName, idClass, t);
        this.idName = idName;
    }

    public UpdateItemOutcome put(T key, String attribute, Object value) {
        return put(new PrimaryKey(idName, key), attribute, value);
    }

    public UpdateItemOutcome add(T key, String attribute, Object... values) {
        return add(new PrimaryKey(idName, key), attribute, values);
    }

    public UpdateItemOutcome add(T key, String attribute, Collection values) {
        return add(key, attribute, values.toArray());
    }

    public Item getItem(T key) {
        return getItem(idName, key);
    }

}
