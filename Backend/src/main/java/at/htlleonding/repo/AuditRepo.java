package at.htlleonding.repo;

import static at.htlleonding.repo.RepositorySupport.isSuperadmin;

import at.htlleonding.DTOs.BenutzerDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        List<Document> eintraege = getDatabase()
            .getCollection("auditLog")
            .find(filter)
            .sort(new Document("zeitpunkt", -1))
            .limit(100)
            .into(new ArrayList<>());
        Map<String, String> adminNamen = new HashMap<>();
        for (Document eintrag : eintraege) {
            String id = eintrag.getString("adminId");
            String name = adminNamen.computeIfAbsent(id, schluessel -> {
                Document benutzer = getDatabase()
                    .getCollection("benutzer")
                    .find(new Document("_id", schluessel))
                    .first();
                return benutzer == null
                    ? schluessel
                    : benutzer.getString("vorname") + " " + benutzer.getString("nachname");
            });
            eintrag.put("adminName", name);
        }
        return eintraege;
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
