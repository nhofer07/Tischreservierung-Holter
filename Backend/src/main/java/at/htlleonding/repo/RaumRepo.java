package at.htlleonding.repo;

import static at.htlleonding.repo.RepositoryMapper.toRoom;
import static at.htlleonding.repo.RepositoryMapper.toRoomDto;
import static at.htlleonding.repo.RepositoryMapper.toRoomOptionDto;
import static at.htlleonding.repo.RepositorySupport.isBlank;
import static at.htlleonding.repo.RepositorySupport.validateDepartmentAccess;

import at.htlleonding.DTOs.BenutzerDTO;
import at.htlleonding.DTOs.RaumAuswahlDTO;
import at.htlleonding.DTOs.RaumDTO;
import at.htlleonding.DTOs.RaumRequestDTO;
import at.htlleonding.model.Arbeitsplatz;
import com.mongodb.client.MongoDatabase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.Document;

@ApplicationScoped
public class RaumRepo extends MongoRepository {

    @Inject
    AuditRepo auditRepo;

    @Inject
    BenutzerZugriff benutzerZugriff;

    @Inject
    ReservierungRepo reservierungRepo;

    public RaumAuswahlDTO createRoom(String adminId, RaumRequestDTO request) {
        BenutzerDTO admin = benutzerZugriff.requireAdmin(adminId);
        validateRoomRequest(request);
        validateDepartmentAccess(admin, request.abteilungId());
        String id = "RAUM-" + System.currentTimeMillis();
        Document dokument = buildRoomDocument(id, request);
        getDatabase().getCollection("raeume").insertOne(dokument);
        auditRepo.recordAuditEntry(adminId, "RAUM_ANGELEGT", request.name());
        getDatabase()
            .getCollection("benutzer")
            .updateMany(
                new Document("abteilungId", request.abteilungId()).append("bevorzugterRaumId", ""),
                new Document("$set", new Document("bevorzugterRaumId", id))
            );
        return toRoomOptionDto(dokument);
    }

    public RaumAuswahlDTO updateRoom(String adminId, String id, RaumRequestDTO request) {
        BenutzerDTO admin = benutzerZugriff.requireAdmin(adminId);
        validateRoomRequest(request);
        validateDepartmentAccess(admin, request.abteilungId());
        Document dokument = buildRoomDocument(id, request);
        if (
            getDatabase()
                .getCollection("raeume")
                .replaceOne(new Document("_id", id), dokument)
                .getMatchedCount() == 0
        ) {
            throw new NotFoundException("Raum wurde nicht gefunden.");
        }
        getDatabase()
            .getCollection("arbeitsplaetze")
            .updateMany(
                new Document("raumId", id),
                new Document(
                    "$set",
                    new Document("standortId", request.standortId())
                        .append("abteilungId", request.abteilungId())
                        .append("abteilungName", request.abteilungName())
                )
            );
        auditRepo.recordAuditEntry(adminId, "RAUM_GEAENDERT", request.name());
        return toRoomOptionDto(dokument);
    }

