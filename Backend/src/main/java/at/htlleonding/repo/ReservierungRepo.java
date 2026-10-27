package at.htlleonding.repo;

import static at.htlleonding.repo.RepositoryMapper.toReservationDto;
import static at.htlleonding.repo.RepositoryMapper.toWorkstation;
import static at.htlleonding.repo.RepositorySupport.isBlank;
import static at.htlleonding.repo.RepositorySupport.isSuperadmin;
import static at.htlleonding.repo.RepositorySupport.parseDateTime;
import static at.htlleonding.repo.RepositorySupport.validateDepartmentAccess;
import static at.htlleonding.repo.RepositorySupport.validateTimeRange;

import at.htlleonding.DTOs.BenutzerDTO;
import at.htlleonding.DTOs.NameRequestDTO;
import at.htlleonding.DTOs.ReservierungDTO;
import at.htlleonding.DTOs.ReservierungRequestDTO;
import at.htlleonding.model.Arbeitsplatz;
import com.mongodb.client.MongoDatabase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;

@ApplicationScoped
public class ReservierungRepo extends MongoRepository {

    @Inject
    AuditRepo auditRepo;

    @Inject
    BenutzerZugriff benutzerZugriff;

    @Inject
    EinstellungRepo einstellungRepo;

    public List<ReservierungDTO> getAllReservations(String adminId) {
        BenutzerDTO admin = benutzerZugriff.requireAdmin(adminId);
        MongoDatabase db = getDatabase();
        Document filter = isSuperadmin(admin)
            ? new Document()
            : new Document("abteilungId", admin.abteilungId());
        return db
            .getCollection("reservierungen")
            .find(filter)
            .sort(new Document("reservierungAnfang", -1))
            .map(dokument -> toReservationDto(db, dokument))
            .into(new ArrayList<>());
    }

    public ReservierungDTO createReservation(ReservierungRequestDTO request) {
        BenutzerDTO benutzer = benutzerZugriff.getUser(request.benutzerId());
        Date anfang = parseDateTime(request.reservierungAnfang());
        Date ende = parseDateTime(request.reservierungEnde());
        validateTimeRange(anfang, ende);
        einstellungRepo.validateBookingLimit(anfang, ende);

        MongoDatabase db = getDatabase();
        Document arbeitsplatzDocument = db
            .getCollection("arbeitsplaetze")
            .find(new Document("_id", request.arbeitsplatzId()))
            .first();

        if (arbeitsplatzDocument == null) {
            throw new NotFoundException("Arbeitsplatz wurde nicht in der Datenbank gefunden.");
        }

        Arbeitsplatz arbeitsplatz = toWorkstation(arbeitsplatzDocument);
        validateDepartmentAccess(arbeitsplatz, benutzer);
        if (request.wiederholungen() < 0 || request.wiederholungen() > 51) {
            throw new BadRequestException(
                "Die Anzahl der Wiederholungen muss zwischen 0 und 51 liegen."
            );
        }
        int anzahl = request.wiederholungen() + 1;
        List<Document> neueReservierungen = new ArrayList<>();
        for (int index = 0; index < anzahl; index++) {
            Date serienAnfang = Date.from(
                anfang.toInstant().atZone(ZoneId.of("Europe/Vienna")).plusWeeks(index).toInstant()
            );
            Date serienEnde = Date.from(
                ende.toInstant().atZone(ZoneId.of("Europe/Vienna")).plusWeeks(index).toInstant()
            );
            einstellungRepo.validateBookingLimit(serienAnfang, serienEnde);
            validateWorkstationAvailability(db, arbeitsplatz.getId(), serienAnfang, serienEnde);
            validateUserAvailability(db, benutzer.id(), serienAnfang, serienEnde, null);
            neueReservierungen.add(
                new Document("_id", "RES-" + System.currentTimeMillis() + "-" + index)
                    .append("benutzerId", benutzer.id())
                    .append("arbeitsplatzId", arbeitsplatz.getId())
                    .append("raumId", arbeitsplatz.getRoomId())
                    .append("standortId", arbeitsplatz.getLocationId())
                    .append("abteilungId", arbeitsplatz.getDepartmentId())
                    .append("reservierungAnfang", serienAnfang)
                    .append("reservierungEnde", serienEnde)
                    .append("status", "reserviert")
            );
        }
        db.getCollection("reservierungen").insertMany(neueReservierungen);
        return toReservationDto(db, neueReservierungen.get(0));
    }

