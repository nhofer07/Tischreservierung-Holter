package at.htlleonding.repo;

import static at.htlleonding.repo.RepositorySupport.isBlank;

import at.htlleonding.DTOs.NameRequestDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;

@ApplicationScoped
public class EquipmentRepo extends MongoRepository {

    @Inject
    AuditRepo auditRepo;

    @Inject
    BenutzerZugriff benutzerZugriff;

    public List<String> getEquipmentCatalog(String adminId) {
        benutzerZugriff.requireAdmin(adminId);
        return getDatabase()
            .getCollection("equipment")
            .find()
            .sort(new Document("name", 1))
            .map(dokument -> dokument.getString("name"))
            .into(new ArrayList<>());
    }

    public String createEquipment(String adminId, NameRequestDTO request) {
        benutzerZugriff.requireSuperadmin(adminId);
        if (request == null || isBlank(request.name())) {
            throw new BadRequestException("Name ist erforderlich.");
        }
        String name = request.name().trim();
        if (
            getDatabase().getCollection("equipment").find(new Document("name", name)).first() !=
            null
        ) {
            throw new BadRequestException("Dieses Equipment existiert bereits.");
        }
        getDatabase()
            .getCollection("equipment")
            .insertOne(
                new Document("_id", "EQ-" + System.currentTimeMillis()).append("name", name)
            );
        auditRepo.recordAuditEntry(adminId, "EQUIPMENT_ANGELEGT", name);
        return name;
    }

    public void deleteEquipment(String adminId, String name) {
        benutzerZugriff.requireSuperadmin(adminId);
        if (
            getDatabase()
                .getCollection("arbeitsplaetze")
                .find(new Document("equipment", name))
                .first() != null
        ) {
            throw new BadRequestException("Equipment wird noch von Arbeitsplaetzen verwendet.");
        }
        getDatabase().getCollection("equipment").deleteOne(new Document("name", name));
        auditRepo.recordAuditEntry(adminId, "EQUIPMENT_GELOESCHT", name);
    }
}
