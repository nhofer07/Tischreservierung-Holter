package at.htlleonding.repo;

import at.htlleonding.DTOs.*;
import at.htlleonding.model.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class ArbeitsplatzRepo {

    @Inject
    MongoClient mongoClient;

    public List<StandortDTO> getStandorte() {
        List<StandortDTO> standorteAusDb = database()
                .getCollection("standorte")
                .find()
                .map(this::toStandort)
                .map(this::toDTO)
                .into(new ArrayList<>());

        if (standorteAusDb.isEmpty()) {
            throw new NotFoundException("Keine Standorte in der Datenbank gefunden.");
        }

        return standorteAusDb;
    }

    public RaumDTO getRaum(String raumId, String von, String bis) {
        MongoDatabase db = database();
        Document raumDocument = db.getCollection("raeume").find(new Document("_id", raumId)).first();

        if (raumDocument == null) {
            throw new NotFoundException("Raum wurde nicht in der Datenbank gefunden.");
        }

        List<Arbeitsplatz> arbeitsplaetze = db.getCollection("arbeitsplaetze")
                .find(new Document("raumId", raumId))
                .map(this::toArbeitsplatz)
                .into(new ArrayList<>());

        if (arbeitsplaetze.isEmpty()) {
            throw new NotFoundException("Keine Arbeitsplaetze fuer diesen Raum gefunden.");
        }

        setzeReservierungsstatus(db, arbeitsplaetze, von, bis);
        return toDTO(toRaum(raumDocument, arbeitsplaetze));
    }

    public List<RaumAuswahlDTO> getRaeumeByStandort(String standortId) {
        List<RaumAuswahlDTO> raeumeAusDb = database()
                .getCollection("raeume")
                .find(new Document("standortId", standortId))
                .map(this::toRaumAuswahlDTO)
                .into(new ArrayList<>());

        if (raeumeAusDb.isEmpty()) {
            throw new NotFoundException("Keine Raeume fuer diesen Standort gefunden.");
        }

        return raeumeAusDb;
    }

    public ArbeitsplatzDTO reservieren(ReservierungRequestDTO reservierungRequestDTO) {
        Date anfang = parseDatum(reservierungRequestDTO.reservierungAnfang());
        Date ende = parseDatum(reservierungRequestDTO.reservierungEnde());
        pruefeZeitraum(anfang, ende);

        MongoDatabase db = database();
        MongoCollection<Document> arbeitsplatzCollection = db.getCollection("arbeitsplaetze");
        Document arbeitsplatzDoc = arbeitsplatzCollection
                .find(new Document("_id", reservierungRequestDTO.arbeitsplatzId()))
                .first();

        if (arbeitsplatzDoc == null) {
            throw new NotFoundException("Arbeitsplatz wurde nicht in der Datenbank gefunden.");
        }

        Arbeitsplatz arbeitsplatz = toArbeitsplatz(arbeitsplatzDoc);

        pruefeReservierung(arbeitsplatz);
        pruefeReservierungskonflikt(db, arbeitsplatz.getId(), anfang, ende);

        db.getCollection("reservierungen").insertOne(new Document()
                .append("_id", "RES-" + System.currentTimeMillis())
                .append("arbeitsplatzId", arbeitsplatz.getId())
                .append("raumId", arbeitsplatz.getRaumId())
                .append("standortId", arbeitsplatz.getStandortId())
                .append("abteilungId", arbeitsplatz.getAbteilungId())
                .append("reservierungAnfang", anfang)
                .append("reservierungEnde", ende)
                .append("status", "reserviert"));

        arbeitsplatz.setStatus("reserviert");
        return toDTO(arbeitsplatz);
    }

    private void setzeReservierungsstatus(MongoDatabase db, List<Arbeitsplatz> arbeitsplaetze, String von, String bis) {
        if (von == null || bis == null) {
            return;
        }

        Date anfang = parseDatum(von);
        Date ende = parseDatum(bis);
        pruefeZeitraum(anfang, ende);

        Set<String> reservierteArbeitsplaetze = new HashSet<>();
        db.getCollection("reservierungen")
                .find(new Document("status", "reserviert")
                        .append("reservierungAnfang", new Document("$lt", ende))
                        .append("reservierungEnde", new Document("$gt", anfang)))
                .map(doc -> doc.getString("arbeitsplatzId"))
                .into(reservierteArbeitsplaetze);

        for (Arbeitsplatz arbeitsplatz : arbeitsplaetze) {
            arbeitsplatz.setStatus(reservierteArbeitsplaetze.contains(arbeitsplatz.getId()) ? "reserviert" : "frei");
        }
    }

    private void pruefeReservierungskonflikt(MongoDatabase db, String arbeitsplatzId, Date anfang, Date ende) {
        Document konflikt = db.getCollection("reservierungen")
                .find(new Document("arbeitsplatzId", arbeitsplatzId)
                        .append("status", "reserviert")
                        .append("reservierungAnfang", new Document("$lt", ende))
                        .append("reservierungEnde", new Document("$gt", anfang)))
                .first();

        if (konflikt != null) {
            throw new BadRequestException("Dieser Arbeitsplatz ist im ausgewaehlten Zeitraum reserviert.");
        }
    }

    private Date parseDatum(String wert) {
        if (wert == null || wert.isBlank()) {
            throw new BadRequestException("Bitte einen gueltigen Zeitraum auswaehlen.");
        }

        return Date.from(Instant.parse(wert));
    }

    private void pruefeZeitraum(Date anfang, Date ende) {
        if (!anfang.before(ende)) {
            throw new BadRequestException("Das Ende muss nach dem Beginn liegen.");
        }
    }

    private void pruefeReservierung(Arbeitsplatz arbeitsplatz) {
        if (!"frei".equals(arbeitsplatz.getStatus())) {
            throw new BadRequestException("Dieser Arbeitsplatz ist nicht frei.");
        }
    }

    private MongoDatabase database() {
        return mongoClient.getDatabase("tischreservierung");
    }

    private Standort toStandort(Document doc) {
        return new Standort(
                doc.getString("_id"),
                doc.getString("name"),
                doc.getString("adresse"),
                doc.getString("ort")
        );
    }

    private Raum toRaum(Document doc, List<Arbeitsplatz> arbeitsplaetze) {
        return new Raum(
                doc.getString("_id"),
                doc.getString("name"),
                doc.getString("stockwerk"),
                doc.getString("standortId"),
                doc.getString("abteilungId"),
                doc.getString("abteilungName"),
                arbeitsplaetze
        );
    }

    private RaumAuswahlDTO toRaumAuswahlDTO(Document doc) {
        return new RaumAuswahlDTO(
                doc.getString("_id"),
                doc.getString("name"),
                doc.getString("standortId"),
                doc.getString("abteilungName")
        );
    }

    private Arbeitsplatz toArbeitsplatz(Document doc) {
        Document position = doc.get("position", Document.class);
        List<String> equipment = doc.getList("equipment", String.class, List.of());

        return new Arbeitsplatz(
                doc.getString("_id"),
                doc.getInteger("tischnr", 0),
                doc.getString("raumId"),
                doc.getString("standortId"),
                doc.getString("abteilungId"),
                doc.getString("abteilungName"),
                doc.getString("name"),
                doc.getString("status"),
                new Position(
                        position != null ? position.getInteger("x", 0) : 0,
                        position != null ? position.getInteger("y", 0) : 0
                ),
                equipment.stream().map(Equipment::new).toList()
        );
    }

    private StandortDTO toDTO(Standort standort) {
        return new StandortDTO(
                standort.getId(),
                standort.getName(),
                standort.getAdresse(),
                standort.getOrt()
        );
    }

    private RaumDTO toDTO(Raum raum) {
        return new RaumDTO(
                raum.getId(),
                raum.getName(),
                raum.getStockwerk(),
                raum.getStandortId(),
                raum.getAbteilungId(),
                raum.getAbteilungName(),
                raum.getArbeitsplaetze().stream().map(this::toDTO).toList()
        );
    }

    private ArbeitsplatzDTO toDTO(Arbeitsplatz arbeitsplatz) {
        return new ArbeitsplatzDTO(
                arbeitsplatz.getId(),
                arbeitsplatz.getTischnr(),
                arbeitsplatz.getName(),
                arbeitsplatz.getStatus(),
                arbeitsplatz.getAbteilungId(),
                arbeitsplatz.getAbteilungName(),
                arbeitsplatz.getPosition().getX(),
                arbeitsplatz.getPosition().getY(),
                arbeitsplatz.getEquipment().stream().map(e -> new EquipmentDTO(e.getName())).toList()
        );
    }
}
