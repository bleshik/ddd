package ddd.repository;

/**
 * Exception for cases when something is already exists (e.g. entity in a repository).
 */
public class AlreadyExistsException extends RepositoryException {
    AlreadyExistsException() {
        super("ALREADY_EXISTS");
    }
}
