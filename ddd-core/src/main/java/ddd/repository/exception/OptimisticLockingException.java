package ddd.repository.exception;

/**
 * Exception for cases, when it's not possible to solve the conflict of two concurrent modifications.
 */
public class OptimisticLockingException extends RuntimeException {
    public OptimisticLockingException(String message) {
        super(message);
    }
    public OptimisticLockingException(String message, Throwable cause) {
        super(message, cause);
    }
}
