package ddd.repository;

/**
 * Generic exception for repositories.
 */
public class RepositoryException extends RuntimeException {
    RepositoryException(String msg, Throwable t) {
        super(msg, t);
    }
    RepositoryException(String msg) {
        super(msg);
    }
}
