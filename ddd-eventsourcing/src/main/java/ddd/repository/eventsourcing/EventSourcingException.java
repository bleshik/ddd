package ddd.repository.eventsourcing;

/**
 * Exception related to Event Sourcing.
 */
public class EventSourcingException extends RuntimeException {
    public EventSourcingException(String message, Throwable cause) {
        super(message, cause);
    }
}