    public void deleteRoom(String adminId, String id) {
        benutzerZugriff.requireSuperadmin(adminId);
        Document geloeschterRaum = getDatabase()
            .getCollection("raeume")
            .find(new Document("_id", id))
            .first();
        if (geloeschterRaum == null) throw new NotFoundException("Raum wurde nicht gefunden.");
        if (
            getDatabase()
                .getCollection("reservierungen")
                .find(
                    new Document("raumId", id)
                        .append("status", "reserviert")
                        .append("reservierungEnde", new Document("$gt", new Date()))
                )
                .first() != null
        ) {
            throw new BadRequestException(
                "Raum mit aktiven Reservierungen kann nicht geloescht werden."
            );
        }
        getDatabase().getCollection("reservierungen").deleteMany(new Document("raumId", id));
        getDatabase().getCollection("arbeitsplaetze").deleteMany(new Document("raumId", id));
        if (
            getDatabase()
                .getCollection("raeume")
                .deleteOne(new Document("_id", id))
                .getDeletedCount() == 0
        ) {
            throw new NotFoundException("Raum wurde nicht gefunden.");
        }
        Document ersatz = getDatabase()
            .getCollection("raeume")
            .find(
                new Document("abteilungId", geloeschterRaum.getString("abteilungId")).append(
                    "standortId",
                    geloeschterRaum.getString("standortId")
                )
            )
            .first();
        if (ersatz == null) {
            ersatz = getDatabase()
                .getCollection("raeume")
                .find(new Document("abteilungId", geloeschterRaum.getString("abteilungId")))
                .first();
        }
        getDatabase()
            .getCollection("benutzer")
            .updateMany(
                new Document("bevorzugterRaumId", id),
                new Document(
                    "$set",
                    new Document("bevorzugterRaumId", ersatz == null ? "" : ersatz.getString("_id"))
                )
            );
        auditRepo.recordAuditEntry(adminId, "RAUM_GELOESCHT", id);
    }

    public RaumDTO getRoom(String raumId, String von, String bis, String benutzerId) {
        BenutzerDTO benutzer = benutzerZugriff.getUser(benutzerId);
        MongoDatabase db = getDatabase();
        Document raumDocument = db
            .getCollection("raeume")
            .find(new Document("_id", raumId))
            .first();

        if (raumDocument == null) {
            throw new NotFoundException("Raum wurde nicht in der Datenbank gefunden.");
        }

        List<Arbeitsplatz> arbeitsplaetze = db
            .getCollection("arbeitsplaetze")
            .find(new Document("raumId", raumId))
            .sort(new Document("tischnr", 1))
            .map(RepositoryMapper::toWorkstation)
            .into(new ArrayList<>());

        reservierungRepo.applyReservationStatus(db, arbeitsplaetze, von, bis);
        return toRoomDto(toRoom(raumDocument, arbeitsplaetze), benutzer);
    }

    public List<RaumAuswahlDTO> getRoomsByLocation(String standortId) {
        List<RaumAuswahlDTO> raeume = getDatabase()
            .getCollection("raeume")
            .find(new Document("standortId", standortId))
            .sort(new Document("stockwerk", 1).append("name", 1))
            .map(RepositoryMapper::toRoomOptionDto)
            .into(new ArrayList<>());

        if (raeume.isEmpty()) {
            throw new NotFoundException("Keine Raeume fuer diesen Standort gefunden.");
        }

        return raeume;
    }

    private void validateRoomRequest(RaumRequestDTO request) {
        if (
            request == null ||
            isBlank(request.name()) ||
            isBlank(request.stockwerk()) ||
            isBlank(request.standortId()) ||
            isBlank(request.abteilungId()) ||
            isBlank(request.abteilungName())
        ) {
            throw new BadRequestException("Bitte alle Raumdaten ausfuellen.");
        }
    }

    private Document buildRoomDocument(String id, RaumRequestDTO request) {
        return new Document("_id", id)
            .append("name", request.name().trim())
            .append("stockwerk", request.stockwerk().trim())
            .append("standortId", request.standortId())
            .append("abteilungId", request.abteilungId())
            .append("abteilungName", request.abteilungName().trim())
            .append(
                "elemente",
                request.elemente() == null
                    ? List.of()
                    : request
                          .elemente()
                          .stream()
                          .map(element ->
                              new Document("id", element.id())
                                  .append("typ", element.typ())
                                  .append("x", element.x())
                                  .append("y", element.y())
                                  .append("breite", element.breite())
                                  .append("hoehe", element.hoehe())
                                  .append("text", element.text())
                                  .append("rotation", element.rotation())
                          )
                          .toList()
            )
            .append("beschreibung", "Im Room Builder verwalteter Raum");
    }
}
