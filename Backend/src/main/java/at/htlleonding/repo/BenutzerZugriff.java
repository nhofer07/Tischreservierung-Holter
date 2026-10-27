package at.htlleonding.repo;

import static at.htlleonding.repo.RepositoryMapper.toUser;
import static at.htlleonding.repo.RepositoryMapper.toUserDto;
import static at.htlleonding.repo.RepositorySupport.isSuperadmin;

import at.htlleonding.DTOs.BenutzerDTO;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import org.bson.Document;
import org.eclipse.microprofile.jwt.JsonWebToken;

/** Prüft die angemeldete Identität, aktive Benutzerkonten und Administratorrechte. */
@ApplicationScoped
public class BenutzerZugriff extends MongoRepository {

    @Inject
    SecurityIdentity securityIdentity;

    public BenutzerDTO getUser(String id) {
        if (id == null || id.isBlank()) {
            throw new BadRequestException("Bitte einen Benutzer auswaehlen.");
        }

        validateAuthenticatedIdentity(id);

        Document dokument = getDatabase()
            .getCollection("benutzer")
            .find(new Document("_id", id))
            .first();
        if (dokument == null) {
            throw new NotFoundException("Benutzer wurde nicht gefunden.");
        }

        if (!dokument.getBoolean("aktiv", true) || dokument.getBoolean("geloescht", false)) {
            throw new ForbiddenException("Dieses Benutzerkonto ist deaktiviert oder gelöscht.");
        }
        return toUserDto(toUser(dokument));
    }

    private void validateAuthenticatedIdentity(String benutzerId) {
        if (securityIdentity == null || securityIdentity.isAnonymous()) {
            return;
        }
        if (!(securityIdentity.getPrincipal() instanceof JsonWebToken token)) {
            throw new ForbiddenException("Die angemeldete Identität ist ungültig.");
        }

        String email = token.getClaim("email");
        if (email == null || email.isBlank()) {
            email = token.getClaim("preferred_username");
        }
        if (email == null || email.isBlank()) {
            email = token.getClaim("upn");
        }
        Document konto = email == null ? null : findUserByEntraEmail(email);
        if (konto == null || !benutzerId.equals(konto.getString("_id"))) {
            throw new ForbiddenException(
                "Die Benutzer-ID gehört nicht zum angemeldeten Microsoft-Konto."
            );
        }
    }

    public BenutzerDTO requireAdmin(String id) {
        BenutzerDTO benutzer = getUser(id);
        if (!"ADMIN".equals(benutzer.rolle()) && !"SUPERADMIN".equals(benutzer.rolle())) {
            throw new BadRequestException(
                "Diese Funktion ist nur fuer Administratoren verfuegbar."
            );
        }
        return benutzer;
    }

    Document findUserByEntraEmail(String email) {
        String normalisiert = email.trim().toLowerCase();
        Document filter = new Document(
            "$or",
            List.of(new Document("email", normalisiert), new Document("entraEmail", normalisiert))
        );
        return getDatabase().getCollection("benutzer").find(filter).first();
    }

    public BenutzerDTO requireSuperadmin(String id) {
        BenutzerDTO benutzer = requireAdmin(id);
        if (!isSuperadmin(benutzer)) {
            throw new BadRequestException("Diese Funktion ist nur fuer Superadmins verfuegbar.");
        }
        return benutzer;
    }
}
