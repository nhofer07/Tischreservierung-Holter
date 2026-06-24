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

    private final Benutzer demoBenutzer = new Benutzer(
            "USER-DEMO",
            "Demo",
            "Benutzer",
            "demo@example.com",
            "angestellter",
            "ABT-IT",
            "IT",
            "RAUM-WELS-OG1-TEAM"
    );

    private final List<Standort> demoStandorte = List.of(
            new Standort("STANDORT-WELS", "HOLTER Wels", "Sengerstrasse 27", "Wels"),
            new Standort("STANDORT-LINZ", "HOLTER Linz", "Industriezeile 1", "Linz")
    );

    private final List<Arbeitsplatz> demoArbeitsplaetze = new ArrayList<>(List.of(
            arbeitsplatz("WELS-TISCH-101", 101, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 20, 34, "frei", "2 Monitore", "Dockingstation"),
            arbeitsplatz("WELS-TISCH-102", 102, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 40, 34, "frei", "Monitor", "Dockingstation", "Hoehenverstellbarer Tisch"),
            arbeitsplatz("WELS-TISCH-103", 103, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 60, 34, "frei", "2 Monitore", "Rollcontainer"),
            arbeitsplatz("WELS-TISCH-104", 104, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 80, 34, "frei", "Monitor"),
            arbeitsplatz("WELS-TISCH-105", 105, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 20, 58, "frei", "Hoehenverstellbarer Tisch", "Monitor", "Dockingstation"),
            arbeitsplatz("WELS-TISCH-106", 106, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 40, 58, "frei", "2 Monitore", "Dockingstation", "Hoehenverstellbarer Tisch"),
            arbeitsplatz("WELS-TISCH-107", 107, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 60, 58, "frei", "Monitor", "Kopfhoerer", "USB-C Hub"),
            arbeitsplatz("WELS-TISCH-108", 108, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT", 80, 58, "frei", "Monitor", "Dockingstation", "Hoehenverstellbarer Tisch"),

            arbeitsplatz("WELS-MKT-301", 301, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 18, 30, "frei", "2 Monitore", "Grafiktablett", "Dockingstation"),
            arbeitsplatz("WELS-MKT-302", 302, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 38, 30, "frei", "Monitor", "Hoehenverstellbarer Tisch"),
            arbeitsplatz("WELS-MKT-303", 303, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 58, 30, "frei", "2 Monitore", "Dockingstation"),
            arbeitsplatz("WELS-MKT-304", 304, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 78, 30, "frei", "Monitor", "Kopfhoerer"),
            arbeitsplatz("WELS-MKT-305", 305, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 20, 61, "frei", "2 Monitore", "Rollcontainer"),
            arbeitsplatz("WELS-MKT-306", 306, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 40, 61, "frei", "Monitor", "Dockingstation", "Hoehenverstellbarer Tisch"),
            arbeitsplatz("WELS-MKT-307", 307, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 60, 61, "frei", "2 Monitore", "Dockingstation"),
            arbeitsplatz("WELS-MKT-308", 308, "RAUM-WELS-EG-MARKETING", "STANDORT-WELS", "ABT-MARKETING", "Marketing", 80, 61, "frei", "Monitor", "USB-C Hub"),

            arbeitsplatz("LINZ-TISCH-201", 201, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 20, 29, "frei", "Monitor", "Dockingstation"),
            arbeitsplatz("LINZ-TISCH-202", 202, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 40, 29, "frei", "2 Monitore"),
            arbeitsplatz("LINZ-TISCH-203", 203, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 60, 29, "frei", "Monitor", "Rollcontainer", "Hoehenverstellbarer Tisch"),
            arbeitsplatz("LINZ-TISCH-204", 204, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 80, 29, "frei", "Hoehenverstellbarer Tisch", "Monitor", "Dockingstation"),
            arbeitsplatz("LINZ-TISCH-205", 205, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 20, 60, "frei", "2 Monitore", "Dockingstation"),
            arbeitsplatz("LINZ-TISCH-206", 206, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 40, 60, "frei", "Monitor", "Kopfhoerer"),
            arbeitsplatz("LINZ-TISCH-207", 207, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 60, 60, "frei", "Monitor", "Dockingstation", "Hoehenverstellbarer Tisch"),
            arbeitsplatz("LINZ-TISCH-208", 208, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT", 80, 60, "frei", "2 Monitore", "Dockingstation"),

            arbeitsplatz("LINZ-MKT-401", 401, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 20, 30, "frei", "2 Monitore", "Dockingstation", "Grafiktablett"),
            arbeitsplatz("LINZ-MKT-402", 402, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 40, 30, "frei", "Monitor", "Hoehenverstellbarer Tisch"),
            arbeitsplatz("LINZ-MKT-403", 403, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 60, 30, "frei", "2 Monitore", "Dockingstation"),
            arbeitsplatz("LINZ-MKT-404", 404, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 80, 30, "frei", "Monitor", "Kopfhoerer"),
            arbeitsplatz("LINZ-MKT-405", 405, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 20, 61, "frei", "Hoehenverstellbarer Tisch", "Monitor", "Dockingstation"),
            arbeitsplatz("LINZ-MKT-406", 406, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 40, 61, "frei", "2 Monitore", "Rollcontainer"),
            arbeitsplatz("LINZ-MKT-407", 407, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 60, 61, "frei", "Monitor", "USB-C Hub"),
            arbeitsplatz("LINZ-MKT-408", 408, "RAUM-LINZ-OG2-MARKETING", "STANDORT-LINZ", "ABT-MARKETING", "Marketing", 80, 61, "frei", "2 Monitore", "Dockingstation", "Hoehenverstellbarer Tisch")
    ));

    private Arbeitsplatz arbeitsplatz(
            String id,
            int tischnr,
            String raumId,
            String standortId,
            String abteilungId,
            String abteilungName,
            int x,
            int y,
            String status,
            String... equipment
    ) {
        List<Equipment> ausstattung = new ArrayList<>();
        ausstattung.add(new Equipment("Tastatur"));
        ausstattung.add(new Equipment("Maus"));

        for (String name : equipment) {
            ausstattung.add(new Equipment(name));
        }

        return new Arbeitsplatz(
                id,
                tischnr,
                raumId,
                standortId,
                abteilungId,
                abteilungName,
                abteilungName + " " + tischnr,
                status,
                new Position(x, y),
                ausstattung
        );
    }

    public BenutzerDTO getDemoBenutzer() {
        try {
            Document benutzer = database().getCollection("benutzer").find(new Document("_id", "USER-DEMO")).first();

            if (benutzer != null) {
                return toDTO(toBenutzer(benutzer));
            }
        } catch (Exception ignored) {
        }

        return toDTO(demoBenutzer);
    }

    public List<StandortDTO> getStandorte() {
        try {
            List<StandortDTO> standorteAusDb = database()
                    .getCollection("standorte")
                    .find()
                    .map(this::toStandort)
                    .map(this::toDTO)
                    .into(new ArrayList<>());

            if (!standorteAusDb.isEmpty()) {
                return standorteAusDb;
            }
        } catch (Exception ignored) {
        }

        return demoStandorte.stream().map(this::toDTO).toList();
    }

    public RaumDTO getRaum(String raumId, String von, String bis) {
        try {
            MongoDatabase db = database();
            Document raumDocument = db.getCollection("raeume").find(new Document("_id", raumId)).first();

            if (raumDocument != null) {
                List<Arbeitsplatz> arbeitsplaetze = db.getCollection("arbeitsplaetze")
                        .find(new Document("raumId", raumId))
                        .map(this::toArbeitsplatz)
                        .into(new ArrayList<>());

                if (arbeitsplaetze.size() >= 8) {
                    setzeReservierungsstatus(db, arbeitsplaetze, von, bis);
                    return toDTO(toRaum(raumDocument, arbeitsplaetze));
                }
            }
        } catch (Exception ignored) {
        }

        return toDTO(demoRaum(raumId));
    }

    public List<RaumAuswahlDTO> getRaeumeByStandort(String standortId) {
        try {
            List<RaumAuswahlDTO> raeumeAusDb = database()
                    .getCollection("raeume")
                    .find(new Document("standortId", standortId))
                    .map(this::toRaumAuswahlDTO)
                    .into(new ArrayList<>());

            if (!raeumeAusDb.isEmpty()) {
                return raeumeAusDb;
            }
        } catch (Exception ignored) {
        }

        return demoRaeume().stream()
                .filter(raum -> raum.getStandortId().equals(standortId))
                .map(this::toAuswahlDTO)
                .toList();
    }

    private Raum demoRaum(String raumId) {
        return switch (raumId) {
            case "RAUM-WELS-OG1-TEAM" -> new Raum(
                    raumId, "Teamraum Wels IT", "1. OG", "STANDORT-WELS", "ABT-IT", "IT",
                    demoArbeitsplaetze.stream().filter(a -> a.getRaumId().equals(raumId)).toList()
            );
            case "RAUM-WELS-EG-MARKETING" -> new Raum(
                    raumId, "Marketingraum Wels", "EG", "STANDORT-WELS", "ABT-MARKETING", "Marketing",
                    demoArbeitsplaetze.stream().filter(a -> a.getRaumId().equals(raumId)).toList()
            );
            case "RAUM-LINZ-EG-PROJEKT" -> new Raum(
                    raumId, "Projektraum Linz IT", "EG", "STANDORT-LINZ", "ABT-IT", "IT",
                    demoArbeitsplaetze.stream().filter(a -> a.getRaumId().equals(raumId)).toList()
            );
            case "RAUM-LINZ-OG2-MARKETING" -> new Raum(
                    raumId, "Marketingraum Linz", "2. OG", "STANDORT-LINZ", "ABT-MARKETING", "Marketing",
                    demoArbeitsplaetze.stream().filter(a -> a.getRaumId().equals(raumId)).toList()
            );
            default -> throw new NotFoundException();
        };
    }

    private List<Raum> demoRaeume() {
        return List.of(
                demoRaum("RAUM-WELS-OG1-TEAM"),
                demoRaum("RAUM-WELS-EG-MARKETING"),
                demoRaum("RAUM-LINZ-EG-PROJEKT"),
                demoRaum("RAUM-LINZ-OG2-MARKETING")
        );
    }

    public ArbeitsplatzDTO reservieren(ReservierungRequestDTO reservierungRequestDTO) {
        Date anfang = parseDatum(reservierungRequestDTO.reservierungAnfang());
        Date ende = parseDatum(reservierungRequestDTO.reservierungEnde());
        pruefeZeitraum(anfang, ende);

        try {
            MongoDatabase db = database();
            MongoCollection<Document> arbeitsplatzCollection = db.getCollection("arbeitsplaetze");
            Document arbeitsplatzDoc = arbeitsplatzCollection
                    .find(new Document("_id", reservierungRequestDTO.arbeitsplatzId()))
                    .first();

            if (arbeitsplatzDoc != null) {
                Arbeitsplatz arbeitsplatz = toArbeitsplatz(arbeitsplatzDoc);
                BenutzerDTO benutzer = getDemoBenutzer();

                pruefeReservierung(arbeitsplatz, benutzer.abteilungId());
                pruefeReservierungskonflikt(db, arbeitsplatz.getId(), anfang, ende);

                db.getCollection("reservierungen").insertOne(new Document()
                        .append("_id", "RES-" + System.currentTimeMillis())
                        .append("benutzerId", benutzer.id())
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
        } catch (BadRequestException | NotFoundException e) {
            throw e;
        } catch (Exception ignored) {
        }

        Arbeitsplatz arbeitsplatz = demoArbeitsplaetze.stream()
                .filter(a -> a.getId().equals(reservierungRequestDTO.arbeitsplatzId()))
                .findFirst()
                .orElseThrow(NotFoundException::new);

        pruefeReservierung(arbeitsplatz, demoBenutzer.getAbteilungId());
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

    private void pruefeReservierung(Arbeitsplatz arbeitsplatz, String abteilungId) {
        if (!arbeitsplatz.getAbteilungId().equals(abteilungId)) {
            throw new BadRequestException("Nur Tische aus der eigenen Abteilung koennen reserviert werden.");
        }

        if (!"frei".equals(arbeitsplatz.getStatus())) {
            throw new BadRequestException("Dieser Arbeitsplatz ist nicht frei.");
        }
    }

    private MongoDatabase database() {
        return mongoClient.getDatabase("tischreservierung");
    }

    private Benutzer toBenutzer(Document doc) {
        return new Benutzer(
                doc.getString("_id"),
                doc.getString("vorname"),
                doc.getString("nachname"),
                doc.getString("email"),
                doc.getString("rolle"),
                doc.getString("abteilungId"),
                doc.getString("abteilungName"),
                doc.getString("bevorzugterRaumId")
        );
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

    private RaumAuswahlDTO toAuswahlDTO(Raum raum) {
        return new RaumAuswahlDTO(
                raum.getId(),
                raum.getName(),
                raum.getStandortId(),
                raum.getAbteilungName()
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

    private BenutzerDTO toDTO(Benutzer benutzer) {
        return new BenutzerDTO(
                benutzer.getId(),
                benutzer.getVorname(),
                benutzer.getNachname(),
                benutzer.getEmail(),
                benutzer.getRolle(),
                benutzer.getAbteilungId(),
                benutzer.getAbteilungName(),
                benutzer.getBevorzugterRaumId()
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
