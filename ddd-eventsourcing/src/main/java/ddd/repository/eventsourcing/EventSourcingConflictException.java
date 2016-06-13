package ddd.repository.eventsourcing;

/**
 * Exception for cases, when it's not possible to solve the conflict of two concurrent modifications.
 */
public class EventSourcingConflictException extends EventSourcingException {
    public EventSourcingConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
