package ddd.repository;

/**
 * Exception for cases when something was not found (e.g. entity in a repository).
 */
public class NotFoundException extends RepositoryException {
    NotFoundException() {
        super("NOT_FOUND");
    }
}
