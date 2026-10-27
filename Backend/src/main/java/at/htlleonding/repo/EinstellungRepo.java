package at.htlleonding.repo;

import static at.htlleonding.repo.RepositorySupport.isBlank;

import at.htlleonding.DTOs.FirmenDesignHistorieDTO;
import at.htlleonding.DTOs.FirmenEinstellungDTO;
import at.htlleonding.DTOs.ReservierungsEinstellungDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.bson.Document;

@ApplicationScoped
public class EinstellungRepo extends MongoRepository {

    @Inject
    AuditRepo auditRepo;

    @Inject
    BenutzerZugriff benutzerZugriff;

    public FirmenEinstellungDTO getCompanySettings() {
        Document dokument = getDatabase()
            .getCollection("einstellungen")
            .find(new Document("_id", "FIRMA"))
            .first();
        if (dokument == null) {
            return new FirmenEinstellungDTO(
                "HOLTER",
                "DeskVision",
                "#a51e2d",
                "#343638",
                "/holter-logo.png"
            );
        }
        return new FirmenEinstellungDTO(
            dokument.getString("firmenname"),
            "DeskVision",
            dokument.getString("primaerfarbe"),
            dokument.getString("akzentfarbe"),
            dokument.getString("logoUrl")
        );
    }

    public FirmenEinstellungDTO saveCompanySettings(String adminId, FirmenEinstellungDTO request) {
        benutzerZugriff.requireSuperadmin(adminId);
        if (request == null || isBlank(request.firmenname())) {
            throw new BadRequestException("Der Firmenname ist erforderlich.");
        }
        if (!isValidHexColor(request.primaerfarbe()) || !isValidHexColor(request.akzentfarbe())) {
            throw new BadRequestException(
                "Farben müssen als Hex-Wert wie #a51e2d angegeben werden."
            );
        }
        String logoUrl = isBlank(request.logoUrl()) ? "" : request.logoUrl().trim();
        Document werte = new Document("firmenname", request.firmenname().trim())
            .append("produktname", "DeskVision")
            .append("primaerfarbe", request.primaerfarbe().toLowerCase())
            .append("akzentfarbe", request.akzentfarbe().toLowerCase())
            .append("logoUrl", logoUrl);
        updateCompanySettings(werte);
        Document letzterEintrag = getDatabase()
            .getCollection("designHistorie")
            .find()
            .sort(new Document("zeitpunkt", -1))
            .first();
        if (!hasSameBranding(letzterEintrag, werte)) {
            Document historie = new Document("_id", "DESIGN-" + System.nanoTime()).append(
                "zeitpunkt",
                new Date()
            );
            historie.putAll(werte);
            getDatabase().getCollection("designHistorie").insertOne(historie);
        }
        auditRepo.recordAuditEntry(adminId, "FIRMENDESIGN_GEAENDERT", request.firmenname().trim());
        return getCompanySettings();
    }

    public FirmenEinstellungDTO applyBrandingHistoryEntry(String adminId, String historieId) {
        benutzerZugriff.requireSuperadmin(adminId);
        Document historie = getDatabase()
            .getCollection("designHistorie")
            .find(new Document("_id", historieId))
            .first();
        if (historie == null) {
            throw new NotFoundException("Designvariante wurde nicht gefunden.");
        }
        Document werte = new Document("firmenname", historie.getString("firmenname"))
            .append("produktname", "DeskVision")
            .append("primaerfarbe", historie.getString("primaerfarbe"))
            .append("akzentfarbe", historie.getString("akzentfarbe"))
            .append("logoUrl", historie.getString("logoUrl"));
        updateCompanySettings(werte);
        auditRepo.recordAuditEntry(adminId, "FIRMENDESIGN_VERWENDET", historieId);
        return getCompanySettings();
    }

    public void deleteBrandingHistoryEntry(String adminId, String historieId) {
        benutzerZugriff.requireSuperadmin(adminId);
        Document historie = getDatabase()
            .getCollection("designHistorie")
            .find(new Document("_id", historieId))
            .first();
        if (historie == null) {
            throw new NotFoundException("Designvariante wurde nicht gefunden.");
        }
        Document gleichesDesign = new Document("firmenname", historie.getString("firmenname"))
            .append("primaerfarbe", historie.getString("primaerfarbe"))
            .append("akzentfarbe", historie.getString("akzentfarbe"))
            .append("logoUrl", historie.getString("logoUrl"));
        long geloescht = getDatabase()
            .getCollection("designHistorie")
            .deleteMany(gleichesDesign)
            .getDeletedCount();
        if (geloescht == 0) {
            throw new NotFoundException("Designvariante wurde nicht gefunden.");
        }
        auditRepo.recordAuditEntry(adminId, "FIRMENDESIGN_HISTORIE_GELOESCHT", historieId);
    }