    public List<ReservierungDTO> getReservations(String benutzerId) {
        benutzerZugriff.getUser(benutzerId);
        MongoDatabase db = getDatabase();
        List<Document> dokumente = db
            .getCollection("reservierungen")
            .find(new Document("benutzerId", benutzerId))
            .sort(new Document("reservierungAnfang", 1))
            .into(new ArrayList<>());

        return dokumente
            .stream()
            .map(dokument -> toReservationDto(db, dokument))
            .toList();
    }

    public void cancelReservation(String id, String benutzerId) {
        benutzerZugriff.getUser(benutzerId);
        Document filter = new Document("_id", id)
            .append("benutzerId", benutzerId)
            .append("status", "reserviert");

        Document reservierung = getDatabase().getCollection("reservierungen").find(filter).first();
        if (reservierung == null) {
            throw new NotFoundException("Reservierung wurde nicht gefunden.");
        }

        getDatabase()
            .getCollection("reservierungen")
            .updateOne(filter, new Document("$set", new Document("status", "storniert")));
    }

    public ReservierungDTO updateReservation(String id, ReservierungRequestDTO request) {
        benutzerZugriff.getUser(request.benutzerId());
        Date anfang = parseDateTime(request.reservierungAnfang());
        Date ende = parseDateTime(request.reservierungEnde());
        validateTimeRange(anfang, ende);
        einstellungRepo.validateBookingLimit(anfang, ende);
        MongoDatabase db = getDatabase();
        Document filter = new Document("_id", id)
            .append("benutzerId", request.benutzerId())
            .append("status", "reserviert");
        Document reservierung = db.getCollection("reservierungen").find(filter).first();
        if (reservierung == null) {
            throw new NotFoundException("Aktive Reservierung wurde nicht gefunden.");
        }
        Document konflikt = db
            .getCollection("reservierungen")
            .find(
                new Document("_id", new Document("$ne", id))
                    .append("arbeitsplatzId", reservierung.getString("arbeitsplatzId"))
                    .append("status", "reserviert")
                    .append("reservierungAnfang", new Document("$lt", ende))
                    .append("reservierungEnde", new Document("$gt", anfang))
            )
            .first();
        if (konflikt != null) {
            throw new BadRequestException(
                "Der geänderte Zeitraum überschneidet eine Reservierung."
            );
        }
        validateUserAvailability(db, request.benutzerId(), anfang, ende, id);
        db.getCollection("reservierungen").updateOne(
            filter,
            new Document(
                "$set",
                new Document("reservierungAnfang", anfang).append("reservierungEnde", ende)
            )
        );
        reservierung.put("reservierungAnfang", anfang);
        reservierung.put("reservierungEnde", ende);
        return toReservationDto(db, reservierung);
    }

    public void cancelReservationAsAdmin(String adminId, String id, NameRequestDTO request) {
        BenutzerDTO admin = benutzerZugriff.requireAdmin(adminId);
        Document reservierung = getDatabase()
            .getCollection("reservierungen")
            .find(new Document("_id", id))
            .first();
        if (reservierung == null) {
            throw new NotFoundException("Aktive Reservierung wurde nicht gefunden.");
        }
        if (
            !isSuperadmin(admin) &&
            !admin.abteilungId().equals(reservierung.getString("abteilungId"))
        ) {
            throw new BadRequestException(
                "Abteilungsleitungen verwalten nur ihre eigene Abteilung."
            );
        }
        Document filter = new Document("_id", id).append("status", "reserviert");
        if (
            getDatabase()
                .getCollection("reservierungen")
                .updateOne(filter, new Document("$set", new Document("status", "storniert")))
                .getMatchedCount() == 0
        ) {
            throw new NotFoundException("Aktive Reservierung wurde nicht gefunden.");
        }
        String grund =
            request == null || isBlank(request.name())
                ? "Kein Grund angegeben"
                : request.name().trim();
        getDatabase()
            .getCollection("reservierungen")
            .updateOne(
                new Document("_id", id),
                new Document("$set", new Document("stornierungsgrund", grund))
            );
        auditRepo.recordAuditEntry(adminId, "RESERVIERUNG_STORNIERT", id + " · " + grund);
    }

