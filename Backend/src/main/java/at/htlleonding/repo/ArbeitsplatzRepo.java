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

import java.util.ArrayList;
import java.util.List;

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
            new Arbeitsplatz("WELS-TISCH-101", 101, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT",
                    "Wels 101", "frei", new Position(22, 34),
                    List.of(new Equipment("2 Monitore"), new Equipment("Dockingstation"), new Equipment("Tastatur"), new Equipment("Maus"))),
            new Arbeitsplatz("WELS-TISCH-102", 102, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT",
                    "Wels 102", "reserviert", new Position(40, 34),
                    List.of(new Equipment("Monitor"), new Equipment("Dockingstation"))),
            new Arbeitsplatz("WELS-TISCH-103", 103, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT",
                    "Wels 103", "frei", new Position(58, 34),
                    List.of(new Equipment("2 Monitore"), new Equipment("Rollcontainer"))),
            new Arbeitsplatz("WELS-TISCH-104", 104, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT",
                    "Wels 104", "frei", new Position(76, 34),
                    List.of(new Equipment("Monitor"), new Equipment("Tastatur"), new Equipment("Maus"))),
            new Arbeitsplatz("WELS-TISCH-105", 105, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT",
                    "Wels 105", "frei", new Position(22, 64),
                    List.of(new Equipment("Hoehenverstellbarer Tisch"), new Equipment("Monitor"))),
            new Arbeitsplatz("WELS-TISCH-106", 106, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT",
                    "Wels 106", "reserviert", new Position(40, 64),
                    List.of(new Equipment("2 Monitore"), new Equipment("Dockingstation"))),
            new Arbeitsplatz("WELS-TISCH-107", 107, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT",
                    "Wels 107", "frei", new Position(58, 64),
                    List.of(new Equipment("Monitor"), new Equipment("Kopfhoerer"))),
            new Arbeitsplatz("WELS-TISCH-108", 108, "RAUM-WELS-OG1-TEAM", "STANDORT-WELS", "ABT-IT", "IT",
                    "Wels 108", "frei", new Position(76, 64),
                    List.of(new Equipment("Monitor"), new Equipment("Dockingstation"))),
            new Arbeitsplatz("LINZ-TISCH-201", 201, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT",
                    "Linz 201", "frei", new Position(24, 27),
                    List.of(new Equipment("Monitor"), new Equipment("Dockingstation"))),
            new Arbeitsplatz("LINZ-TISCH-202", 202, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT",
                    "Linz 202", "frei", new Position(24, 48),
                    List.of(new Equipment("2 Monitore"), new Equipment("Tastatur"), new Equipment("Maus"))),
            new Arbeitsplatz("LINZ-TISCH-203", 203, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT",
                    "Linz 203", "reserviert", new Position(24, 69),
                    List.of(new Equipment("Monitor"), new Equipment("Rollcontainer"))),
            new Arbeitsplatz("LINZ-TISCH-204", 204, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT",
                    "Linz 204", "frei", new Position(50, 27),
                    List.of(new Equipment("Hoehenverstellbarer Tisch"), new Equipment("Monitor"))),
            new Arbeitsplatz("LINZ-TISCH-205", 205, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT",
                    "Linz 205", "frei", new Position(50, 69),
                    List.of(new Equipment("2 Monitore"), new Equipment("Dockingstation"))),
            new Arbeitsplatz("LINZ-TISCH-206", 206, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT",
                    "Linz 206", "frei", new Position(76, 27),
                    List.of(new Equipment("Monitor"), new Equipment("Kopfhoerer"))),
            new Arbeitsplatz("LINZ-TISCH-207", 207, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT",
                    "Linz 207", "reserviert", new Position(76, 48),
                    List.of(new Equipment("Monitor"), new Equipment("Dockingstation"))),
            new Arbeitsplatz("LINZ-TISCH-208", 208, "RAUM-LINZ-EG-PROJEKT", "STANDORT-LINZ", "ABT-IT", "IT",
                    "Linz 208", "frei", new Position(76, 69),
                    List.of(new Equipment("2 Monitore"), new Equipment("Dockingstation")))
    ));

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

    public RaumDTO getRaum(String raumId) {
        try {
            MongoDatabase db = database();
            Document raumDocument = db.getCollection("raeume").find(new Document("_id", raumId)).first();

            if (raumDocument != null) {
                List<Arbeitsplatz> arbeitsplaetze = db.getCollection("arbeitsplaetze")
                        .find(new Document("raumId", raumId))
                        .map(this::toArbeitsplatz)
                        .into(new ArrayList<>());

                if (arbeitsplaetze.size() >= 8) {
                    return toDTO(toRaum(raumDocument, arbeitsplaetze));
                }
            }
        } catch (Exception ignored) {
        }

        if (!"RAUM-WELS-OG1-TEAM".equals(raumId) && !"RAUM-LINZ-EG-PROJEKT".equals(raumId)) {
            throw new NotFoundException();
        }

        if ("RAUM-LINZ-EG-PROJEKT".equals(raumId)) {
            return toDTO(new Raum(
                    "RAUM-LINZ-EG-PROJEKT",
                    "Projektraum Linz",
                    "EG",
                    "STANDORT-LINZ",
                    "ABT-IT",
                    "IT",
                    demoArbeitsplaetze.stream().filter(a -> a.getRaumId().equals(raumId)).toList()
            ));
        }

        return toDTO(new Raum(
                "RAUM-WELS-OG1-TEAM",
                "Teamraum Wels",
                "1. OG",
                "STANDORT-WELS",
                "ABT-IT",
                "IT",
                demoArbeitsplaetze.stream().filter(a -> a.getRaumId().equals(raumId)).toList()
        ));
    }

    public ArbeitsplatzDTO reservieren(ReservierungRequestDTO reservierungRequestDTO) {
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
                arbeitsplatz.setStatus("reserviert");

                arbeitsplatzCollection.updateOne(
                        new Document("_id", arbeitsplatz.getId()),
                        new Document("$set", new Document("status", "reserviert"))
                );

                db.getCollection("reservierungen").insertOne(new Document()
                        .append("_id", "RES-" + System.currentTimeMillis())
                        .append("benutzerId", benutzer.id())
                        .append("arbeitsplatzId", arbeitsplatz.getId())
                        .append("raumId", arbeitsplatz.getRaumId())
                        .append("standortId", arbeitsplatz.getStandortId())
                        .append("abteilungId", arbeitsplatz.getAbteilungId())
                        .append("status", "reserviert"));

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
