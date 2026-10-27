package at.htlleonding.repo;

import static at.htlleonding.repo.RepositorySupport.hashPassword;
import static at.htlleonding.repo.RepositorySupport.isBlank;
import static at.htlleonding.repo.RepositorySupport.isSuperadmin;

import at.htlleonding.DTOs.AbteilungDTO;
import at.htlleonding.DTOs.BenutzerDTO;
import at.htlleonding.DTOs.NameRequestDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;

@ApplicationScoped
public class AbteilungRepo extends MongoRepository {

    @Inject
    AuditRepo auditRepo;

    @Inject
    BenutzerZugriff benutzerZugriff;

    public List<AbteilungDTO> getDepartments(String adminId) {
        BenutzerDTO admin = benutzerZugriff.requireAdmin(adminId);
        Document filter = isSuperadmin(admin)
            ? new Document()
            : new Document("_id", admin.abteilungId());
        return getDatabase()
            .getCollection("abteilungen")
            .find(filter)
            .sort(new Document("name", 1))
            .map(dokument ->
                new AbteilungDTO(dokument.getString("_id"), dokument.getString("name"))
            )
            .into(new ArrayList<>());
    }

    public AbteilungDTO createDepartment(String adminId, NameRequestDTO request) {
        benutzerZugriff.requireSuperadmin(adminId);
        if (request == null || isBlank(request.name())) {
            throw new BadRequestException("Name ist erforderlich.");
        }
        String name = request.name().trim();
        String id = "ABT-" + name.toUpperCase().replaceAll("[^A-Z0-9]+", "-");
        Document dokument = new Document("_id", id).append("name", name);
        try {
            getDatabase().getCollection("abteilungen").insertOne(dokument);
        } catch (RuntimeException exception) {
            throw new BadRequestException("Diese Abteilung existiert bereits.");
        }
        String emailTeil = name.toLowerCase().replaceAll("[^a-z0-9]+", ".");
        getDatabase()
            .getCollection("benutzer")
            .insertOne(
                new Document("_id", "USER-" + id)
                    .append("vorname", "Test")
                    .append("nachname", name)
                    .append("email", emailTeil + "@example.test")
                    .append("passwortHash", hashPassword("holter123"))
                    .append("rolle", "USER")
                    .append("aktiv", true)
                    .append("abteilungId", id)
                    .append("abteilungName", name)
                    .append("bevorzugterRaumId", "")
            );
        auditRepo.recordAuditEntry(adminId, "ABTEILUNG_ANGELEGT", name);
        return new AbteilungDTO(id, name);
    }

    public void deleteDepartment(String adminId, String id) {
        benutzerZugriff.requireSuperadmin(adminId);
        if (
            getDatabase().getCollection("raeume").find(new Document("abteilungId", id)).first() !=
            null
        ) {
            throw new BadRequestException("Abteilung wird noch von Raeumen verwendet.");
        }
        if (
            getDatabase()
                .getCollection("reservierungen")
                .find(new Document("abteilungId", id))
                .first() != null
        ) {
            throw new BadRequestException("Abteilung besitzt noch Reservierungen.");
        }
        getDatabase()
            .getCollection("benutzer")
            .deleteMany(new Document("abteilungId", id).append("rolle", "USER"));
        if (
            getDatabase()
                .getCollection("abteilungen")
                .deleteOne(new Document("_id", id))
                .getDeletedCount() == 0
        ) {
            throw new NotFoundException("Abteilung wurde nicht gefunden.");
        }
        auditRepo.recordAuditEntry(adminId, "ABTEILUNG_GELOESCHT", id);
    }
}
