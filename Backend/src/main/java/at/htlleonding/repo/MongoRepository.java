package at.htlleonding.repo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import jakarta.inject.Inject;

/** Gemeinsamer MongoDB-Zugriff; fachliche Operationen liegen in den konkreten Repositories. */
public abstract class MongoRepository {

    @Inject
    MongoClient mongoClient;

    protected MongoDatabase getDatabase() {
        return mongoClient.getDatabase("tischreservierung");
    }
}
