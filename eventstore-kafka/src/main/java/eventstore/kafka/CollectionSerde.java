package eventstore.kafka;

import eventstore.kafka.gson.GsonSerde;
import eventstore.util.RuntimeGeneric;
import java.util.Collection;

public class CollectionSerde<T extends Collection> extends GsonSerde<T> { }
