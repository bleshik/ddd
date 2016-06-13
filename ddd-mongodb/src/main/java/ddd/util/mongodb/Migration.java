package ddd.util.mongodb;

import com.mongodb.MongoTimeoutException;

/**
 * Utility methods for MongoDB migrations.
 * TODO: use some tool rather than that
 */
public final class Migration {
    private Migration() {}
    /**
     * Runs the given runnable, catching the timeouts in case when the mongo server is not available, so this waits
     * until the mongo server is up and running.
     * TODO: maybe better to crash the application, so that automatic restart reconnects to the server when it's alive?
     */
    public static void migrate(Runnable migrations) {
        try {
            migrations.run();
        } catch(MongoTimeoutException e) {
            migrate(migrations);
        }
    }
}
