package at.htlleonding.repo;

import at.htlleonding.DTOs.BenutzerDTO;
import at.htlleonding.DTOs.BenutzerRequestDTO;
import at.htlleonding.model.Arbeitsplatz;
import jakarta.ws.rs.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;

/** Gemeinsame Validierungen und Hilfsfunktionen ohne Datenbankzugriff. */
public final class RepositorySupport {

    private RepositorySupport() {}

    static boolean isSuperadmin(BenutzerDTO benutzer) {
        return "SUPERADMIN".equals(benutzer.rolle());
    }

    static void validateDepartmentAccess(BenutzerDTO admin, String abteilungId) {
        if (!isSuperadmin(admin) && !admin.abteilungId().equals(abteilungId)) {
            throw new BadRequestException(
                "Abteilungsleitungen verwalten nur ihre eigene Abteilung."
            );
        }
    }

    static void validateUserPermissions(BenutzerDTO admin, BenutzerRequestDTO request) {
        validateDepartmentAccess(admin, request.abteilungId());
        if (!isSuperadmin(admin) && !"USER".equals(request.rolle())) {
            throw new BadRequestException("Nur Superadmins duerfen Adminrollen vergeben.");
        }
        if (
            !"USER".equals(request.rolle()) &&
            !"ADMIN".equals(request.rolle()) &&
            !"SUPERADMIN".equals(request.rolle())
        ) {
            throw new BadRequestException("Unbekannte Benutzerrolle.");
        }
    }

    static boolean isBlank(String wert) {
        return wert == null || wert.isBlank();
    }

    static String hashPassword(String passwort) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(passwort.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 ist nicht verfuegbar.", exception);
        }
    }

    static void validateDepartmentAccess(Arbeitsplatz arbeitsplatz, BenutzerDTO benutzer) {
        if (!arbeitsplatz.getDepartmentId().equals(benutzer.abteilungId())) {
            throw new BadRequestException(
                "Es koennen nur Arbeitsplaetze der eigenen Abteilung reserviert werden."
            );
        }
    }

    static Date parseDateTime(String wert) {
        if (wert == null || wert.isBlank()) {
            throw new BadRequestException("Bitte einen gueltigen Zeitraum auswaehlen.");
        }

        try {
            return Date.from(Instant.parse(wert));
        } catch (DateTimeParseException exception) {
            throw new BadRequestException("Der Zeitraum hat ein ungueltiges Format.");
        }
    }

    static void validateTimeRange(Date anfang, Date ende) {
        if (!anfang.before(ende)) {
            throw new BadRequestException("Das Ende muss nach dem Beginn liegen.");
        }
    }
}
