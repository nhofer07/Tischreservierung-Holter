package at.htlleonding.repo;

import static at.htlleonding.repo.RepositoryMapper.toUser;
import static at.htlleonding.repo.RepositoryMapper.toUserDto;
import static at.htlleonding.repo.RepositorySupport.hashPassword;
import static at.htlleonding.repo.RepositorySupport.isBlank;
import static at.htlleonding.repo.RepositorySupport.isSuperadmin;
import static at.htlleonding.repo.RepositorySupport.validateDepartmentAccess;
import static at.htlleonding.repo.RepositorySupport.validateUserPermissions;

import at.htlleonding.DTOs.BenutzerDTO;
import at.htlleonding.DTOs.BenutzerRequestDTO;
import at.htlleonding.DTOs.LoginRequestDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;

@ApplicationScoped
public class BenutzerRepo extends MongoRepository {

    @Inject
    AuditRepo auditRepo;

    @Inject
    BenutzerZugriff benutzerZugriff;

    public List<BenutzerDTO> getUsers() {
        List<BenutzerDTO> benutzer = getDatabase()
            .getCollection("benutzer")
            .find(new Document("geloescht", new Document("$ne", true)))
            .sort(new Document("nachname", 1))
            .map(RepositoryMapper::toUser)
            .map(RepositoryMapper::toUserDto)
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

        Document dokument = getDatabase()
            .getCollection("benutzer")
            .find(new Document("email", request.email().trim().toLowerCase()))
            .first();

        if (
            dokument == null ||
            !hashPassword(request.passwort()).equals(dokument.getString("passwortHash"))
        ) {
            throw new BadRequestException("E-Mail oder Passwort ist falsch.");
        }

        if (!dokument.getBoolean("aktiv", true)) {
            throw new BadRequestException("Dieses Benutzerkonto ist deaktiviert.");
        }

        return toUserDto(toUser(dokument));
    }

    public BenutzerDTO loginWithEntra(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Das Entra-Token enthält keine E-Mail-Adresse.");
        }

        Document dokument = benutzerZugriff.findUserByEntraEmail(email);

