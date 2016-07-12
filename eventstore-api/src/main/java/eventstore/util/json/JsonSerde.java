package eventstore.util.json;

/**
 * Object converting from a JSON to a POJO, and backwards.
 */
public interface JsonSerde {
    /**
     * Deserializes from a JSON to a POJO.
     * @param json json string
     */
    Object deserialize(String json);

    /**
     * Serializes to a JSON from a POJO.
     * @param obj an object is to be converted to JSON
     */
    String serialize(Object obj);

}
