package at.htlleonding.repo;

import at.htlleonding.DTOs.ArbeitsplatzDTO;
import at.htlleonding.DTOs.BenutzerDTO;
import at.htlleonding.DTOs.EquipmentDTO;
import at.htlleonding.DTOs.RaumAuswahlDTO;
import at.htlleonding.DTOs.RaumDTO;
import at.htlleonding.DTOs.RaumElementDTO;
import at.htlleonding.DTOs.ReservierungDTO;
import at.htlleonding.DTOs.StandortDTO;
import at.htlleonding.model.Arbeitsplatz;
import at.htlleonding.model.Benutzer;
import at.htlleonding.model.Equipment;
import at.htlleonding.model.Position;
import at.htlleonding.model.Raum;
import at.htlleonding.model.RaumElement;
import at.htlleonding.model.Standort;
import com.mongodb.client.MongoDatabase;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import org.bson.Document;

/** Übersetzt MongoDB-Dokumente, Modelle und DTOs ohne eigene Datenänderungen. */
public final class RepositoryMapper {

    private RepositoryMapper() {}

    static String getDeskNumber(Document dokument) {
        Object wert = dokument.get("tischnr");
        return wert == null ? "" : String.valueOf(wert);
    }

    static Benutzer toUser(Document dokument) {
        return new Benutzer(
            dokument.getString("_id"),
            dokument.getString("vorname"),
            dokument.getString("nachname"),
            dokument.getString("email"),
            dokument.getString("rolle"),
            dokument.getString("abteilungId"),
            dokument.getString("abteilungName"),
            dokument.getString("bevorzugterRaumId"),
            dokument.getBoolean("aktiv", true)
        );
    }

    static Standort toLocation(Document dokument) {
        return new Standort(
            dokument.getString("_id"),
            dokument.getString("name"),
            dokument.getString("adresse"),
            dokument.getString("ort")
        );
    }

    static Raum toRoom(Document dokument, List<Arbeitsplatz> arbeitsplaetze) {
        List<Document> elementDokumente = dokument.getList("elemente", Document.class, List.of());
        List<RaumElement> elemente = elementDokumente
            .stream()
            .map(element ->
                new RaumElement(
                    element.getString("id"),
                    element.getString("typ"),
                    getNumericValue(element, "x"),
                    getNumericValue(element, "y"),
                    getNumericValue(element, "breite"),
                    getNumericValue(element, "hoehe"),
                    element.getString("text"),
                    getNumericValue(element, "rotation")
                )
            )
            .toList();
        return new Raum(
            dokument.getString("_id"),
            dokument.getString("name"),
            dokument.getString("stockwerk"),
            dokument.getString("standortId"),
            dokument.getString("abteilungId"),
            dokument.getString("abteilungName"),
            elemente,
            arbeitsplaetze
        );
    }

    static double getNumericValue(Document dokument, String feld) {
        Number wert = dokument.get(feld, Number.class);
        return wert == null ? 0 : wert.doubleValue();
    }

    static Arbeitsplatz toWorkstation(Document dokument) {
        Document position = dokument.get("position", Document.class);
        List<String> equipment = dokument.getList("equipment", String.class, List.of());

        return new Arbeitsplatz(
            dokument.getString("_id"),
            getDeskNumber(dokument),
            dokument.getString("raumId"),
            dokument.getString("standortId"),
            dokument.getString("abteilungId"),
            dokument.getString("abteilungName"),
            dokument.getString("name"),
            dokument.getString("status"),
            new Position(
                position != null ? position.getInteger("x", 0) : 0,
                position != null ? position.getInteger("y", 0) : 0
            ),
            equipment.stream().map(Equipment::new).toList(),
            getNumericValue(dokument, "rotation"),
            dokument.containsKey("breite") ? getNumericValue(dokument, "breite") : 12,
            dokument.containsKey("hoehe") ? getNumericValue(dokument, "hoehe") : 12
        );
    }

    static BenutzerDTO toUserDto(Benutzer benutzer) {
        return new BenutzerDTO(
            benutzer.getId(),
            benutzer.getFirstName(),
            benutzer.getLastName(),
            benutzer.getEmail(),
            benutzer.getRole(),
            benutzer.getDepartmentId(),
            benutzer.getDepartmentName(),
            benutzer.getPreferredRoomId(),
            benutzer.isActive()
        );
    }

    static StandortDTO toLocationDto(Standort standort) {
        return new StandortDTO(
            standort.getId(),
            standort.getName(),
            standort.getAddress(),
            standort.getCity()
        );
    }

