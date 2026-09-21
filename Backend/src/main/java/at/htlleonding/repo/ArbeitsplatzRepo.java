package at.htlleonding.repo;

import at.htlleonding.DTOs.*;
import at.htlleonding.model.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.bson.Document;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ArbeitsplatzRepo {

    @Inject
    MongoClient mongoClient;

    public List<BenutzerDTO> getBenutzer() {
        List<BenutzerDTO> benutzer = database()
                .getCollection("benutzer")
                .find()
                .sort(new Document("nachname", 1))
                .map(this::toBenutzer)
                .map(this::toDTO)
                .into(new ArrayList<>());

        if (benutzer.isEmpty()) {
            throw new NotFoundException("Keine Benutzer in der Datenbank gefunden.");
        }

        return benutzer;
    }

    public BenutzerDTO login(LoginRequestDTO request) {
        if (request == null || request.email() == null || request.passwort() == null) {
            throw new BadRequestException("E-Mail und Passwort sind erforderlich.");
        }

        Document dokument = database().getCollection("benutzer")
                .find(new Document("email", request.email().trim().toLowerCase()))
                .first();

        if (dokument == null || !hash(request.passwort()).equals(dokument.getString("passwortHash"))) {
            throw new BadRequestException("E-Mail oder Passwort ist falsch.");
        }

        if (!dokument.getBoolean("aktiv", true)) throw new BadRequestException("Dieses Benutzerkonto ist deaktiviert.");

        return toDTO(toBenutzer(dokument));
    }

    public RaumAuswahlDTO raumAnlegen(String adminId, RaumRequestDTO request) {
        BenutzerDTO admin = pruefeAdmin(adminId);
        pruefeRaum(request);
        pruefeAbteilung(admin, request.abteilungId());
        String id = "RAUM-" + System.currentTimeMillis();
        Document dokument = raumDocument(id, request);
        database().getCollection("raeume").insertOne(dokument);
        protokollieren(adminId, "RAUM_ANGELEGT", request.name());
        database().getCollection("benutzer").updateMany(
                new Document("abteilungId", request.abteilungId()).append("bevorzugterRaumId", ""),
                new Document("$set", new Document("bevorzugterRaumId", id)));
        return toRaumAuswahlDTO(dokument);
    }

    public List<AbteilungDTO> getAbteilungen(String adminId) {
        BenutzerDTO admin = pruefeAdmin(adminId);
        Document filter = istSuperadmin(admin) ? new Document() : new Document("_id", admin.abteilungId());
        return database().getCollection("abteilungen").find(filter).sort(new Document("name", 1))
                .map(dokument -> new AbteilungDTO(dokument.getString("_id"), dokument.getString("name")))
                .into(new ArrayList<>());
    }

    public AbteilungDTO abteilungAnlegen(String adminId, NameRequestDTO request) {
        pruefeSuperadmin(adminId);
        if (request == null || leer(request.name())) throw new BadRequestException("Name ist erforderlich.");
        String name = request.name().trim();
        String id = "ABT-" + name.toUpperCase().replaceAll("[^A-Z0-9]+", "-");
        Document dokument = new Document("_id", id).append("name", name);
        try {
            database().getCollection("abteilungen").insertOne(dokument);
        } catch (RuntimeException exception) {
            throw new BadRequestException("Diese Abteilung existiert bereits.");
        }
        String emailTeil = name.toLowerCase().replaceAll("[^a-z0-9]+", ".");
        database().getCollection("benutzer").insertOne(new Document("_id", "USER-" + id)
                .append("vorname", "Test")
                .append("nachname", name)
                .append("email", emailTeil + "@example.test")
                .append("passwortHash", hash("holter123"))
                .append("rolle", "USER")
                .append("aktiv", true)
                .append("abteilungId", id)
                .append("abteilungName", name)
                .append("bevorzugterRaumId", ""));
        protokollieren(adminId, "ABTEILUNG_ANGELEGT", name);
        return new AbteilungDTO(id, name);
    }

    public void abteilungLoeschen(String adminId, String id) {
        pruefeSuperadmin(adminId);
        if (database().getCollection("raeume").find(new Document("abteilungId", id)).first() != null) {
            throw new BadRequestException("Abteilung wird noch von Raeumen verwendet.");
        }
        if (database().getCollection("reservierungen").find(new Document("abteilungId", id)).first() != null) {
            throw new BadRequestException("Abteilung besitzt noch Reservierungen.");
        }
        database().getCollection("benutzer").deleteMany(new Document("abteilungId", id).append("rolle", "USER"));
        if (database().getCollection("abteilungen").deleteOne(new Document("_id", id)).getDeletedCount() == 0) {
            throw new NotFoundException("Abteilung wurde nicht gefunden.");
        }
        protokollieren(adminId, "ABTEILUNG_GELOESCHT", id);
    }

    public List<String> getEquipment(String adminId) {
        pruefeAdmin(adminId);
        return database().getCollection("equipment").find().sort(new Document("name", 1))
                .map(dokument -> dokument.getString("name")).into(new ArrayList<>());
    }

    public List<BenutzerDTO> getBenutzerAlsAdmin(String adminId) {
        BenutzerDTO admin = pruefeAdmin(adminId);
        if (istSuperadmin(admin)) return getBenutzer();
        return database().getCollection("benutzer").find(new Document("abteilungId", admin.abteilungId()))
                .sort(new Document("nachname", 1)).map(this::toBenutzer).map(this::toDTO).into(new ArrayList<>());
    }

    public BenutzerDTO benutzerAnlegen(String adminId, BenutzerRequestDTO request) {
        BenutzerDTO admin = pruefeAdmin(adminId);
        pruefeBenutzerRequest(request);
        pruefeBenutzerRechte(admin, request);
        if (database().getCollection("benutzer").find(new Document("email", request.email().trim().toLowerCase())).first() != null)
            throw new BadRequestException("Diese E-Mail-Adresse wird bereits verwendet.");
        String id = "USER-" + System.currentTimeMillis();
        Document dokument = benutzerDocument(id, request).append("passwortHash", hash("holter123"));
        database().getCollection("benutzer").insertOne(dokument);
        protokollieren(adminId, "BENUTZER_ANGELEGT", request.email());
        return toDTO(toBenutzer(dokument));
    }

    public BenutzerDTO benutzerAendern(String adminId, String id, BenutzerRequestDTO request) {
        BenutzerDTO admin = pruefeAdmin(adminId);
        pruefeBenutzerRequest(request);
        Document alt = database().getCollection("benutzer").find(new Document("_id", id)).first();
        if (alt == null) throw new NotFoundException("Benutzer wurde nicht gefunden.");
        pruefeAbteilung(admin, request.abteilungId());
        if (!istSuperadmin(admin) && !alt.getString("rolle").equals(request.rolle()))
            throw new BadRequestException("Nur Superadmins duerfen Adminrollen aendern.");
        Document dokument = benutzerDocument(id, request).append("passwortHash", alt.getString("passwortHash"));
        database().getCollection("benutzer").replaceOne(new Document("_id", id), dokument);
        protokollieren(adminId, "BENUTZER_GEAENDERT", request.email());
        return toDTO(toBenutzer(dokument));
    }

    public void passwortZuruecksetzen(String adminId, String id, PasswortRequestDTO request) {
        BenutzerDTO admin = pruefeAdmin(adminId);
        Document ziel = database().getCollection("benutzer").find(new Document("_id", id)).first();
        if (ziel == null) throw new NotFoundException("Benutzer wurde nicht gefunden.");
        if (!istSuperadmin(admin) && !admin.abteilungId().equals(ziel.getString("abteilungId")))
            throw new BadRequestException("Abteilungsleitungen verwalten nur ihre eigene Abteilung.");
        if (request == null || leer(request.passwort()) || request.passwort().length() < 6)
            throw new BadRequestException("Das Passwort muss mindestens sechs Zeichen haben.");
        if (database().getCollection("benutzer").updateOne(new Document("_id", id),
                new Document("$set", new Document("passwortHash", hash(request.passwort())))).getMatchedCount() == 0)
            throw new NotFoundException("Benutzer wurde nicht gefunden.");
        protokollieren(adminId, "PASSWORT_ZURUECKGESETZT", id);
    }

    public StandortDTO standortAnlegen(String adminId, StandortRequestDTO request) {
        pruefeSuperadmin(adminId); pruefeStandortRequest(request);
        String id = "STANDORT-" + System.currentTimeMillis();
        Document dokument = standortDocument(id, request);
        database().getCollection("standorte").insertOne(dokument);
        protokollieren(adminId, "STANDORT_ANGELEGT", request.name());
        return toDTO(toStandort(dokument));
    }

    public StandortDTO standortAendern(String adminId, String id, StandortRequestDTO request) {
        pruefeSuperadmin(adminId); pruefeStandortRequest(request);
        Document dokument = standortDocument(id, request);
        if (database().getCollection("standorte").replaceOne(new Document("_id", id), dokument).getMatchedCount() == 0)
            throw new NotFoundException("Standort wurde nicht gefunden.");
        protokollieren(adminId, "STANDORT_GEAENDERT", request.name());
        return toDTO(toStandort(dokument));
    }

    public void standortLoeschen(String adminId, String id) {
        pruefeSuperadmin(adminId);
        if (database().getCollection("raeume").find(new Document("standortId", id)).first() != null)
            throw new BadRequestException("Standort besitzt noch Räume.");
        if (database().getCollection("standorte").deleteOne(new Document("_id", id)).getDeletedCount() == 0)
            throw new NotFoundException("Standort wurde nicht gefunden.");
        protokollieren(adminId, "STANDORT_GELOESCHT", id);
    }

    public String equipmentAnlegen(String adminId, NameRequestDTO request) {
        pruefeSuperadmin(adminId);
        if (request == null || leer(request.name())) throw new BadRequestException("Name ist erforderlich.");
        String name = request.name().trim();
        if (database().getCollection("equipment").find(new Document("name", name)).first() != null) {
            throw new BadRequestException("Dieses Equipment existiert bereits.");
        }
        database().getCollection("equipment").insertOne(new Document("_id", "EQ-" + System.currentTimeMillis()).append("name", name));
        protokollieren(adminId, "EQUIPMENT_ANGELEGT", name);
        return name;
    }

    public void equipmentLoeschen(String adminId, String name) {
        pruefeSuperadmin(adminId);
        if (database().getCollection("arbeitsplaetze").find(new Document("equipment", name)).first() != null) {
            throw new BadRequestException("Equipment wird noch von Arbeitsplaetzen verwendet.");
        }
        database().getCollection("equipment").deleteOne(new Document("name", name));
        protokollieren(adminId, "EQUIPMENT_GELOESCHT", name);
    }

    public List<ReservierungDTO> getAlleReservierungen(String adminId) {
        BenutzerDTO admin = pruefeAdmin(adminId);
        MongoDatabase db = database();
        Document filter = istSuperadmin(admin) ? new Document() : new Document("abteilungId", admin.abteilungId());
        return db.getCollection("reservierungen").find(filter).sort(new Document("reservierungAnfang", -1))
                .map(dokument -> toReservierungDTO(db, dokument)).into(new ArrayList<>());
    }

    public RaumAuswahlDTO raumAendern(String adminId, String id, RaumRequestDTO request) {
        BenutzerDTO admin = pruefeAdmin(adminId);
        pruefeRaum(request);
        pruefeAbteilung(admin, request.abteilungId());
        Document dokument = raumDocument(id, request);
        if (database().getCollection("raeume").replaceOne(new Document("_id", id), dokument).getMatchedCount() == 0) {
            throw new NotFoundException("Raum wurde nicht gefunden.");
        }
        database().getCollection("arbeitsplaetze").updateMany(new Document("raumId", id),
                new Document("$set", new Document("standortId", request.standortId())
                        .append("abteilungId", request.abteilungId())
                        .append("abteilungName", request.abteilungName())));
        protokollieren(adminId, "RAUM_GEAENDERT", request.name());
        return toRaumAuswahlDTO(dokument);
    }

    public void raumLoeschen(String adminId, String id) {
        pruefeSuperadmin(adminId);
        if (database().getCollection("reservierungen")
                .find(new Document("raumId", id).append("status", "reserviert")
                        .append("reservierungEnde", new Document("$gt", new Date()))).first() != null) {
            throw new BadRequestException("Raum mit aktiven Reservierungen kann nicht geloescht werden.");
        }
        database().getCollection("reservierungen").deleteMany(new Document("raumId", id));
        database().getCollection("arbeitsplaetze").deleteMany(new Document("raumId", id));
        if (database().getCollection("raeume").deleteOne(new Document("_id", id)).getDeletedCount() == 0) {
            throw new NotFoundException("Raum wurde nicht gefunden.");
        }
        Document ersatz = database().getCollection("raeume").find().first();
        database().getCollection("benutzer").updateMany(new Document("bevorzugterRaumId", id),
                new Document("$set", new Document("bevorzugterRaumId", ersatz == null ? "" : ersatz.getString("_id"))));
        protokollieren(adminId, "RAUM_GELOESCHT", id);
    }

    public ArbeitsplatzDTO arbeitsplatzAnlegen(String adminId, ArbeitsplatzRequestDTO request) {
        BenutzerDTO admin = pruefeAdmin(adminId);
        pruefeArbeitsplatz(request);
        pruefeAbteilung(admin, request.abteilungId());
        String id = "TISCH-" + System.currentTimeMillis();
        Document dokument = arbeitsplatzDocument(id, request);
        database().getCollection("arbeitsplaetze").insertOne(dokument);
        protokollieren(adminId, "ARBEITSPLATZ_ANGELEGT", request.name());
        return toDTO(toArbeitsplatz(dokument), admin);
    }

    public ArbeitsplatzDTO arbeitsplatzAendern(String adminId, String id, ArbeitsplatzRequestDTO request) {
        BenutzerDTO admin = pruefeAdmin(adminId);
        pruefeArbeitsplatz(request);
        pruefeAbteilung(admin, request.abteilungId());
        Document dokument = arbeitsplatzDocument(id, request);
        if (database().getCollection("arbeitsplaetze").replaceOne(new Document("_id", id), dokument).getMatchedCount() == 0) {
            throw new NotFoundException("Arbeitsplatz wurde nicht gefunden.");
        }
        protokollieren(adminId, "ARBEITSPLATZ_GEAENDERT", request.name());
        return toDTO(toArbeitsplatz(dokument), admin);
    }

    public void arbeitsplatzLoeschen(String adminId, String id) {
        pruefeAdmin(adminId);
        if (database().getCollection("reservierungen")
                .find(new Document("arbeitsplatzId", id).append("status", "reserviert")).first() != null) {
            throw new BadRequestException("Arbeitsplatz mit aktiver Reservierung kann nicht geloescht werden.");
        }
        if (database().getCollection("arbeitsplaetze").deleteOne(new Document("_id", id)).getDeletedCount() == 0) {
            throw new NotFoundException("Arbeitsplatz wurde nicht gefunden.");
        }
        protokollieren(adminId, "ARBEITSPLATZ_GELOESCHT", id);
    }

    public List<StandortDTO> getStandorte() {
        List<StandortDTO> standorte = database()
                .getCollection("standorte")
                .find()
                .sort(new Document("ort", 1))
                .map(this::toStandort)
                .map(this::toDTO)
                .into(new ArrayList<>());

        if (standorte.isEmpty()) {
            throw new NotFoundException("Keine Standorte in der Datenbank gefunden.");
        }

        return standorte;
    }

    public RaumDTO getRaum(String raumId, String von, String bis, String benutzerId) {
        BenutzerDTO benutzer = getBenutzer(benutzerId);
        MongoDatabase db = database();
        Document raumDocument = db.getCollection("raeume").find(new Document("_id", raumId)).first();

        if (raumDocument == null) {
            throw new NotFoundException("Raum wurde nicht in der Datenbank gefunden.");
        }

        List<Arbeitsplatz> arbeitsplaetze = db.getCollection("arbeitsplaetze")
                .find(new Document("raumId", raumId))
                .sort(new Document("tischnr", 1))
                .map(this::toArbeitsplatz)
                .into(new ArrayList<>());

        setzeReservierungsstatus(db, arbeitsplaetze, von, bis);
        return toDTO(toRaum(raumDocument, arbeitsplaetze), benutzer);
    }

    public List<RaumAuswahlDTO> getRaeumeByStandort(String standortId) {
        List<RaumAuswahlDTO> raeume = database()
                .getCollection("raeume")
                .find(new Document("standortId", standortId))
                .sort(new Document("stockwerk", 1).append("name", 1))
                .map(this::toRaumAuswahlDTO)
                .into(new ArrayList<>());

        if (raeume.isEmpty()) {
            throw new NotFoundException("Keine Raeume fuer diesen Standort gefunden.");
        }

        return raeume;
    }

    public ReservierungDTO reservieren(ReservierungRequestDTO request) {
        BenutzerDTO benutzer = getBenutzer(request.benutzerId());
        Date anfang = parseDatum(request.reservierungAnfang());
        Date ende = parseDatum(request.reservierungEnde());
        pruefeZeitraum(anfang, ende);

        MongoDatabase db = database();
        Document arbeitsplatzDocument = db.getCollection("arbeitsplaetze")
                .find(new Document("_id", request.arbeitsplatzId()))
                .first();

        if (arbeitsplatzDocument == null) {
            throw new NotFoundException("Arbeitsplatz wurde nicht in der Datenbank gefunden.");
        }

        Arbeitsplatz arbeitsplatz = toArbeitsplatz(arbeitsplatzDocument);
        pruefeAbteilung(arbeitsplatz, benutzer);
        int anzahl = Math.max(1, Math.min(12, request.wiederholungen() + 1));
        List<Document> neueReservierungen = new ArrayList<>();
        for (int index = 0; index < anzahl; index++) {
            Date serienAnfang = Date.from(anfang.toInstant().plusSeconds(604800L * index));
            Date serienEnde = Date.from(ende.toInstant().plusSeconds(604800L * index));
            pruefeReservierungskonflikt(db, arbeitsplatz.getId(), serienAnfang, serienEnde);
            pruefeBenutzerReservierungskonflikt(db, benutzer.id(), serienAnfang, serienEnde, null);
            neueReservierungen.add(new Document("_id", "RES-" + System.currentTimeMillis() + "-" + index)
                    .append("benutzerId", benutzer.id()).append("arbeitsplatzId", arbeitsplatz.getId())
                    .append("raumId", arbeitsplatz.getRaumId()).append("standortId", arbeitsplatz.getStandortId())
                    .append("abteilungId", arbeitsplatz.getAbteilungId()).append("reservierungAnfang", serienAnfang)
                    .append("reservierungEnde", serienEnde).append("status", "reserviert"));
        }
        db.getCollection("reservierungen").insertMany(neueReservierungen);
        return toReservierungDTO(db, neueReservierungen.get(0));
    }

    public List<ReservierungDTO> getReservierungen(String benutzerId) {
        getBenutzer(benutzerId);
        MongoDatabase db = database();
        List<Document> dokumente = db.getCollection("reservierungen")
                .find(new Document("benutzerId", benutzerId))
                .sort(new Document("reservierungAnfang", 1))
                .into(new ArrayList<>());

        return dokumente.stream()
                .map(dokument -> toReservierungDTO(db, dokument))
                .toList();
    }

    public void stornieren(String id, String benutzerId) {
        getBenutzer(benutzerId);
        Document filter = new Document("_id", id)
                .append("benutzerId", benutzerId)
                .append("status", "reserviert");

        Document reservierung = database().getCollection("reservierungen").find(filter).first();
        if (reservierung == null) {
            throw new NotFoundException("Reservierung wurde nicht gefunden.");
        }

        database().getCollection("reservierungen")
                .updateOne(filter, new Document("$set", new Document("status", "storniert")));
    }

    public ReservierungDTO reservierungAendern(String id, ReservierungRequestDTO request) {
        getBenutzer(request.benutzerId());
        Date anfang = parseDatum(request.reservierungAnfang()); Date ende = parseDatum(request.reservierungEnde());
        pruefeZeitraum(anfang, ende);
        MongoDatabase db = database();
        Document filter = new Document("_id", id).append("benutzerId", request.benutzerId()).append("status", "reserviert");
        Document reservierung = db.getCollection("reservierungen").find(filter).first();
        if (reservierung == null) throw new NotFoundException("Aktive Reservierung wurde nicht gefunden.");
        Document konflikt = db.getCollection("reservierungen").find(new Document("_id", new Document("$ne", id))
                .append("arbeitsplatzId", reservierung.getString("arbeitsplatzId")).append("status", "reserviert")
                .append("reservierungAnfang", new Document("$lt", ende)).append("reservierungEnde", new Document("$gt", anfang))).first();
        if (konflikt != null) throw new BadRequestException("Der geänderte Zeitraum überschneidet eine Reservierung.");
        pruefeBenutzerReservierungskonflikt(db, request.benutzerId(), anfang, ende, id);
        db.getCollection("reservierungen").updateOne(filter, new Document("$set", new Document("reservierungAnfang", anfang).append("reservierungEnde", ende)));
        reservierung.put("reservierungAnfang", anfang); reservierung.put("reservierungEnde", ende);
        return toReservierungDTO(db, reservierung);
    }

    public void reservierungAlsAdminStornieren(String adminId, String id, NameRequestDTO request) {
        BenutzerDTO admin = pruefeAdmin(adminId);
        Document reservierung = database().getCollection("reservierungen").find(new Document("_id", id)).first();
        if (reservierung == null) throw new NotFoundException("Aktive Reservierung wurde nicht gefunden.");
        if (!istSuperadmin(admin) && !admin.abteilungId().equals(reservierung.getString("abteilungId")))
            throw new BadRequestException("Abteilungsleitungen verwalten nur ihre eigene Abteilung.");
        Document filter = new Document("_id", id).append("status", "reserviert");
        if (database().getCollection("reservierungen")
                .updateOne(filter, new Document("$set", new Document("status", "storniert")))
                .getMatchedCount() == 0) {
            throw new NotFoundException("Aktive Reservierung wurde nicht gefunden.");
        }
        String grund = request == null || leer(request.name()) ? "Kein Grund angegeben" : request.name().trim();
        database().getCollection("reservierungen").updateOne(new Document("_id", id),
                new Document("$set", new Document("stornierungsgrund", grund)));
        protokollieren(adminId, "RESERVIERUNG_STORNIERT", id + " · " + grund);
    }

    public List<Document> getAuditLog(String adminId) {
        BenutzerDTO admin = pruefeAdmin(adminId);
        Document filter = istSuperadmin(admin) ? new Document() : new Document("adminId", admin.id());
        return database().getCollection("auditLog").find(filter).sort(new Document("zeitpunkt", -1)).limit(100).into(new ArrayList<>());
    }

    private BenutzerDTO getBenutzer(String id) {
        if (id == null || id.isBlank()) {
            throw new BadRequestException("Bitte einen Benutzer auswaehlen.");
        }

        Document dokument = database().getCollection("benutzer").find(new Document("_id", id)).first();
        if (dokument == null) {
            throw new NotFoundException("Benutzer wurde nicht gefunden.");
        }

        return toDTO(toBenutzer(dokument));
    }

    private BenutzerDTO pruefeAdmin(String id) {
        BenutzerDTO benutzer = getBenutzer(id);
        if (!"ADMIN".equals(benutzer.rolle()) && !"SUPERADMIN".equals(benutzer.rolle())) {
            throw new BadRequestException("Diese Funktion ist nur fuer Administratoren verfuegbar.");
        }
        return benutzer;
    }

    private BenutzerDTO pruefeSuperadmin(String id) {
        BenutzerDTO benutzer = pruefeAdmin(id);
        if (!istSuperadmin(benutzer)) throw new BadRequestException("Diese Funktion ist nur fuer Superadmins verfuegbar.");
        return benutzer;
    }

    private boolean istSuperadmin(BenutzerDTO benutzer) {
        return "SUPERADMIN".equals(benutzer.rolle());
    }

    private void pruefeAbteilung(BenutzerDTO admin, String abteilungId) {
        if (!istSuperadmin(admin) && !admin.abteilungId().equals(abteilungId))
            throw new BadRequestException("Abteilungsleitungen verwalten nur ihre eigene Abteilung.");
    }

    private void pruefeBenutzerRechte(BenutzerDTO admin, BenutzerRequestDTO request) {
        pruefeAbteilung(admin, request.abteilungId());
        if (!istSuperadmin(admin) && !"USER".equals(request.rolle()))
            throw new BadRequestException("Nur Superadmins duerfen Adminrollen vergeben.");
        if (!"USER".equals(request.rolle()) && !"ADMIN".equals(request.rolle()) && !"SUPERADMIN".equals(request.rolle()))
            throw new BadRequestException("Unbekannte Benutzerrolle.");
    }

    private void pruefeRaum(RaumRequestDTO request) {
        if (request == null || leer(request.name()) || leer(request.stockwerk()) || leer(request.standortId())
                || leer(request.abteilungId()) || leer(request.abteilungName())) {
            throw new BadRequestException("Bitte alle Raumdaten ausfuellen.");
        }
    }

    private void pruefeArbeitsplatz(ArbeitsplatzRequestDTO request) {
        if (request == null || leer(request.tischnr()) || request.tischnr().trim().length() > 12 || leer(request.name()) || leer(request.raumId())
                || leer(request.standortId()) || leer(request.abteilungId()) || leer(request.abteilungName())
                || request.x() < 8 || request.x() > 92 || request.y() < 12 || request.y() > 86
                || request.breite() < 6 || request.breite() > 24 || request.hoehe() < 6 || request.hoehe() > 24) {
            throw new BadRequestException("Arbeitsplatzdaten oder Position sind ungueltig.");
        }
    }

    private void pruefeBenutzerRequest(BenutzerRequestDTO request) {
        if (request == null || leer(request.vorname()) || leer(request.nachname()) || leer(request.email())
                || leer(request.rolle()) || leer(request.abteilungId()) || leer(request.abteilungName()))
            throw new BadRequestException("Bitte alle Benutzerdaten ausfüllen.");
    }

    private void pruefeStandortRequest(StandortRequestDTO request) {
        if (request == null || leer(request.name()) || leer(request.adresse()) || leer(request.ort()))
            throw new BadRequestException("Bitte alle Standortdaten ausfüllen.");
    }

    private boolean leer(String wert) {
        return wert == null || wert.isBlank();
    }

    private Document raumDocument(String id, RaumRequestDTO request) {
        return new Document("_id", id)
                .append("name", request.name().trim())
                .append("stockwerk", request.stockwerk().trim())
                .append("standortId", request.standortId())
                .append("abteilungId", request.abteilungId())
                .append("abteilungName", request.abteilungName().trim())
                .append("elemente", request.elemente() == null ? List.of() : request.elemente().stream()
                        .map(element -> new Document("id", element.id())
                                .append("typ", element.typ())
                                .append("x", element.x())
                                .append("y", element.y())
                                .append("breite", element.breite())
                                .append("hoehe", element.hoehe())
                                .append("text", element.text())
                                .append("rotation", element.rotation()))
                        .toList())
                .append("beschreibung", "Im Room Builder verwalteter Raum");
    }

    private Document arbeitsplatzDocument(String id, ArbeitsplatzRequestDTO request) {
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

    private Document benutzerDocument(String id, BenutzerRequestDTO request) {
        return new Document("_id", id).append("vorname", request.vorname().trim())
                .append("nachname", request.nachname().trim()).append("email", request.email().trim().toLowerCase())
                .append("rolle", request.rolle()).append("abteilungId", request.abteilungId())
                .append("abteilungName", request.abteilungName()).append("bevorzugterRaumId", request.bevorzugterRaumId())
                .append("aktiv", request.aktiv());
    }

    private Document standortDocument(String id, StandortRequestDTO request) {
        return new Document("_id", id).append("name", request.name().trim())
                .append("adresse", request.adresse().trim()).append("ort", request.ort().trim());
    }

    private String hash(String passwort) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(passwort.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 ist nicht verfuegbar.", exception);
        }
    }

    private void setzeReservierungsstatus(MongoDatabase db, List<Arbeitsplatz> arbeitsplaetze, String von, String bis) {
        Date anfang = parseDatum(von);
        Date ende = parseDatum(bis);
        pruefeZeitraum(anfang, ende);

        List<Document> reservierungen = db.getCollection("reservierungen")
                .find(new Document("status", "reserviert")
                        .append("reservierungAnfang", new Document("$lt", ende))
                        .append("reservierungEnde", new Document("$gt", anfang)))
                .into(new ArrayList<>());
        Map<String, String> reserviertVon = new HashMap<>();
        Map<String, Document> reservierungsdaten = new HashMap<>();
        for (Document reservierung : reservierungen) {
            Document benutzer = db.getCollection("benutzer")
                    .find(new Document("_id", reservierung.getString("benutzerId"))).first();
            if (benutzer != null) {
                reserviertVon.put(reservierung.getString("arbeitsplatzId"),
                        benutzer.getString("vorname") + " " + benutzer.getString("nachname"));
                reservierungsdaten.put(reservierung.getString("arbeitsplatzId"), reservierung);
            }
        }

        for (Arbeitsplatz arbeitsplatz : arbeitsplaetze) {
            String name = reserviertVon.get(arbeitsplatz.getId());
            arbeitsplatz.setStatus(name == null ? "frei" : "reserviert");
            arbeitsplatz.setReserviertVon(name == null ? "" : name);
            Document daten = reservierungsdaten.get(arbeitsplatz.getId());
            if (daten != null) {
                arbeitsplatz.setReservierungBenutzerId(daten.getString("benutzerId"));
                arbeitsplatz.setReservierungAnfang(daten.getDate("reservierungAnfang"));
                arbeitsplatz.setReservierungEnde(daten.getDate("reservierungEnde"));
            }
        }
    }

    private void pruefeAbteilung(Arbeitsplatz arbeitsplatz, BenutzerDTO benutzer) {
        if (!arbeitsplatz.getAbteilungId().equals(benutzer.abteilungId())) {
            throw new BadRequestException("Es koennen nur Arbeitsplaetze der eigenen Abteilung reserviert werden.");
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

    private void pruefeBenutzerReservierungskonflikt(MongoDatabase db, String benutzerId, Date anfang, Date ende, String ausnahmeId) {
        Document filter = new Document("benutzerId", benutzerId)
                .append("status", "reserviert")
                .append("reservierungAnfang", new Document("$lt", ende))
                .append("reservierungEnde", new Document("$gt", anfang));
        if (ausnahmeId != null) filter.append("_id", new Document("$ne", ausnahmeId));
        if (db.getCollection("reservierungen").find(filter).first() != null) {
            throw new BadRequestException("Ein Mitarbeiter kann im selben Zeitraum nur einen Arbeitsplatz reservieren.");
        }
    }

    private String tischnummer(Document dokument) {
        Object wert = dokument.get("tischnr");
        return wert == null ? "" : String.valueOf(wert);
    }

    private Date parseDatum(String wert) {
        if (wert == null || wert.isBlank()) {
            throw new BadRequestException("Bitte einen gueltigen Zeitraum auswaehlen.");
        }

        try {
            return Date.from(Instant.parse(wert));
        } catch (DateTimeParseException exception) {
            throw new BadRequestException("Der Zeitraum hat ein ungueltiges Format.");
        }
    }

    private void pruefeZeitraum(Date anfang, Date ende) {
        if (!anfang.before(ende)) {
            throw new BadRequestException("Das Ende muss nach dem Beginn liegen.");
        }
    }

    private MongoDatabase database() {
        return mongoClient.getDatabase("tischreservierung");
    }

    private void protokollieren(String adminId, String aktion, String details) {
        database().getCollection("auditLog").insertOne(new Document("_id", "AUDIT-" + System.nanoTime())
                .append("adminId", adminId).append("aktion", aktion).append("details", details)
                .append("zeitpunkt", new Date()));
    }

    private Benutzer toBenutzer(Document dokument) {
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

    private Standort toStandort(Document dokument) {
        return new Standort(
                dokument.getString("_id"),
                dokument.getString("name"),
                dokument.getString("adresse"),
                dokument.getString("ort")
        );
    }

    private Raum toRaum(Document dokument, List<Arbeitsplatz> arbeitsplaetze) {
        List<Document> elementDokumente = dokument.getList("elemente", Document.class, List.of());
        List<RaumElement> elemente = elementDokumente.stream()
                .map(element -> new RaumElement(
                        element.getString("id"),
                        element.getString("typ"),
                        zahl(element, "x"),
                        zahl(element, "y"),
                        zahl(element, "breite"),
                        zahl(element, "hoehe"),
                        element.getString("text"),
                        zahl(element, "rotation")))
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

    private double zahl(Document dokument, String feld) {
        Number wert = dokument.get(feld, Number.class);
        return wert == null ? 0 : wert.doubleValue();
    }

    private Arbeitsplatz toArbeitsplatz(Document dokument) {
        Document position = dokument.get("position", Document.class);
        List<String> equipment = dokument.getList("equipment", String.class, List.of());

        return new Arbeitsplatz(
                dokument.getString("_id"),
                tischnummer(dokument),
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
                zahl(dokument, "rotation"),
                dokument.containsKey("breite") ? zahl(dokument, "breite") : 12,
                dokument.containsKey("hoehe") ? zahl(dokument, "hoehe") : 12
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
                benutzer.getBevorzugterRaumId(),
                benutzer.isAktiv()
        );
    }

    private StandortDTO toDTO(Standort standort) {
        return new StandortDTO(standort.getId(), standort.getName(), standort.getAdresse(), standort.getOrt());
    }

    private RaumAuswahlDTO toRaumAuswahlDTO(Document dokument) {
        return new RaumAuswahlDTO(
                dokument.getString("_id"),
                dokument.getString("name"),
                dokument.getString("standortId"),
                dokument.getString("abteilungName")
        );
    }

    private RaumDTO toDTO(Raum raum, BenutzerDTO benutzer) {
        return new RaumDTO(
                raum.getId(),
                raum.getName(),
                raum.getStockwerk(),
                raum.getStandortId(),
                raum.getAbteilungId(),
                raum.getAbteilungName(),
                raum.getElemente().stream().map(element -> new RaumElementDTO(
                        element.getId(), element.getTyp(), element.getX(), element.getY(),
                        element.getBreite(), element.getHoehe(), element.getText(), element.getRotation())).toList(),
                raum.getArbeitsplaetze().stream()
                        .map(arbeitsplatz -> toDTO(arbeitsplatz, benutzer))
                        .toList()
        );
    }

    private ArbeitsplatzDTO toDTO(Arbeitsplatz arbeitsplatz, BenutzerDTO benutzer) {
        boolean eigeneAbteilung = arbeitsplatz.getAbteilungId().equals(benutzer.abteilungId());
        boolean reservierbar = eigeneAbteilung && "frei".equals(arbeitsplatz.getStatus());
        String hinweis = !eigeneAbteilung
                ? "Nur fuer Abteilung " + arbeitsplatz.getAbteilungName()
                : "reserviert".equals(arbeitsplatz.getStatus()) ? "Im Zeitraum reserviert" : "";

        return new ArbeitsplatzDTO(
                arbeitsplatz.getId(),
                arbeitsplatz.getTischnr(),
                arbeitsplatz.getName(),
                arbeitsplatz.getStatus(),
                arbeitsplatz.getAbteilungId(),
                arbeitsplatz.getAbteilungName(),
                reservierbar,
                hinweis,
                arbeitsplatz.getReserviertVon(),
                benutzer.id().equals(arbeitsplatz.getReservierungBenutzerId()),
                arbeitsplatz.getReservierungAnfang() == null ? "" : arbeitsplatz.getReservierungAnfang().toInstant().toString(),
                arbeitsplatz.getReservierungEnde() == null ? "" : arbeitsplatz.getReservierungEnde().toInstant().toString(),
                arbeitsplatz.getRotation(),
                arbeitsplatz.getBreite(),
                arbeitsplatz.getHoehe(),
                arbeitsplatz.getPosition().getX(),
                arbeitsplatz.getPosition().getY(),
                arbeitsplatz.getEquipment().stream().map(equipment -> new EquipmentDTO(equipment.getName())).toList()
        );
    }

    private ReservierungDTO toReservierungDTO(MongoDatabase db, Document reservierung) {
        Document arbeitsplatz = db.getCollection("arbeitsplaetze")
                .find(new Document("_id", reservierung.getString("arbeitsplatzId"))).first();
        Document raum = db.getCollection("raeume")
                .find(new Document("_id", reservierung.getString("raumId"))).first();
        Document standort = db.getCollection("standorte")
                .find(new Document("_id", reservierung.getString("standortId"))).first();
        Document benutzer = db.getCollection("benutzer")
                .find(new Document("_id", reservierung.getString("benutzerId"))).first();

        if (arbeitsplatz == null || raum == null || standort == null || benutzer == null) {
            throw new NotFoundException("Daten zur Reservierung sind unvollstaendig.");
        }

        return new ReservierungDTO(
                reservierung.getString("_id"),
                reservierung.getString("benutzerId"),
                benutzer.getString("vorname") + " " + benutzer.getString("nachname"),
                reservierung.getString("arbeitsplatzId"),
                tischnummer(arbeitsplatz),
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