    void applyReservationStatus(
        MongoDatabase db,
        List<Arbeitsplatz> arbeitsplaetze,
        String von,
        String bis
    ) {
        Date anfang = parseDateTime(von);
        Date ende = parseDateTime(bis);
        validateTimeRange(anfang, ende);

        List<Document> reservierungen = db
            .getCollection("reservierungen")
            .find(
                new Document("status", "reserviert")
                    .append("reservierungAnfang", new Document("$lt", ende))
                    .append("reservierungEnde", new Document("$gt", anfang))
            )
            .into(new ArrayList<>());
        Map<String, String> reserviertVon = new HashMap<>();
        Map<String, Document> reservierungsdaten = new HashMap<>();
        for (Document reservierung : reservierungen) {
            Document benutzer = db
                .getCollection("benutzer")
                .find(new Document("_id", reservierung.getString("benutzerId")))
                .first();
            if (benutzer != null) {
                reserviertVon.put(
                    reservierung.getString("arbeitsplatzId"),
                    benutzer.getString("vorname") + " " + benutzer.getString("nachname")
                );
                reservierungsdaten.put(reservierung.getString("arbeitsplatzId"), reservierung);
            }
        }

        for (Arbeitsplatz arbeitsplatz : arbeitsplaetze) {
            String name = reserviertVon.get(arbeitsplatz.getId());
            arbeitsplatz.setStatus(name == null ? "frei" : "reserviert");
            arbeitsplatz.setReservedByName(name == null ? "" : name);
            Document daten = reservierungsdaten.get(arbeitsplatz.getId());
            if (daten != null) {
                arbeitsplatz.setReservationUserId(daten.getString("benutzerId"));
                arbeitsplatz.setReservationStart(daten.getDate("reservierungAnfang"));
                arbeitsplatz.setReservationEnd(daten.getDate("reservierungEnde"));
            }

            Document naechsteReservierung = db
                .getCollection("reservierungen")
                .find(
                    new Document("arbeitsplatzId", arbeitsplatz.getId())
                        .append("status", "reserviert")
                        .append("reservierungAnfang", new Document("$gte", ende))
                )
                .sort(new Document("reservierungAnfang", 1))
                .first();
            if (naechsteReservierung != null) {
                arbeitsplatz.setNextReservationStart(
                    naechsteReservierung.getDate("reservierungAnfang")
                );
                arbeitsplatz.setNextReservationEnd(
                    naechsteReservierung.getDate("reservierungEnde")
                );
            }
        }
    }

    private void validateWorkstationAvailability(
        MongoDatabase db,
        String arbeitsplatzId,
        Date anfang,
        Date ende
    ) {
        Document konflikt = db
            .getCollection("reservierungen")
            .find(
                new Document("arbeitsplatzId", arbeitsplatzId)
                    .append("status", "reserviert")
                    .append("reservierungAnfang", new Document("$lt", ende))
                    .append("reservierungEnde", new Document("$gt", anfang))
            )
            .first();

        if (konflikt != null) {
            String datum = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                .withZone(ZoneId.of("Europe/Vienna"))
                .format(anfang.toInstant());
            throw new BadRequestException(
                "Dieser Arbeitsplatz ist am " + datum + " bereits reserviert."
            );
        }
    }

    private void validateUserAvailability(
        MongoDatabase db,
        String benutzerId,
        Date anfang,
        Date ende,
        String ausnahmeId
    ) {
        Document filter = new Document("benutzerId", benutzerId)
            .append("status", "reserviert")
            .append("reservierungAnfang", new Document("$lt", ende))
            .append("reservierungEnde", new Document("$gt", anfang));
        if (ausnahmeId != null) {
            filter.append("_id", new Document("$ne", ausnahmeId));
        }
        if (db.getCollection("reservierungen").find(filter).first() != null) {
            String datum = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                .withZone(ZoneId.of("Europe/Vienna"))
                .format(anfang.toInstant());
            throw new BadRequestException(
                "Am " + datum + " besteht bereits eine eigene Reservierung."
            );
        }
    }
}
