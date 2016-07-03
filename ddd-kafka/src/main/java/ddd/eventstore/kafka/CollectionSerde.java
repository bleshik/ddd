package ddd.eventstore.kafka;

import ddd.eventstore.kafka.gson.GsonSerde;
import ddd.util.RuntimeGeneric;
import java.util.Collection;

public class CollectionSerde<T extends Collection> extends GsonSerde<T> { }