        if (dokument == null) {
            throw new NotFoundException(
                "Für dieses Microsoft-Konto wurde noch kein Mitarbeiterprofil angelegt."
            );
        }
        if (!dokument.getBoolean("aktiv", true)) {
            throw new BadRequestException("Dieses Benutzerkonto ist deaktiviert.");
        }
        return toUserDto(toUser(dokument));
    }

    public List<BenutzerDTO> getManagedUsers(String adminId) {
        BenutzerDTO admin = benutzerZugriff.requireAdmin(adminId);
        if (isSuperadmin(admin)) {
            return getUsers();
        }
        return getDatabase()
            .getCollection("benutzer")
            .find(
                new Document("abteilungId", admin.abteilungId()).append(
                    "geloescht",
                    new Document("$ne", true)
                )
            )
            .sort(new Document("nachname", 1))
            .map(RepositoryMapper::toUser)
            .map(RepositoryMapper::toUserDto)
            .into(new ArrayList<>());
    }

    public BenutzerDTO createUser(String adminId, BenutzerRequestDTO request) {
        BenutzerDTO admin = benutzerZugriff.requireAdmin(adminId);
        validateUserRequest(request);
        validateUserPermissions(admin, request);
        if (
            getDatabase()
                .getCollection("benutzer")
                .find(new Document("email", request.email().trim().toLowerCase()))
                .first() != null
        ) {
            throw new BadRequestException("Diese E-Mail-Adresse wird bereits verwendet.");
        }
        String id = "USER-" + System.currentTimeMillis();
        Document dokument = buildUserDocument(id, request).append(
            "passwortHash",
            hashPassword("holter123")
        );
        getDatabase().getCollection("benutzer").insertOne(dokument);
        auditRepo.recordAuditEntry(adminId, "BENUTZER_ANGELEGT", request.email());
        return toUserDto(toUser(dokument));
    }

    public BenutzerDTO updateUser(String adminId, String id, BenutzerRequestDTO request) {
        BenutzerDTO admin = benutzerZugriff.requireAdmin(adminId);
        validateUserRequest(request);
        Document alt = getDatabase()
            .getCollection("benutzer")
            .find(new Document("_id", id))
            .first();
        if (alt == null) {
            throw new NotFoundException("Benutzer wurde nicht gefunden.");
        }
        validateDepartmentAccess(admin, alt.getString("abteilungId"));
        validateDepartmentAccess(admin, request.abteilungId());
        if (alt.getBoolean("geloescht", false)) {
            throw new BadRequestException("Gelöschte Benutzer können nicht bearbeitet werden.");
        }
        if (adminId.equals(id) && !request.aktiv()) {
            throw new BadRequestException("Das eigene Konto kann nicht deaktiviert werden.");
        }
        if (!isSuperadmin(admin) && !alt.getString("rolle").equals(request.rolle())) {
            throw new BadRequestException("Nur Superadmins duerfen Adminrollen aendern.");
        }
        Document dokument = buildUserDocument(id, request).append(
            "passwortHash",
            alt.getString("passwortHash")
        );
        if (alt.containsKey("entraEmail")) {
            dokument.put("entraEmail", alt.getString("entraEmail"));
        }
        getDatabase().getCollection("benutzer").replaceOne(new Document("_id", id), dokument);
        if (!request.aktiv()) {
            deleteUserReservations(adminId, id);
        }
        auditRepo.recordAuditEntry(adminId, "BENUTZER_GEAENDERT", request.email());
        return toUserDto(toUser(dokument));
    }

    public void deleteUser(String adminId, String id) {
        benutzerZugriff.requireSuperadmin(adminId);
        Document benutzer = getDatabase()
            .getCollection("benutzer")
            .find(new Document("_id", id))
            .first();
        if (benutzer == null) {
            throw new NotFoundException("Benutzer wurde nicht gefunden.");
        }
        if (!"USER".equals(benutzer.getString("rolle"))) {
            throw new BadRequestException(
                "Admin-Konten werden über die Rollenverwaltung geändert und nicht als Mitarbeiter gelöscht."
            );
        }
        getDatabase()
            .getCollection("benutzer")
            .updateOne(
                new Document("_id", id),
                new Document("$set", new Document("aktiv", false).append("geloescht", true))
            );
        deleteUserReservations(adminId, id);
        auditRepo.recordAuditEntry(adminId, "BENUTZER_GELOESCHT", benutzer.getString("email"));
    }

    private void validateUserRequest(BenutzerRequestDTO request) {
        if (
            request == null ||
            isBlank(request.vorname()) ||
            isBlank(request.nachname()) ||
            isBlank(request.email()) ||
            isBlank(request.rolle()) ||
            isBlank(request.abteilungId()) ||
            isBlank(request.abteilungName())
        ) {
            throw new BadRequestException("Bitte alle Benutzerdaten ausfüllen.");
        }
    }

    private Document buildUserDocument(String id, BenutzerRequestDTO request) {
        return new Document("_id", id)
            .append("vorname", request.vorname().trim())
            .append("nachname", request.nachname().trim())
            .append("email", request.email().trim().toLowerCase())
            .append("rolle", request.rolle())
            .append("abteilungId", request.abteilungId())
            .append("abteilungName", request.abteilungName())
            .append("bevorzugterRaumId", request.bevorzugterRaumId())
            .append("aktiv", request.aktiv());
    }

    private void deleteUserReservations(String adminId, String benutzerId) {
        long anzahl = getDatabase()
            .getCollection("reservierungen")
            .deleteMany(new Document("benutzerId", benutzerId))
            .getDeletedCount();
        auditRepo.recordAuditEntry(
            adminId,
            "BENUTZER_RESERVIERUNGEN_GELOESCHT",
            benutzerId + ": " + anzahl
        );
    }
}
