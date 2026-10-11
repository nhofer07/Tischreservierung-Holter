package at.htlleonding.repo;

import at.htlleonding.DTOs.*;
import at.htlleonding.model.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.bson.Document;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class ArbeitsplatzRepo {

    @Inject
    MongoClient mongoClient;

    @Inject
    SecurityIdentity securityIdentity;

    public FirmenEinstellungDTO getCompanySettings() {
        Document dokument = getDatabase().getCollection("einstellungen")
                .find(new Document("_id", "FIRMA")).first();
        if (dokument == null) {
            return new FirmenEinstellungDTO("HOLTER", "DeskVision", "#a51e2d", "#343638", "/holter-logo.png");
        }
        return new FirmenEinstellungDTO(
                dokument.getString("firmenname"), "DeskVision",
                dokument.getString("primaerfarbe"), dokument.getString("akzentfarbe"),
                dokument.getString("logoUrl"));
    }

    public void requireSuperadminAccess(String adminId) {
        requireSuperadmin(adminId);
    }

    public FirmenEinstellungDTO saveCompanySettings(String adminId, FirmenEinstellungDTO request) {
        requireSuperadmin(adminId);
        if (request == null || isBlank(request.firmenname())) {
            throw new BadRequestException("Der Firmenname ist erforderlich.");
        }
        if (!isValidHexColor(request.primaerfarbe()) || !isValidHexColor(request.akzentfarbe())) {
            throw new BadRequestException("Farben müssen als Hex-Wert wie #a51e2d angegeben werden.");
        }
        String logoUrl = isBlank(request.logoUrl()) ? "" : request.logoUrl().trim();
        Document werte = new Document("firmenname", request.firmenname().trim())
                .append("produktname", "DeskVision")
                .append("primaerfarbe", request.primaerfarbe().toLowerCase())
                .append("akzentfarbe", request.akzentfarbe().toLowerCase())
                .append("logoUrl", logoUrl);
        updateCompanySettings(werte);
        Document letzterEintrag = getDatabase().getCollection("designHistorie").find()
                .sort(new Document("zeitpunkt", -1)).first();
        if (!hasSameBranding(letzterEintrag, werte)) {
            Document historie = new Document("_id", "DESIGN-" + System.nanoTime())
                    .append("zeitpunkt", new Date());
            historie.putAll(werte);
            getDatabase().getCollection("designHistorie").insertOne(historie);
        }
        recordAuditEntry(adminId, "FIRMENDESIGN_GEAENDERT", request.firmenname().trim());
        return getCompanySettings();
    }

    public FirmenEinstellungDTO applyBrandingHistoryEntry(String adminId, String historieId) {
        requireSuperadmin(adminId);
        Document historie = getDatabase().getCollection("designHistorie")
                .find(new Document("_id", historieId)).first();
        if (historie == null) throw new NotFoundException("Designvariante wurde nicht gefunden.");
        Document werte = new Document("firmenname", historie.getString("firmenname"))
                .append("produktname", "DeskVision")
                .append("primaerfarbe", historie.getString("primaerfarbe"))
                .append("akzentfarbe", historie.getString("akzentfarbe"))
                .append("logoUrl", historie.getString("logoUrl"));
        updateCompanySettings(werte);
        recordAuditEntry(adminId, "FIRMENDESIGN_VERWENDET", historieId);
        return getCompanySettings();
    }

    public void deleteBrandingHistoryEntry(String adminId, String historieId) {
        requireSuperadmin(adminId);
        Document historie = getDatabase().getCollection("designHistorie")
                .find(new Document("_id", historieId)).first();
        if (historie == null) throw new NotFoundException("Designvariante wurde nicht gefunden.");
        Document gleichesDesign = new Document("firmenname", historie.getString("firmenname"))
                .append("primaerfarbe", historie.getString("primaerfarbe"))
                .append("akzentfarbe", historie.getString("akzentfarbe"))
                .append("logoUrl", historie.getString("logoUrl"));
        long geloescht = getDatabase().getCollection("designHistorie")
                .deleteMany(gleichesDesign).getDeletedCount();
        if (geloescht == 0) throw new NotFoundException("Designvariante wurde nicht gefunden.");
        recordAuditEntry(adminId, "FIRMENDESIGN_HISTORIE_GELOESCHT", historieId);
    }

    private void updateCompanySettings(Document werte) {
        getDatabase().getCollection("einstellungen").updateOne(
                new Document("_id", "FIRMA"), new Document("$set", werte),
                new com.mongodb.client.model.UpdateOptions().upsert(true));
    }

    private boolean hasSameBranding(Document links, Document rechts) {
        if (links == null) return false;
        return Objects.equals(links.getString("firmenname"), rechts.getString("firmenname"))
                && Objects.equals(links.getString("primaerfarbe"), rechts.getString("primaerfarbe"))
                && Objects.equals(links.getString("akzentfarbe"), rechts.getString("akzentfarbe"))
                && Objects.equals(links.getString("logoUrl"), rechts.getString("logoUrl"));
    }

    public List<FirmenDesignHistorieDTO> getBrandingHistory(String adminId) {
        requireSuperadmin(adminId);
        List<FirmenDesignHistorieDTO> ergebnis = new ArrayList<>();
        Set<String> bekannteDesigns = new HashSet<>();
        for (Document dokument : getDatabase().getCollection("designHistorie").find().sort(new Document("zeitpunkt", -1))) {
            String schluessel = dokument.getString("firmenname") + "|" + dokument.getString("primaerfarbe") + "|"
                    + dokument.getString("akzentfarbe") + "|" + dokument.getString("logoUrl");
            if (!bekannteDesigns.add(schluessel)) continue;
            ergebnis.add(new FirmenDesignHistorieDTO(
                    dokument.getString("_id"), dokument.getString("firmenname"), "DeskVision",
                    dokument.getString("primaerfarbe"), dokument.getString("akzentfarbe"),
                    dokument.getString("logoUrl"), dokument.getDate("zeitpunkt").toInstant().toString()));
            if (ergebnis.size() == 6) break;
        }
        return ergebnis;
    }

    private boolean isValidHexColor(String farbe) {
        return farbe != null && farbe.matches("#[0-9a-fA-F]{6}");
    }

    public List<BenutzerDTO> getUsers() {
        List<BenutzerDTO> benutzer = getDatabase()
                .getCollection("benutzer")
                .find(new Document("geloescht", new Document("$ne", true)))
                .sort(new Document("nachname", 1))
                .map(this::toUser)
                .map(this::toUserDto)
                .into(new ArrayList<>());

        if (benutzer.isEmpty()) {
            throw new NotFoundException("Keine Benutzer in der Datenbank gefunden.");
        }

        return benutzer;
    }

    public BenutzerDTO loginWithTestAccount(LoginRequestDTO request) {
        if (request == null || request.email() == null || request.passwort() == null) {
            throw new BadRequestException("E-Mail und Passwort sind erforderlich.");
        }

        Document dokument = getDatabase().getCollection("benutzer")
                .find(new Document("email", request.email().trim().toLowerCase()))
                .first();

        if (dokument == null || !hashPassword(request.passwort()).equals(dokument.getString("passwortHash"))) {
            throw new BadRequestException("E-Mail oder Passwort ist falsch.");
        }

        if (!dokument.getBoolean("aktiv", true)) throw new BadRequestException("Dieses Benutzerkonto ist deaktiviert.");

        return toUserDto(toUser(dokument));
    }

    public BenutzerDTO loginWithEntra(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Das Entra-Token enthält keine E-Mail-Adresse.");
        }

        Document dokument = findUserByEntraEmail(email);

        if (dokument == null) {
            throw new NotFoundException("Für dieses Microsoft-Konto wurde noch kein Mitarbeiterprofil angelegt.");
        }
        if (!dokument.getBoolean("aktiv", true)) {
            throw new BadRequestException("Dieses Benutzerkonto ist deaktiviert.");
        }
        return toUserDto(toUser(dokument));
    }

    public RaumAuswahlDTO createRoom(String adminId, RaumRequestDTO request) {
        BenutzerDTO admin = requireAdmin(adminId);
        validateRoomRequest(request);
        validateDepartmentAccess(admin, request.abteilungId());
        String id = "RAUM-" + System.currentTimeMillis();
        Document dokument = buildRoomDocument(id, request);
        getDatabase().getCollection("raeume").insertOne(dokument);
        recordAuditEntry(adminId, "RAUM_ANGELEGT", request.name());
        getDatabase().getCollection("benutzer").updateMany(
                new Document("abteilungId", request.abteilungId()).append("bevorzugterRaumId", ""),
                new Document("$set", new Document("bevorzugterRaumId", id)));
        return toRoomOptionDto(dokument);
    }

    public List<AbteilungDTO> getDepartments(String adminId) {
        BenutzerDTO admin = requireAdmin(adminId);
        Document filter = isSuperadmin(admin) ? new Document() : new Document("_id", admin.abteilungId());
        return getDatabase().getCollection("abteilungen").find(filter).sort(new Document("name", 1))
                .map(dokument -> new AbteilungDTO(dokument.getString("_id"), dokument.getString("name")))
                .into(new ArrayList<>());
    }

    public AbteilungDTO createDepartment(String adminId, NameRequestDTO request) {
        requireSuperadmin(adminId);
        if (request == null || isBlank(request.name())) throw new BadRequestException("Name ist erforderlich.");
        String name = request.name().trim();
        String id = "ABT-" + name.toUpperCase().replaceAll("[^A-Z0-9]+", "-");
        Document dokument = new Document("_id", id).append("name", name);
        try {
            getDatabase().getCollection("abteilungen").insertOne(dokument);
        } catch (RuntimeException exception) {
            throw new BadRequestException("Diese Abteilung existiert bereits.");
        }
        String emailTeil = name.toLowerCase().replaceAll("[^a-z0-9]+", ".");
        getDatabase().getCollection("benutzer").insertOne(new Document("_id", "USER-" + id)
                .append("vorname", "Test")
                .append("nachname", name)
                .append("email", emailTeil + "@example.test")
                .append("passwortHash", hashPassword("holter123"))
                .append("rolle", "USER")
                .append("aktiv", true)
                .append("abteilungId", id)
                .append("abteilungName", name)
                .append("bevorzugterRaumId", ""));
        recordAuditEntry(adminId, "ABTEILUNG_ANGELEGT", name);
        return new AbteilungDTO(id, name);
    }

    public void deleteDepartment(String adminId, String id) {
        requireSuperadmin(adminId);
        if (getDatabase().getCollection("raeume").find(new Document("abteilungId", id)).first() != null) {
            throw new BadRequestException("Abteilung wird noch von Raeumen verwendet.");
        }
        if (getDatabase().getCollection("reservierungen").find(new Document("abteilungId", id)).first() != null) {
            throw new BadRequestException("Abteilung besitzt noch Reservierungen.");
        }
        getDatabase().getCollection("benutzer").deleteMany(new Document("abteilungId", id).append("rolle", "USER"));
        if (getDatabase().getCollection("abteilungen").deleteOne(new Document("_id", id)).getDeletedCount() == 0) {
            throw new NotFoundException("Abteilung wurde nicht gefunden.");
        }
        recordAuditEntry(adminId, "ABTEILUNG_GELOESCHT", id);
    }

    public List<String> getEquipmentCatalog(String adminId) {
        requireAdmin(adminId);
        return getDatabase().getCollection("equipment").find().sort(new Document("name", 1))
                .map(dokument -> dokument.getString("name")).into(new ArrayList<>());
    }

    public List<BenutzerDTO> getManagedUsers(String adminId) {
        BenutzerDTO admin = requireAdmin(adminId);
        if (isSuperadmin(admin)) return getUsers();
        return getDatabase().getCollection("benutzer").find(new Document("abteilungId", admin.abteilungId())
                        .append("geloescht", new Document("$ne", true)))
                .sort(new Document("nachname", 1)).map(this::toUser).map(this::toUserDto).into(new ArrayList<>());
    }

    public BenutzerDTO createUser(String adminId, BenutzerRequestDTO request) {
        BenutzerDTO admin = requireAdmin(adminId);
        validateUserRequest(request);
        validateUserPermissions(admin, request);
        if (getDatabase().getCollection("benutzer").find(new Document("email", request.email().trim().toLowerCase())).first() != null)
            throw new BadRequestException("Diese E-Mail-Adresse wird bereits verwendet.");
        String id = "USER-" + System.currentTimeMillis();
        Document dokument = buildUserDocument(id, request).append("passwortHash", hashPassword("holter123"));
        getDatabase().getCollection("benutzer").insertOne(dokument);
        recordAuditEntry(adminId, "BENUTZER_ANGELEGT", request.email());
        return toUserDto(toUser(dokument));
    }

    public BenutzerDTO updateUser(String adminId, String id, BenutzerRequestDTO request) {
        BenutzerDTO admin = requireAdmin(adminId);
        validateUserRequest(request);
        Document alt = getDatabase().getCollection("benutzer").find(new Document("_id", id)).first();
        if (alt == null) throw new NotFoundException("Benutzer wurde nicht gefunden.");
        validateDepartmentAccess(admin, alt.getString("abteilungId"));
        validateDepartmentAccess(admin, request.abteilungId());
        if (alt.getBoolean("geloescht", false)) throw new BadRequestException("Gelöschte Benutzer können nicht bearbeitet werden.");
        if (adminId.equals(id) && !request.aktiv()) throw new BadRequestException("Das eigene Konto kann nicht deaktiviert werden.");
        if (!isSuperadmin(admin) && !alt.getString("rolle").equals(request.rolle()))
            throw new BadRequestException("Nur Superadmins duerfen Adminrollen aendern.");
        Document dokument = buildUserDocument(id, request).append("passwortHash", alt.getString("passwortHash"));
        if (alt.containsKey("entraEmail")) dokument.put("entraEmail", alt.getString("entraEmail"));
        getDatabase().getCollection("benutzer").replaceOne(new Document("_id", id), dokument);
        if (!request.aktiv()) deleteUserReservations(adminId, id);
        recordAuditEntry(adminId, "BENUTZER_GEAENDERT", request.email());
        return toUserDto(toUser(dokument));
    }

    public void deleteUser(String adminId, String id) {
        requireSuperadmin(adminId);
        Document benutzer = getDatabase().getCollection("benutzer").find(new Document("_id", id)).first();
        if (benutzer == null) throw new NotFoundException("Benutzer wurde nicht gefunden.");
        if (!"USER".equals(benutzer.getString("rolle"))) {
            throw new BadRequestException("Admin-Konten werden über die Rollenverwaltung geändert und nicht als Mitarbeiter gelöscht.");
        }
        getDatabase().getCollection("benutzer").updateOne(new Document("_id", id),
                new Document("$set", new Document("aktiv", false).append("geloescht", true)));
        deleteUserReservations(adminId, id);
        recordAuditEntry(adminId, "BENUTZER_GELOESCHT", benutzer.getString("email"));
    }

    public StandortDTO createLocation(String adminId, StandortRequestDTO request) {
        requireSuperadmin(adminId); validateLocationRequest(request);
        String id = "STANDORT-" + System.currentTimeMillis();
        Document dokument = buildLocationDocument(id, request);
        getDatabase().getCollection("standorte").insertOne(dokument);
        recordAuditEntry(adminId, "STANDORT_ANGELEGT", request.name());
        return toLocationDto(toLocation(dokument));
    }

    public StandortDTO updateLocation(String adminId, String id, StandortRequestDTO request) {
        requireSuperadmin(adminId); validateLocationRequest(request);
        Document dokument = buildLocationDocument(id, request);
        if (getDatabase().getCollection("standorte").replaceOne(new Document("_id", id), dokument).getMatchedCount() == 0)
            throw new NotFoundException("Standort wurde nicht gefunden.");
        recordAuditEntry(adminId, "STANDORT_GEAENDERT", request.name());
        return toLocationDto(toLocation(dokument));
    }

    public void deleteLocation(String adminId, String id) {
        requireSuperadmin(adminId);
        if (getDatabase().getCollection("raeume").find(new Document("standortId", id)).first() != null)
            throw new BadRequestException("Standort besitzt noch Räume.");
        if (getDatabase().getCollection("standorte").deleteOne(new Document("_id", id)).getDeletedCount() == 0)
            throw new NotFoundException("Standort wurde nicht gefunden.");
        recordAuditEntry(adminId, "STANDORT_GELOESCHT", id);
    }

    public String createEquipment(String adminId, NameRequestDTO request) {
        requireSuperadmin(adminId);
        if (request == null || isBlank(request.name())) throw new BadRequestException("Name ist erforderlich.");
        String name = request.name().trim();
        if (getDatabase().getCollection("equipment").find(new Document("name", name)).first() != null) {
            throw new BadRequestException("Dieses Equipment existiert bereits.");
        }
        getDatabase().getCollection("equipment").insertOne(new Document("_id", "EQ-" + System.currentTimeMillis()).append("name", name));
        recordAuditEntry(adminId, "EQUIPMENT_ANGELEGT", name);
        return name;
    }

    public void deleteEquipment(String adminId, String name) {
        requireSuperadmin(adminId);
        if (getDatabase().getCollection("arbeitsplaetze").find(new Document("equipment", name)).first() != null) {
            throw new BadRequestException("Equipment wird noch von Arbeitsplaetzen verwendet.");
        }
        getDatabase().getCollection("equipment").deleteOne(new Document("name", name));
        recordAuditEntry(adminId, "EQUIPMENT_GELOESCHT", name);
    }

    public List<ReservierungDTO> getAllReservations(String adminId) {
        BenutzerDTO admin = requireAdmin(adminId);
        MongoDatabase db = getDatabase();
        Document filter = isSuperadmin(admin) ? new Document() : new Document("abteilungId", admin.abteilungId());
        return db.getCollection("reservierungen").find(filter).sort(new Document("reservierungAnfang", -1))
                .map(dokument -> toReservationDto(db, dokument)).into(new ArrayList<>());
    }

    public RaumAuswahlDTO updateRoom(String adminId, String id, RaumRequestDTO request) {
        BenutzerDTO admin = requireAdmin(adminId);
        validateRoomRequest(request);
        validateDepartmentAccess(admin, request.abteilungId());
        Document dokument = buildRoomDocument(id, request);
        if (getDatabase().getCollection("raeume").replaceOne(new Document("_id", id), dokument).getMatchedCount() == 0) {
            throw new NotFoundException("Raum wurde nicht gefunden.");
        }
        getDatabase().getCollection("arbeitsplaetze").updateMany(new Document("raumId", id),
                new Document("$set", new Document("standortId", request.standortId())
                        .append("abteilungId", request.abteilungId())
                        .append("abteilungName", request.abteilungName())));
        recordAuditEntry(adminId, "RAUM_GEAENDERT", request.name());
        return toRoomOptionDto(dokument);
    }

    public void deleteRoom(String adminId, String id) {
        requireSuperadmin(adminId);
        if (getDatabase().getCollection("reservierungen")
                .find(new Document("raumId", id).append("status", "reserviert")
                        .append("reservierungEnde", new Document("$gt", new Date()))).first() != null) {
            throw new BadRequestException("Raum mit aktiven Reservierungen kann nicht geloescht werden.");
        }
        getDatabase().getCollection("reservierungen").deleteMany(new Document("raumId", id));
        getDatabase().getCollection("arbeitsplaetze").deleteMany(new Document("raumId", id));
        if (getDatabase().getCollection("raeume").deleteOne(new Document("_id", id)).getDeletedCount() == 0) {
            throw new NotFoundException("Raum wurde nicht gefunden.");
        }
        Document ersatz = getDatabase().getCollection("raeume").find().first();
        getDatabase().getCollection("benutzer").updateMany(new Document("bevorzugterRaumId", id),
                new Document("$set", new Document("bevorzugterRaumId", ersatz == null ? "" : ersatz.getString("_id"))));
        recordAuditEntry(adminId, "RAUM_GELOESCHT", id);
    }

    public ArbeitsplatzDTO createWorkstation(String adminId, ArbeitsplatzRequestDTO request) {
        BenutzerDTO admin = requireAdmin(adminId);
        validateWorkstationRequest(request);
        validateDepartmentAccess(admin, request.abteilungId());
        String id = "TISCH-" + System.currentTimeMillis();
        Document dokument = buildWorkstationDocument(id, request);
        getDatabase().getCollection("arbeitsplaetze").insertOne(dokument);
        recordAuditEntry(adminId, "ARBEITSPLATZ_ANGELEGT", request.name());
        return toWorkstationDto(toWorkstation(dokument), admin);
    }

    public ArbeitsplatzDTO updateWorkstation(String adminId, String id, ArbeitsplatzRequestDTO request) {
        BenutzerDTO admin = requireAdmin(adminId);
        validateWorkstationRequest(request);
        validateDepartmentAccess(admin, request.abteilungId());
        Document dokument = buildWorkstationDocument(id, request);
        if (getDatabase().getCollection("arbeitsplaetze").replaceOne(new Document("_id", id), dokument).getMatchedCount() == 0) {
            throw new NotFoundException("Arbeitsplatz wurde nicht gefunden.");
        }
        recordAuditEntry(adminId, "ARBEITSPLATZ_GEAENDERT", request.name());
        return toWorkstationDto(toWorkstation(dokument), admin);
    }

    public void deleteWorkstation(String adminId, String id) {
        requireAdmin(adminId);
        if (getDatabase().getCollection("reservierungen")
                .find(new Document("arbeitsplatzId", id).append("status", "reserviert")).first() != null) {
            throw new BadRequestException("Arbeitsplatz mit aktiver Reservierung kann nicht geloescht werden.");
        }
        if (getDatabase().getCollection("arbeitsplaetze").deleteOne(new Document("_id", id)).getDeletedCount() == 0) {
            throw new NotFoundException("Arbeitsplatz wurde nicht gefunden.");
        }
        recordAuditEntry(adminId, "ARBEITSPLATZ_GELOESCHT", id);
    }

    public List<StandortDTO> getLocations() {
        List<StandortDTO> standorte = getDatabase()
                .getCollection("standorte")
                .find()
                .sort(new Document("ort", 1))
                .map(this::toLocation)
                .map(this::toLocationDto)
                .into(new ArrayList<>());

        if (standorte.isEmpty()) {
            throw new NotFoundException("Keine Standorte in der Datenbank gefunden.");
        }

        return standorte;
    }

    public RaumDTO getRoom(String raumId, String von, String bis, String benutzerId) {
        BenutzerDTO benutzer = getUser(benutzerId);
        MongoDatabase db = getDatabase();
        Document raumDocument = db.getCollection("raeume").find(new Document("_id", raumId)).first();

        if (raumDocument == null) {
            throw new NotFoundException("Raum wurde nicht in der Datenbank gefunden.");
        }

        List<Arbeitsplatz> arbeitsplaetze = db.getCollection("arbeitsplaetze")
                .find(new Document("raumId", raumId))
                .sort(new Document("tischnr", 1))
                .map(this::toWorkstation)
                .into(new ArrayList<>());

        applyReservationStatus(db, arbeitsplaetze, von, bis);
        return toRoomDto(toRoom(raumDocument, arbeitsplaetze), benutzer);
    }

    public List<RaumAuswahlDTO> getRoomsByLocation(String standortId) {
        List<RaumAuswahlDTO> raeume = getDatabase()
                .getCollection("raeume")
                .find(new Document("standortId", standortId))
                .sort(new Document("stockwerk", 1).append("name", 1))
                .map(this::toRoomOptionDto)
                .into(new ArrayList<>());

        if (raeume.isEmpty()) {
            throw new NotFoundException("Keine Raeume fuer diesen Standort gefunden.");
        }

        return raeume;
    }

    public ReservierungDTO createReservation(ReservierungRequestDTO request) {
        BenutzerDTO benutzer = getUser(request.benutzerId());
        Date anfang = parseDateTime(request.reservierungAnfang());
        Date ende = parseDateTime(request.reservierungEnde());
        validateTimeRange(anfang, ende);
        validateBookingLimit(anfang, ende);

        MongoDatabase db = getDatabase();
        Document arbeitsplatzDocument = db.getCollection("arbeitsplaetze")
                .find(new Document("_id", request.arbeitsplatzId()))
                .first();

        if (arbeitsplatzDocument == null) {
            throw new NotFoundException("Arbeitsplatz wurde nicht in der Datenbank gefunden.");
        }

        Arbeitsplatz arbeitsplatz = toWorkstation(arbeitsplatzDocument);
        validateDepartmentAccess(arbeitsplatz, benutzer);
        if (request.wiederholungen() < 0 || request.wiederholungen() > 51)
            throw new BadRequestException("Die Anzahl der Wiederholungen muss zwischen 0 und 51 liegen.");
        int anzahl = request.wiederholungen() + 1;
        List<Document> neueReservierungen = new ArrayList<>();
        for (int index = 0; index < anzahl; index++) {
            Date serienAnfang = Date.from(anfang.toInstant().atZone(ZoneId.of("Europe/Vienna")).plusWeeks(index).toInstant());
            Date serienEnde = Date.from(ende.toInstant().atZone(ZoneId.of("Europe/Vienna")).plusWeeks(index).toInstant());
            validateBookingLimit(serienAnfang, serienEnde);
            validateWorkstationAvailability(db, arbeitsplatz.getId(), serienAnfang, serienEnde);
            validateUserAvailability(db, benutzer.id(), serienAnfang, serienEnde, null);
            neueReservierungen.add(new Document("_id", "RES-" + System.currentTimeMillis() + "-" + index)
                    .append("benutzerId", benutzer.id()).append("arbeitsplatzId", arbeitsplatz.getId())
                    .append("raumId", arbeitsplatz.getRoomId()).append("standortId", arbeitsplatz.getLocationId())
                    .append("abteilungId", arbeitsplatz.getDepartmentId()).append("reservierungAnfang", serienAnfang)
                    .append("reservierungEnde", serienEnde).append("status", "reserviert"));
        }
        db.getCollection("reservierungen").insertMany(neueReservierungen);
        return toReservationDto(db, neueReservierungen.get(0));
    }

    public List<ReservierungDTO> getReservations(String benutzerId) {
        getUser(benutzerId);
        MongoDatabase db = getDatabase();
        List<Document> dokumente = db.getCollection("reservierungen")
                .find(new Document("benutzerId", benutzerId))
                .sort(new Document("reservierungAnfang", 1))
                .into(new ArrayList<>());

        return dokumente.stream()
                .map(dokument -> toReservationDto(db, dokument))
                .toList();
    }

    public void cancelReservation(String id, String benutzerId) {
        getUser(benutzerId);
        Document filter = new Document("_id", id)
                .append("benutzerId", benutzerId)
                .append("status", "reserviert");

        Document reservierung = getDatabase().getCollection("reservierungen").find(filter).first();
        if (reservierung == null) {
            throw new NotFoundException("Reservierung wurde nicht gefunden.");
        }

        getDatabase().getCollection("reservierungen")
                .updateOne(filter, new Document("$set", new Document("status", "storniert")));
    }

    public ReservierungDTO updateReservation(String id, ReservierungRequestDTO request) {
        getUser(request.benutzerId());
        Date anfang = parseDateTime(request.reservierungAnfang()); Date ende = parseDateTime(request.reservierungEnde());
        validateTimeRange(anfang, ende);
        validateBookingLimit(anfang, ende);
        MongoDatabase db = getDatabase();
        Document filter = new Document("_id", id).append("benutzerId", request.benutzerId()).append("status", "reserviert");
        Document reservierung = db.getCollection("reservierungen").find(filter).first();
        if (reservierung == null) throw new NotFoundException("Aktive Reservierung wurde nicht gefunden.");
        Document konflikt = db.getCollection("reservierungen").find(new Document("_id", new Document("$ne", id))
                .append("arbeitsplatzId", reservierung.getString("arbeitsplatzId")).append("status", "reserviert")
                .append("reservierungAnfang", new Document("$lt", ende)).append("reservierungEnde", new Document("$gt", anfang))).first();
        if (konflikt != null) throw new BadRequestException("Der geänderte Zeitraum überschneidet eine Reservierung.");
        validateUserAvailability(db, request.benutzerId(), anfang, ende, id);
        db.getCollection("reservierungen").updateOne(filter, new Document("$set", new Document("reservierungAnfang", anfang).append("reservierungEnde", ende)));
        reservierung.put("reservierungAnfang", anfang); reservierung.put("reservierungEnde", ende);
        return toReservationDto(db, reservierung);
    }

    public void cancelReservationAsAdmin(String adminId, String id, NameRequestDTO request) {
        BenutzerDTO admin = requireAdmin(adminId);
        Document reservierung = getDatabase().getCollection("reservierungen").find(new Document("_id", id)).first();
        if (reservierung == null) throw new NotFoundException("Aktive Reservierung wurde nicht gefunden.");
        if (!isSuperadmin(admin) && !admin.abteilungId().equals(reservierung.getString("abteilungId")))
            throw new BadRequestException("Abteilungsleitungen verwalten nur ihre eigene Abteilung.");
        Document filter = new Document("_id", id).append("status", "reserviert");
        if (getDatabase().getCollection("reservierungen")
                .updateOne(filter, new Document("$set", new Document("status", "storniert")))
                .getMatchedCount() == 0) {
            throw new NotFoundException("Aktive Reservierung wurde nicht gefunden.");
        }
        String grund = request == null || isBlank(request.name()) ? "Kein Grund angegeben" : request.name().trim();
        getDatabase().getCollection("reservierungen").updateOne(new Document("_id", id),
                new Document("$set", new Document("stornierungsgrund", grund)));
        recordAuditEntry(adminId, "RESERVIERUNG_STORNIERT", id + " · " + grund);
    }

    public List<Document> getAuditLog(String adminId) {
        BenutzerDTO admin = requireAdmin(adminId);
        Document filter = isSuperadmin(admin) ? new Document() : new Document("adminId", admin.id());
        return getDatabase().getCollection("auditLog").find(filter).sort(new Document("zeitpunkt", -1)).limit(100).into(new ArrayList<>());
    }

    private BenutzerDTO getUser(String id) {
        if (id == null || id.isBlank()) {
            throw new BadRequestException("Bitte einen Benutzer auswaehlen.");
        }

        validateAuthenticatedIdentity(id);

        Document dokument = getDatabase().getCollection("benutzer").find(new Document("_id", id)).first();
        if (dokument == null) {
            throw new NotFoundException("Benutzer wurde nicht gefunden.");
        }

        if (!dokument.getBoolean("aktiv", true) || dokument.getBoolean("geloescht", false))
            throw new ForbiddenException("Dieses Benutzerkonto ist deaktiviert oder gelöscht.");
        return toUserDto(toUser(dokument));
    }

    private void validateAuthenticatedIdentity(String benutzerId) {
        if (securityIdentity == null || securityIdentity.isAnonymous()) return;
        if (!(securityIdentity.getPrincipal() instanceof JsonWebToken token)) {
            throw new ForbiddenException("Die angemeldete Identität ist ungültig.");
        }

        String email = token.getClaim("email");
        if (email == null || email.isBlank()) email = token.getClaim("preferred_username");
        if (email == null || email.isBlank()) email = token.getClaim("upn");
        Document konto = email == null ? null : findUserByEntraEmail(email);
        if (konto == null || !benutzerId.equals(konto.getString("_id"))) {
            throw new ForbiddenException("Die Benutzer-ID gehört nicht zum angemeldeten Microsoft-Konto.");
        }
    }

    private BenutzerDTO requireAdmin(String id) {
        BenutzerDTO benutzer = getUser(id);
        if (!"ADMIN".equals(benutzer.rolle()) && !"SUPERADMIN".equals(benutzer.rolle())) {
            throw new BadRequestException("Diese Funktion ist nur fuer Administratoren verfuegbar.");
        }
        return benutzer;
    }

    private Document findUserByEntraEmail(String email) {
        String normalisiert = email.trim().toLowerCase();
        Document filter = new Document("$or", List.of(
                new Document("email", normalisiert),
                new Document("entraEmail", normalisiert)
        ));
        return getDatabase().getCollection("benutzer").find(filter).first();
    }

    private BenutzerDTO requireSuperadmin(String id) {
        BenutzerDTO benutzer = requireAdmin(id);
        if (!isSuperadmin(benutzer)) throw new BadRequestException("Diese Funktion ist nur fuer Superadmins verfuegbar.");
        return benutzer;
    }

    private boolean isSuperadmin(BenutzerDTO benutzer) {
        return "SUPERADMIN".equals(benutzer.rolle());
    }

    private void validateDepartmentAccess(BenutzerDTO admin, String abteilungId) {
        if (!isSuperadmin(admin) && !admin.abteilungId().equals(abteilungId))
            throw new BadRequestException("Abteilungsleitungen verwalten nur ihre eigene Abteilung.");
    }

    private void validateUserPermissions(BenutzerDTO admin, BenutzerRequestDTO request) {
        validateDepartmentAccess(admin, request.abteilungId());
        if (!isSuperadmin(admin) && !"USER".equals(request.rolle()))
            throw new BadRequestException("Nur Superadmins duerfen Adminrollen vergeben.");
        if (!"USER".equals(request.rolle()) && !"ADMIN".equals(request.rolle()) && !"SUPERADMIN".equals(request.rolle()))
            throw new BadRequestException("Unbekannte Benutzerrolle.");
    }

    private void validateRoomRequest(RaumRequestDTO request) {
        if (request == null || isBlank(request.name()) || isBlank(request.stockwerk()) || isBlank(request.standortId())
                || isBlank(request.abteilungId()) || isBlank(request.abteilungName())) {
            throw new BadRequestException("Bitte alle Raumdaten ausfuellen.");
        }
    }

    private void validateWorkstationRequest(ArbeitsplatzRequestDTO request) {
        if (request == null || isBlank(request.tischnr()) || request.tischnr().trim().length() > 12 || isBlank(request.name()) || isBlank(request.raumId())
                || isBlank(request.standortId()) || isBlank(request.abteilungId()) || isBlank(request.abteilungName())
                || request.x() < 8 || request.x() > 92 || request.y() < 12 || request.y() > 86
                || request.breite() < 6 || request.breite() > 24 || request.hoehe() < 6 || request.hoehe() > 24) {
            throw new BadRequestException("Arbeitsplatzdaten oder Position sind ungueltig.");
        }
    }

    private void validateUserRequest(BenutzerRequestDTO request) {
        if (request == null || isBlank(request.vorname()) || isBlank(request.nachname()) || isBlank(request.email())
                || isBlank(request.rolle()) || isBlank(request.abteilungId()) || isBlank(request.abteilungName()))
            throw new BadRequestException("Bitte alle Benutzerdaten ausfüllen.");
    }

    private void validateLocationRequest(StandortRequestDTO request) {
        if (request == null || isBlank(request.name()) || isBlank(request.adresse()) || isBlank(request.ort()))
            throw new BadRequestException("Bitte alle Standortdaten ausfüllen.");
    }

    private boolean isBlank(String wert) {
        return wert == null || wert.isBlank();
    }

    private Document buildRoomDocument(String id, RaumRequestDTO request) {
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

    private Document buildUserDocument(String id, BenutzerRequestDTO request) {
        return new Document("_id", id).append("vorname", request.vorname().trim())
                .append("nachname", request.nachname().trim()).append("email", request.email().trim().toLowerCase())
                .append("rolle", request.rolle()).append("abteilungId", request.abteilungId())
                .append("abteilungName", request.abteilungName()).append("bevorzugterRaumId", request.bevorzugterRaumId())
                .append("aktiv", request.aktiv());
    }

    private Document buildLocationDocument(String id, StandortRequestDTO request) {
        return new Document("_id", id).append("name", request.name().trim())
                .append("adresse", request.adresse().trim()).append("ort", request.ort().trim());
    }

    private String hashPassword(String passwort) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(passwort.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 ist nicht verfuegbar.", exception);
        }
    }

    private void applyReservationStatus(MongoDatabase db, List<Arbeitsplatz> arbeitsplaetze, String von, String bis) {
        Date anfang = parseDateTime(von);
        Date ende = parseDateTime(bis);
        validateTimeRange(anfang, ende);

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
            arbeitsplatz.setReservedByName(name == null ? "" : name);
            Document daten = reservierungsdaten.get(arbeitsplatz.getId());
            if (daten != null) {
                arbeitsplatz.setReservationUserId(daten.getString("benutzerId"));
                arbeitsplatz.setReservationStart(daten.getDate("reservierungAnfang"));
                arbeitsplatz.setReservationEnd(daten.getDate("reservierungEnde"));
            }

            Document naechsteReservierung = db.getCollection("reservierungen")
                    .find(new Document("arbeitsplatzId", arbeitsplatz.getId())
                            .append("status", "reserviert")
                            .append("reservierungAnfang", new Document("$gte", ende)))
                    .sort(new Document("reservierungAnfang", 1))
                    .first();
            if (naechsteReservierung != null) {
                arbeitsplatz.setNextReservationStart(naechsteReservierung.getDate("reservierungAnfang"));
                arbeitsplatz.setNextReservationEnd(naechsteReservierung.getDate("reservierungEnde"));
            }
        }
    }

    private void validateDepartmentAccess(Arbeitsplatz arbeitsplatz, BenutzerDTO benutzer) {
        if (!arbeitsplatz.getDepartmentId().equals(benutzer.abteilungId())) {
            throw new BadRequestException("Es koennen nur Arbeitsplaetze der eigenen Abteilung reserviert werden.");
        }
    }

    private void validateWorkstationAvailability(MongoDatabase db, String arbeitsplatzId, Date anfang, Date ende) {
        Document konflikt = db.getCollection("reservierungen")
                .find(new Document("arbeitsplatzId", arbeitsplatzId)
                        .append("status", "reserviert")
                        .append("reservierungAnfang", new Document("$lt", ende))
                        .append("reservierungEnde", new Document("$gt", anfang)))
                .first();

        if (konflikt != null) {
            String datum = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                    .withZone(ZoneId.of("Europe/Vienna")).format(anfang.toInstant());
            throw new BadRequestException("Dieser Arbeitsplatz ist am " + datum + " bereits reserviert.");
        }
    }

    private void validateUserAvailability(MongoDatabase db, String benutzerId, Date anfang, Date ende, String ausnahmeId) {
        Document filter = new Document("benutzerId", benutzerId)
                .append("status", "reserviert")
                .append("reservierungAnfang", new Document("$lt", ende))
                .append("reservierungEnde", new Document("$gt", anfang));
        if (ausnahmeId != null) filter.append("_id", new Document("$ne", ausnahmeId));
        if (db.getCollection("reservierungen").find(filter).first() != null) {
            String datum = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                    .withZone(ZoneId.of("Europe/Vienna")).format(anfang.toInstant());
            throw new BadRequestException("Am " + datum + " besteht bereits eine eigene Reservierung.");
        }
    }

    private String getDeskNumber(Document dokument) {
        Object wert = dokument.get("tischnr");
        return wert == null ? "" : String.valueOf(wert);
    }

    private Date parseDateTime(String wert) {
        if (wert == null || wert.isBlank()) {
            throw new BadRequestException("Bitte einen gueltigen Zeitraum auswaehlen.");
        }

        try {
            return Date.from(Instant.parse(wert));
        } catch (DateTimeParseException exception) {
            throw new BadRequestException("Der Zeitraum hat ein ungueltiges Format.");
        }
    }

    private void validateTimeRange(Date anfang, Date ende) {
        if (!anfang.before(ende)) {
            throw new BadRequestException("Das Ende muss nach dem Beginn liegen.");
        }
    }

    public ReservierungsEinstellungDTO getBookingSettings() {
        Document dokument = getDatabase().getCollection("einstellungen").find(new Document("_id", "RESERVIERUNG")).first();
        int wochen = dokument == null ? 12 : dokument.getInteger("maxWochen", 12);
        String bis = java.time.LocalDate.now(ZoneId.of("Europe/Vienna")).plusWeeks(wochen).toString();
        return new ReservierungsEinstellungDTO(wochen, bis);
    }

    public ReservierungsEinstellungDTO saveBookingSettings(String adminId, ReservierungsEinstellungDTO request) {
        requireAdmin(adminId);
        if (request == null || request.maxWochen() < 1 || request.maxWochen() > 52)
            throw new BadRequestException("Die Buchungsgrenze muss zwischen 1 und 52 Wochen liegen.");
        getDatabase().getCollection("einstellungen").updateOne(new Document("_id", "RESERVIERUNG"),
                new Document("$set", new Document("maxWochen", request.maxWochen())),
                new com.mongodb.client.model.UpdateOptions().upsert(true));
        recordAuditEntry(adminId, "RESERVIERUNGSGRENZE_GEAENDERT", request.maxWochen() + " Wochen");
        return getBookingSettings();
    }

    private void validateBookingLimit(Date anfang, Date ende) {
        ReservierungsEinstellungDTO einstellung = getBookingSettings();
        ZoneId zone = ZoneId.of("Europe/Vienna");
        java.time.LocalDate heute = java.time.LocalDate.now(zone);
        Instant grenze = heute.plusWeeks(einstellung.maxWochen()).plusDays(1).atStartOfDay(zone).toInstant();
        if (anfang.toInstant().atZone(zone).toLocalDate().isBefore(heute))
            throw new BadRequestException("Reservierungen können nicht an vergangenen Tagen beginnen.");
        if (!anfang.toInstant().isBefore(grenze) || ende.toInstant().isAfter(grenze))
            throw new BadRequestException("Reservierungen sind maximal " + einstellung.maxWochen() + " Wochen im Voraus möglich (bis " + einstellung.buchbarBis() + ").");
    }

    private void deleteUserReservations(String adminId, String benutzerId) {
        long anzahl = getDatabase().getCollection("reservierungen").deleteMany(new Document("benutzerId", benutzerId)).getDeletedCount();
        recordAuditEntry(adminId, "BENUTZER_RESERVIERUNGEN_GELOESCHT", benutzerId + ": " + anzahl);
    }

    private MongoDatabase getDatabase() {
        return mongoClient.getDatabase("tischreservierung");
    }

    private void recordAuditEntry(String adminId, String aktion, String details) {
        getDatabase().getCollection("auditLog").insertOne(new Document("_id", "AUDIT-" + System.nanoTime())
                .append("adminId", adminId).append("aktion", aktion).append("details", details)
                .append("zeitpunkt", new Date()));
    }

    private Benutzer toUser(Document dokument) {
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

    private Standort toLocation(Document dokument) {
        return new Standort(
                dokument.getString("_id"),
                dokument.getString("name"),
                dokument.getString("adresse"),
                dokument.getString("ort")
        );
    }

    private Raum toRoom(Document dokument, List<Arbeitsplatz> arbeitsplaetze) {
        List<Document> elementDokumente = dokument.getList("elemente", Document.class, List.of());
        List<RaumElement> elemente = elementDokumente.stream()
                .map(element -> new RaumElement(
                        element.getString("id"),
                        element.getString("typ"),
                        getNumericValue(element, "x"),
                        getNumericValue(element, "y"),
                        getNumericValue(element, "breite"),
                        getNumericValue(element, "hoehe"),
                        element.getString("text"),
                        getNumericValue(element, "rotation")))
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

    private double getNumericValue(Document dokument, String feld) {
        Number wert = dokument.get(feld, Number.class);
        return wert == null ? 0 : wert.doubleValue();
    }

    private Arbeitsplatz toWorkstation(Document dokument) {
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

    private BenutzerDTO toUserDto(Benutzer benutzer) {
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

    private StandortDTO toLocationDto(Standort standort) {
        return new StandortDTO(standort.getId(), standort.getName(), standort.getAddress(), standort.getCity());
    }

    private RaumAuswahlDTO toRoomOptionDto(Document dokument) {
        return new RaumAuswahlDTO(
                dokument.getString("_id"),
                dokument.getString("name"),
                dokument.getString("standortId"),
                dokument.getString("abteilungName")
        );
    }

    private RaumDTO toRoomDto(Raum raum, BenutzerDTO benutzer) {
        return new RaumDTO(
                raum.getId(),
                raum.getName(),
                raum.getFloor(),
                raum.getLocationId(),
                raum.getDepartmentId(),
                raum.getDepartmentName(),
                raum.getElements().stream().map(element -> new RaumElementDTO(
                        element.getId(), element.getType(), element.getX(), element.getY(),
                        element.getWidth(), element.getHeight(), element.getText(), element.getRotation())).toList(),
                raum.getWorkstations().stream()
                        .map(arbeitsplatz -> toWorkstationDto(arbeitsplatz, benutzer))
                        .toList()
        );
    }

    private ArbeitsplatzDTO toWorkstationDto(Arbeitsplatz arbeitsplatz, BenutzerDTO benutzer) {
        boolean eigeneAbteilung = arbeitsplatz.getDepartmentId().equals(benutzer.abteilungId());
        boolean reservierbar = eigeneAbteilung && "frei".equals(arbeitsplatz.getStatus());
        String hinweis = !eigeneAbteilung
                ? "Nur fuer Abteilung " + arbeitsplatz.getDepartmentName()
                : "reserviert".equals(arbeitsplatz.getStatus()) ? "Im Zeitraum reserviert" : "";

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
                arbeitsplatz.getReservationStart() == null ? "" : arbeitsplatz.getReservationStart().toInstant().toString(),
                arbeitsplatz.getReservationEnd() == null ? "" : arbeitsplatz.getReservationEnd().toInstant().toString(),
                arbeitsplatz.getNextReservationStart() == null ? "" : arbeitsplatz.getNextReservationStart().toInstant().toString(),
                arbeitsplatz.getNextReservationEnd() == null ? "" : arbeitsplatz.getNextReservationEnd().toInstant().toString(),
                arbeitsplatz.getRotation(),
                arbeitsplatz.getWidth(),
                arbeitsplatz.getHeight(),
                arbeitsplatz.getPosition().getX(),
                arbeitsplatz.getPosition().getY(),
                arbeitsplatz.getEquipment().stream().map(equipment -> new EquipmentDTO(equipment.getName())).toList()
        );
    }

    private ReservierungDTO toReservationDto(MongoDatabase db, Document reservierung) {
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
