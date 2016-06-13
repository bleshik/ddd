package ddd.repository.eventsourcing;

public class EventSourcingConflictException extends EventSourcingException {
    public EventSourcingConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
