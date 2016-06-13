package ddd.util.mongodb;

import com.mongodb.MongoTimeoutException;

public final class Migration {
    private Migration() {}
    public static void migrate(Runnable migrations) {
        try {
            migrations.run();
        } catch(MongoTimeoutException e) {
            migrate(migrations);
        }
    }
}
