package at.htlleonding.repo;

import static at.htlleonding.repo.RepositoryMapper.toLocation;
import static at.htlleonding.repo.RepositoryMapper.toLocationDto;
import static at.htlleonding.repo.RepositorySupport.isBlank;

import at.htlleonding.DTOs.StandortDTO;
import at.htlleonding.DTOs.StandortRequestDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;

@ApplicationScoped
public class StandortRepo extends MongoRepository {

    @Inject
    AuditRepo auditRepo;

    @Inject
    BenutzerZugriff benutzerZugriff;

    public StandortDTO createLocation(String adminId, StandortRequestDTO request) {
        benutzerZugriff.requireSuperadmin(adminId);
        validateLocationRequest(request);
        String id = "STANDORT-" + System.currentTimeMillis();
        Document dokument = buildLocationDocument(id, request);
        getDatabase().getCollection("standorte").insertOne(dokument);
        auditRepo.recordAuditEntry(adminId, "STANDORT_ANGELEGT", request.name());
        return toLocationDto(toLocation(dokument));
    }

    public StandortDTO updateLocation(String adminId, String id, StandortRequestDTO request) {
        benutzerZugriff.requireSuperadmin(adminId);
        validateLocationRequest(request);
        Document dokument = buildLocationDocument(id, request);
        if (
            getDatabase()
                .getCollection("standorte")
                .replaceOne(new Document("_id", id), dokument)
                .getMatchedCount() == 0
        ) {
            throw new NotFoundException("Standort wurde nicht gefunden.");
        }
        auditRepo.recordAuditEntry(adminId, "STANDORT_GEAENDERT", request.name());
        return toLocationDto(toLocation(dokument));
    }

    public void deleteLocation(String adminId, String id) {
        benutzerZugriff.requireSuperadmin(adminId);
        if (
            getDatabase().getCollection("raeume").find(new Document("standortId", id)).first() !=
            null
        ) {
            throw new BadRequestException("Standort besitzt noch Räume.");
        }
        if (
            getDatabase()
                .getCollection("standorte")
                .deleteOne(new Document("_id", id))
                .getDeletedCount() == 0
        ) {
            throw new NotFoundException("Standort wurde nicht gefunden.");
        }
        auditRepo.recordAuditEntry(adminId, "STANDORT_GELOESCHT", id);
    }

    public List<StandortDTO> getLocations() {
        List<StandortDTO> standorte = getDatabase()
            .getCollection("standorte")
            .find()
            .sort(new Document("ort", 1))
            .map(RepositoryMapper::toLocation)
            .map(RepositoryMapper::toLocationDto)
            .into(new ArrayList<>());

        if (standorte.isEmpty()) {
            throw new NotFoundException("Keine Standorte in der Datenbank gefunden.");
        }

        return standorte;
    }

    private void validateLocationRequest(StandortRequestDTO request) {
        if (
            request == null ||
            isBlank(request.name()) ||
            isBlank(request.adresse()) ||
            isBlank(request.ort())
        ) {
            throw new BadRequestException("Bitte alle Standortdaten ausfüllen.");
        }
    }

    private Document buildLocationDocument(String id, StandortRequestDTO request) {
        return new Document("_id", id)
            .append("name", request.name().trim())
            .append("adresse", request.adresse().trim())
            .append("ort", request.ort().trim());
    }
}
