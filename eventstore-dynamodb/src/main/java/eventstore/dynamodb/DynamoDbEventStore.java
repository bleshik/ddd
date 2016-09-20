package eventstore.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.Expected;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateTableSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import eventstore.AbstractEventStore;
import eventstore.Event;
import eventstore.EventStore;
import eventstore.EventStoreException;
import eventstore.util.DbObjectMapper;
import eventstore.util.collection.Collections;
import eventstore.util.dynamodb.ExtendedTable;
import eventstore.util.dynamodb.GsonDynamoDbObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * DynamoDb-based event store.
 */
@SuppressWarnings("unchecked")
public class DynamoDbEventStore extends AbstractEventStore<Item> {

    private final ExtendedTable table;

    public DynamoDbEventStore(
            AmazonDynamoDB client,
            String tableName,
            long readCapacityUnits,
            long writeCapacityUnits) {
        this(
                new ExtendedTable(
                    client,
                    tableName,
                    Arrays.asList(
                        new AttributeDefinition("streamName", "S"),
                        new AttributeDefinition("streamVersion", "N")
                    ),
                    Arrays.asList(
                        new KeySchemaElement("streamName", KeyType.HASH),
                        new KeySchemaElement("streamVersion", KeyType.RANGE)
                    ),
                    new ProvisionedThroughput(readCapacityUnits, writeCapacityUnits)
                ),
                new GsonDynamoDbObjectMapper()
        );
    }

    public DynamoDbEventStore(Table table, DbObjectMapper<Item> mapper) { 
        super(mapper);
        this.table  = new ExtendedTable(table);
    }

    @Override
    protected Iterator<Item> iteratorSince(String streamName, long lastReceivedEvent) {
        return table.queryStream(new QuerySpec()
                .withHashKey("streamName", streamName)
                .withRangeKeyCondition(new RangeKeyCondition("streamVersion").gt(lastReceivedEvent))
        ).iterator();
    }

    @Override
    public void append(String streamName, long currentVersion, List<? extends Event> newEvents) {
        long nextEventIndex = currentVersion;
        for (Event event : newEvents) {
            try {
                table.putItem(
                    new PutItemSpec()
                        .withItem(mapper.mapToDbObject(event.occurred(++nextEventIndex)).withString("streamName", streamName))
                        .withExpected(new Expected("streamName").notExist())
                );
            } catch (ConditionalCheckFailedException e) {
                throw new ConcurrentModificationException(
                        "Failed to put item " + event + " into the stream " + streamName,
                        e
                );
            }
        }
    }

    @Override
    public long size() {
        //TODO: better to use a separate table with counters
        return table.scanStream(new ScanSpec().withAttributesToGet("streamName"), true)
            .map((item) -> item.getString("streamName"))
            .collect(Collectors.toSet()).size();
    }

    @Override
    public long version(String streamName) {
        return table.queryStream(new QuerySpec()
            .withAttributesToGet("streamVersion")
            .withScanIndexForward(false)
            .withHashKey("streamName", streamName)
            .withMaxResultSize(1)
        ).map(e -> (long) e.getLong("streamVersion")).findAny().orElse(0L);
    }

}