    static RaumAuswahlDTO toRoomOptionDto(Document dokument) {
        return new RaumAuswahlDTO(
            dokument.getString("_id"),
            dokument.getString("name"),
            dokument.getString("standortId"),
            dokument.getString("abteilungName")
        );
    }

    static RaumDTO toRoomDto(Raum raum, BenutzerDTO benutzer) {
        return new RaumDTO(
            raum.getId(),
            raum.getName(),
            raum.getFloor(),
            raum.getLocationId(),
            raum.getDepartmentId(),
            raum.getDepartmentName(),
            raum
                .getElements()
                .stream()
                .map(element ->
                    new RaumElementDTO(
                        element.getId(),
                        element.getType(),
                        element.getX(),
                        element.getY(),
                        element.getWidth(),
                        element.getHeight(),
                        element.getText(),
                        element.getRotation()
                    )
                )
                .toList(),
            raum
                .getWorkstations()
                .stream()
                .map(arbeitsplatz -> toWorkstationDto(arbeitsplatz, benutzer))
                .toList()
        );
    }

    static ArbeitsplatzDTO toWorkstationDto(Arbeitsplatz arbeitsplatz, BenutzerDTO benutzer) {
        boolean eigeneAbteilung = arbeitsplatz.getDepartmentId().equals(benutzer.abteilungId());
        boolean reservierbar = eigeneAbteilung && "frei".equals(arbeitsplatz.getStatus());
        String hinweis = !eigeneAbteilung
            ? "Nur fuer Abteilung " + arbeitsplatz.getDepartmentName()
            : "reserviert".equals(arbeitsplatz.getStatus())
              ? "Im Zeitraum reserviert"
              : "";

        return new ArbeitsplatzDTO(
            arbeitsplatz.getId(),
            arbeitsplatz.getDeskNumber(),
            arbeitsplatz.getName(),
            arbeitsplatz.getStatus(),
            arbeitsplatz.getDepartmentId(),
            arbeitsplatz.getDepartmentName(),
            reservierbar,
            hinweis,
            arbeitsplatz.getReservedByName(),
            benutzer.id().equals(arbeitsplatz.getReservationUserId()),
            arbeitsplatz.getReservationStart() == null
                ? ""
                : arbeitsplatz.getReservationStart().toInstant().toString(),
            arbeitsplatz.getReservationEnd() == null
                ? ""
                : arbeitsplatz.getReservationEnd().toInstant().toString(),
            arbeitsplatz.getNextReservationStart() == null
                ? ""
                : arbeitsplatz.getNextReservationStart().toInstant().toString(),
            arbeitsplatz.getNextReservationEnd() == null
                ? ""
                : arbeitsplatz.getNextReservationEnd().toInstant().toString(),
            arbeitsplatz.getRotation(),
            arbeitsplatz.getWidth(),
            arbeitsplatz.getHeight(),
            arbeitsplatz.getPosition().getX(),
            arbeitsplatz.getPosition().getY(),
            arbeitsplatz
                .getEquipment()
                .stream()
                .map(equipment -> new EquipmentDTO(equipment.getName()))
                .toList()
        );
    }

    static ReservierungDTO toReservationDto(MongoDatabase db, Document reservierung) {
        Document arbeitsplatz = db
            .getCollection("arbeitsplaetze")
            .find(new Document("_id", reservierung.getString("arbeitsplatzId")))
            .first();
        Document raum = db
            .getCollection("raeume")
            .find(new Document("_id", reservierung.getString("raumId")))
            .first();
        Document standort = db
            .getCollection("standorte")
            .find(new Document("_id", reservierung.getString("standortId")))
            .first();
        Document benutzer = db
            .getCollection("benutzer")
            .find(new Document("_id", reservierung.getString("benutzerId")))
            .first();

        if (arbeitsplatz == null || raum == null || standort == null || benutzer == null) {
            throw new NotFoundException("Daten zur Reservierung sind unvollstaendig.");
        }

        return new ReservierungDTO(
            reservierung.getString("_id"),
            reservierung.getString("benutzerId"),
            benutzer.getString("vorname") + " " + benutzer.getString("nachname"),
            reservierung.getString("arbeitsplatzId"),
            getDeskNumber(arbeitsplatz),
            arbeitsplatz.getString("name"),
            reservierung.getString("raumId"),
            raum.getString("name"),
            reservierung.getString("standortId"),
            standort.getString("name"),
            reservierung.getDate("reservierungAnfang").toInstant().toString(),
            reservierung.getDate("reservierungEnde").toInstant().toString(),
            reservierung.getString("status"),
            reservierung.getString("stornierungsgrund")
        );
    }
}
