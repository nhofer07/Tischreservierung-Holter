package at.htlleonding.repo;

import static at.htlleonding.repo.RepositorySupport.isSuperadmin;

import at.htlleonding.DTOs.BenutzerDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.Document;

@ApplicationScoped
public class AuditRepo extends MongoRepository {

    @Inject
    BenutzerZugriff benutzerZugriff;

    public List<Document> getAuditLog(String adminId) {
        BenutzerDTO admin = benutzerZugriff.requireAdmin(adminId);
        Document filter = isSuperadmin(admin)
            ? new Document()
            : new Document("adminId", admin.id());
        return getDatabase()
            .getCollection("auditLog")
            .find(filter)
            .sort(new Document("zeitpunkt", -1))
            .limit(100)
            .into(new ArrayList<>());
    }

    void recordAuditEntry(String adminId, String aktion, String details) {
        getDatabase()
            .getCollection("auditLog")
            .insertOne(
                new Document("_id", "AUDIT-" + System.nanoTime())
                    .append("adminId", adminId)
                    .append("aktion", aktion)
                    .append("details", details)
                    .append("zeitpunkt", new Date())
            );
    }
}
