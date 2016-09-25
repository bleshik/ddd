package eventstore.util.json;

import eventstore.util.DbObjectMapper;
import java.util.function.Function;

/**
 * Utility class implementing common logic of mapping POJOs to/from db objects.
 */
public class JsonDbObjectMapper<T> implements DbObjectMapper<T> {
        
	private final JsonSerde jsonSerde;
    private final Function<T, String> toJson;
    private final Function<String, T> fromJson;

    public JsonDbObjectMapper(JsonSerde jsonSerde, Function<T, String> toJson, Function<String, T> fromJson) { 
		this.jsonSerde = jsonSerde;
        this.toJson    = toJson;
        this.fromJson  = fromJson;
    }

    @Override
    public Object mapToObject(T dbObject) {
        return jsonSerde.deserialize(toJson.apply(dbObject));
    }

    @Override
    public T mapToDbObject(Object obj) {
        return fromJson.apply(jsonSerde.serialize(obj));
    }

}