    private void updateCompanySettings(Document werte) {
        getDatabase()
            .getCollection("einstellungen")
            .updateOne(
                new Document("_id", "FIRMA"),
                new Document("$set", werte),
                new com.mongodb.client.model.UpdateOptions().upsert(true)
            );
    }

    private boolean hasSameBranding(Document links, Document rechts) {
        if (links == null) {
            return false;
        }
        return (
            Objects.equals(links.getString("firmenname"), rechts.getString("firmenname")) &&
            Objects.equals(links.getString("primaerfarbe"), rechts.getString("primaerfarbe")) &&
            Objects.equals(links.getString("akzentfarbe"), rechts.getString("akzentfarbe")) &&
            Objects.equals(links.getString("logoUrl"), rechts.getString("logoUrl"))
        );
    }

    public List<FirmenDesignHistorieDTO> getBrandingHistory(String adminId) {
        benutzerZugriff.requireSuperadmin(adminId);
        List<FirmenDesignHistorieDTO> ergebnis = new ArrayList<>();
        Set<String> bekannteDesigns = new HashSet<>();
        for (Document dokument : getDatabase()
            .getCollection("designHistorie")
            .find()
            .sort(new Document("zeitpunkt", -1))) {
            String schluessel =
                dokument.getString("firmenname") +
                "|" +
                dokument.getString("primaerfarbe") +
                "|" +
                dokument.getString("akzentfarbe") +
                "|" +
                dokument.getString("logoUrl");
            if (!bekannteDesigns.add(schluessel)) {
                continue;
            }
            ergebnis.add(
                new FirmenDesignHistorieDTO(
                    dokument.getString("_id"),
                    dokument.getString("firmenname"),
                    "DeskVision",
                    dokument.getString("primaerfarbe"),
                    dokument.getString("akzentfarbe"),
                    dokument.getString("logoUrl"),
                    dokument.getDate("zeitpunkt").toInstant().toString()
                )
            );
            if (ergebnis.size() == 6) {
                break;
            }
        }
        return ergebnis;
    }

    private boolean isValidHexColor(String farbe) {
        return farbe != null && farbe.matches("#[0-9a-fA-F]{6}");
    }

    public ReservierungsEinstellungDTO getBookingSettings() {
        Document dokument = getDatabase()
            .getCollection("einstellungen")
            .find(new Document("_id", "RESERVIERUNG"))
            .first();
        int wochen = dokument == null ? 12 : dokument.getInteger("maxWochen", 12);
        String bis = java.time.LocalDate.now(ZoneId.of("Europe/Vienna"))
            .plusWeeks(wochen)
            .toString();
        return new ReservierungsEinstellungDTO(wochen, bis);
    }

    public ReservierungsEinstellungDTO saveBookingSettings(
        String adminId,
        ReservierungsEinstellungDTO request
    ) {
        benutzerZugriff.requireAdmin(adminId);
        if (request == null || request.maxWochen() < 1 || request.maxWochen() > 52) {
            throw new BadRequestException(
                "Die Buchungsgrenze muss zwischen 1 und 52 Wochen liegen."
            );
        }
        getDatabase()
            .getCollection("einstellungen")
            .updateOne(
                new Document("_id", "RESERVIERUNG"),
                new Document("$set", new Document("maxWochen", request.maxWochen())),
                new com.mongodb.client.model.UpdateOptions().upsert(true)
            );
        auditRepo.recordAuditEntry(
            adminId,
            "RESERVIERUNGSGRENZE_GEAENDERT",
            request.maxWochen() + " Wochen"
        );
        return getBookingSettings();
    }

    void validateBookingLimit(Date anfang, Date ende) {
        ReservierungsEinstellungDTO einstellung = getBookingSettings();
        ZoneId zone = ZoneId.of("Europe/Vienna");
        java.time.LocalDate heute = java.time.LocalDate.now(zone);
        Instant grenze = heute
            .plusWeeks(einstellung.maxWochen())
            .plusDays(1)
            .atStartOfDay(zone)
            .toInstant();
        if (anfang.toInstant().atZone(zone).toLocalDate().isBefore(heute)) {
            throw new BadRequestException(
                "Reservierungen können nicht an vergangenen Tagen beginnen."
            );
        }
        if (!anfang.toInstant().isBefore(grenze) || ende.toInstant().isAfter(grenze)) {
            throw new BadRequestException(
                "Reservierungen sind maximal " +
                    einstellung.maxWochen() +
                    " Wochen im Voraus möglich (bis " +
                    einstellung.buchbarBis() +
                    ")."
            );
        }
    }
}
