package repository.eventsourcing;

public class EventSourcingException extends RuntimeException {
    public EventSourcingException(String message, Throwable cause) {
        super(message, cause);
    }
}
