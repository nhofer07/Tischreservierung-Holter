package at.htlleonding.repo;

import static at.htlleonding.repo.RepositoryMapper.toWorkstation;
import static at.htlleonding.repo.RepositoryMapper.toWorkstationDto;
import static at.htlleonding.repo.RepositorySupport.isBlank;
import static at.htlleonding.repo.RepositorySupport.validateDepartmentAccess;

import at.htlleonding.DTOs.ArbeitsplatzDTO;
import at.htlleonding.DTOs.ArbeitsplatzRequestDTO;
import at.htlleonding.DTOs.BenutzerDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import org.bson.Document;

@ApplicationScoped
public class ArbeitsplatzRepo extends MongoRepository {

    @Inject
    AuditRepo auditRepo;

    @Inject
    BenutzerZugriff benutzerZugriff;

    public ArbeitsplatzDTO createWorkstation(String adminId, ArbeitsplatzRequestDTO request) {
        BenutzerDTO admin = benutzerZugriff.requireAdmin(adminId);
        validateWorkstationRequest(request);
        validateDepartmentAccess(admin, request.abteilungId());
        String id = "TISCH-" + System.currentTimeMillis();
        Document dokument = buildWorkstationDocument(id, request);
        getDatabase().getCollection("arbeitsplaetze").insertOne(dokument);
        auditRepo.recordAuditEntry(adminId, "ARBEITSPLATZ_ANGELEGT", request.name());
        return toWorkstationDto(toWorkstation(dokument), admin);
    }

    public ArbeitsplatzDTO updateWorkstation(
        String adminId,
        String id,
        ArbeitsplatzRequestDTO request
    ) {
        BenutzerDTO admin = benutzerZugriff.requireAdmin(adminId);
        validateWorkstationRequest(request);
        validateDepartmentAccess(admin, request.abteilungId());
        Document dokument = buildWorkstationDocument(id, request);
        if (
            getDatabase()
                .getCollection("arbeitsplaetze")
                .replaceOne(new Document("_id", id), dokument)
                .getMatchedCount() == 0
        ) {
            throw new NotFoundException("Arbeitsplatz wurde nicht gefunden.");
        }
        auditRepo.recordAuditEntry(adminId, "ARBEITSPLATZ_GEAENDERT", request.name());
        return toWorkstationDto(toWorkstation(dokument), admin);
    }

    public void deleteWorkstation(String adminId, String id) {
        benutzerZugriff.requireAdmin(adminId);
        if (
            getDatabase()
                .getCollection("reservierungen")
                .find(new Document("arbeitsplatzId", id).append("status", "reserviert"))
                .first() != null
        ) {
            throw new BadRequestException(
                "Arbeitsplatz mit aktiver Reservierung kann nicht geloescht werden."
            );
        }
        if (
            getDatabase()
                .getCollection("arbeitsplaetze")
                .deleteOne(new Document("_id", id))
                .getDeletedCount() == 0
        ) {
            throw new NotFoundException("Arbeitsplatz wurde nicht gefunden.");
        }
        auditRepo.recordAuditEntry(adminId, "ARBEITSPLATZ_GELOESCHT", id);
    }

    private void validateWorkstationRequest(ArbeitsplatzRequestDTO request) {
        if (
            request == null ||
            isBlank(request.tischnr()) ||
            request.tischnr().trim().length() > 12 ||
            isBlank(request.name()) ||
            isBlank(request.raumId()) ||
            isBlank(request.standortId()) ||
            isBlank(request.abteilungId()) ||
            isBlank(request.abteilungName()) ||
            request.x() < 8 ||
            request.x() > 92 ||
            request.y() < 12 ||
            request.y() > 86 ||
            request.breite() < 6 ||
            request.breite() > 24 ||
            request.hoehe() < 6 ||
            request.hoehe() > 24
        ) {
            throw new BadRequestException("Arbeitsplatzdaten oder Position sind ungueltig.");
        }
    }

    private Document buildWorkstationDocument(String id, ArbeitsplatzRequestDTO request) {
        return new Document("_id", id)
            .append("tischnr", request.tischnr().trim())
            .append("raumId", request.raumId())
            .append("standortId", request.standortId())
            .append("abteilungId", request.abteilungId())
            .append("abteilungName", request.abteilungName())
            .append("name", request.name().trim())
            .append("status", "frei")
            .append("position", new Document("x", request.x()).append("y", request.y()))
            .append("rotation", request.rotation())
            .append("breite", request.breite())
            .append("hoehe", request.hoehe())
            .append("equipment", request.equipment() == null ? List.of() : request.equipment());
    }
}
