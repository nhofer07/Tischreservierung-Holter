package at.htlleonding.repo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import at.htlleonding.DTOs.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Consumer;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReservierungsFeedbackTest {

    BenutzerRepo benutzerRepo;
    ReservierungRepo reservierungRepo;
    EinstellungRepo einstellungRepo;
    MongoClient mongoClient;
    BenutzerZugriff benutzerZugriff;
    MongoCollection<Document> users;
    MongoCollection<Document> reservations;
    MongoCollection<Document> settings;
    MongoCollection<Document> departments;
    final ZoneId zone = ZoneId.of("Europe/Vienna");

    @BeforeEach
    void setup() {
        mongoClient = mock(MongoClient.class, RETURNS_DEEP_STUBS);
        benutzerZugriff = new BenutzerZugriff();
        benutzerZugriff.mongoClient = mongoClient;
        var audit = new AuditRepo();
        audit.mongoClient = mongoClient;
        audit.benutzerZugriff = benutzerZugriff;
        einstellungRepo = new EinstellungRepo();
        einstellungRepo.mongoClient = mongoClient;
        einstellungRepo.benutzerZugriff = benutzerZugriff;
        einstellungRepo.auditRepo = audit;
        benutzerRepo = new BenutzerRepo();
        benutzerRepo.mongoClient = mongoClient;
        benutzerRepo.benutzerZugriff = benutzerZugriff;
        benutzerRepo.auditRepo = audit;
        reservierungRepo = new ReservierungRepo();
        reservierungRepo.mongoClient = mongoClient;
        reservierungRepo.benutzerZugriff = benutzerZugriff;
        reservierungRepo.auditRepo = audit;
        reservierungRepo.einstellungRepo = einstellungRepo;
        var db = mongoClient.getDatabase("tischreservierung");
        users = db.getCollection("benutzer");
        reservations = db.getCollection("reservierungen");
        settings = db.getCollection("einstellungen");
        departments = db.getCollection("abteilungen");
        when(users.find(new Document("_id", "ADMIN")).first()).thenReturn(
            user("ADMIN", "ADMIN", true)
        );
        when(users.find(new Document("_id", "SUPER")).first()).thenReturn(
            user("SUPER", "SUPERADMIN", true)
        );
        when(users.find(new Document("_id", "USER")).first()).thenReturn(
            user("USER", "USER", true)
        );
        when(settings.find(new Document("_id", "RESERVIERUNG")).first()).thenReturn(
            new Document("maxWochen", 2)
        );
        when(reservations.deleteMany(any(Document.class))).thenReturn(DeleteResult.acknowledged(3));
    }

    Document user(String id, String role, boolean active) {
        return new Document("_id", id)
            .append("vorname", "Anna")
            .append("nachname", "Test")
            .append("email", id + "@test.at")
            .append("rolle", role)
            .append("aktiv", active)
            .append("abteilungId", "IT")
            .append("abteilungName", "IT")
            .append("entraEmail", "entra@test.at");
    }

    ReservierungRequestDTO booking(int startDays, int endDays, int repeats) {
        LocalDate today = LocalDate.now(zone);
        return new ReservierungRequestDTO(
            "USER",
            "DESK",
            today.plusDays(startDays).atTime(8, 0).atZone(zone).toInstant().toString(),
            today.plusDays(endDays).atTime(16, 0).atZone(zone).toInstant().toString(),
            repeats
        );
    }

    @Test
    void deactivateDeletesAllReservationsAndPreservesEntraMapping() {
        benutzerRepo.updateUser(
            "ADMIN",
            "USER",
            new BenutzerRequestDTO("Anna", "Test", "user@test.at", "USER", "IT", "IT", "", false)
        );
        verify(reservations).deleteMany(new Document("benutzerId", "USER"));
        verify(users).replaceOne(
            eq(new Document("_id", "USER")),
            argThat(
                (Document d) ->
                    "entra@test.at".equals(d.getString("entraEmail")) && !d.getBoolean("aktiv")
            )
        );
    }

    @Test
    void activeUserUpdateKeepsReservations() {
        benutzerRepo.updateUser(
            "ADMIN",
            "USER",
            new BenutzerRequestDTO("Anna", "Test", "user@test.at", "USER", "IT", "IT", "", true)
        );
        verify(reservations, never()).deleteMany(any(Document.class));
    }

    @Test
    void deleteRemovesReservations() {
        benutzerRepo.deleteUser("SUPER", "USER");
        verify(reservations).deleteMany(new Document("benutzerId", "USER"));
        verify(users).deleteOne(new Document("_id", "USER"));
        verify(users, never()).updateOne(eq(new Document("_id", "USER")), any(Document.class));
    }

    @Test
    void deletedEmailCanBeCreatedAgain() {
        Document deleted = user("OLD", "USER", false)
            .append("email", "wieder@test.at")
            .append("geloescht", true);
        when(users.find(new Document("email", "wieder@test.at")).first()).thenReturn(deleted);

        BenutzerDTO created = benutzerRepo.createUser(
            "SUPER",
            new BenutzerRequestDTO("Wieder", "Da", "wieder@test.at", "USER", "IT", "IT", "", true)
        );

        assertEquals("wieder@test.at", created.email());
        verify(users).deleteOne(new Document("_id", "OLD"));
        verify(users).insertOne(
            argThat(
                document ->
                    "wieder@test.at".equals(document.getString("email")) &&
                    !document.containsKey("geloescht") &&
                    document.getBoolean("aktiv")
            )
        );
    }

    @Test
    void inactiveUserCannotBookFromExistingSession() {
        when(users.find(new Document("_id", "USER")).first()).thenReturn(
            user("USER", "USER", false)
        );
        assertThrows(ForbiddenException.class, () ->
            reservierungRepo.createReservation(booking(1, 1, 0))
        );
        verify(reservations, never()).insertMany(anyList());
    }

    @Test
    void adminCanChangeLimitAndUsersCannot() {
        assertEquals(
            2,
            einstellungRepo
                .saveBookingSettings("ADMIN", new ReservierungsEinstellungDTO(2, null))
                .maxWochen()
        );
        assertThrows(BadRequestException.class, () ->
            einstellungRepo.saveBookingSettings("USER", new ReservierungsEinstellungDTO(2, null))
        );
        for (int weeks : new int[] { 0, -1, 53 }) {
            assertThrows(BadRequestException.class, () ->
                einstellungRepo.saveBookingSettings(
                    "ADMIN",
                    new ReservierungsEinstellungDTO(weeks, null)
                )
            );
        }
    }

    @Test
    void startAndEndBeyondLimitAreRejectedForCreationAndEditing() {
        assertThrows(BadRequestException.class, () ->
            reservierungRepo.createReservation(booking(15, 15, 0))
        );
        assertThrows(BadRequestException.class, () ->
            reservierungRepo.createReservation(booking(1, 15, 0))
        );
        assertThrows(BadRequestException.class, () ->
            reservierungRepo.updateReservation("RES", booking(1, 15, 0))
        );
        verify(reservations, never()).insertMany(anyList());
    }

    @Test
    void pastTimeTodayIsRejectedForCreationAndEditing() {
        Instant jetzt = Instant.now();
        var request = new ReservierungRequestDTO(
            "USER",
            "DESK",
            jetzt.minusSeconds(3600).toString(),
            jetzt.plusSeconds(3600).toString(),
            0
        );
        assertThrows(BadRequestException.class, () -> reservierungRepo.createReservation(request));
        assertThrows(BadRequestException.class, () ->
            reservierungRepo.updateReservation("RES", request)
        );
        verify(reservations, never()).insertMany(anyList());
    }

    @Test
    void lastAllowedDayPassesLimitValidation() {
        // A missing desk is reached only after the boundary validation succeeds.
        assertThrows(jakarta.ws.rs.NotFoundException.class, () ->
            reservierungRepo.createReservation(booking(14, 14, 0))
        );
    }

    @Test
    void recurrenceBeyondLimitDoesNotInsertPartialSeries() {
        var desks = mongoClient.getDatabase("tischreservierung").getCollection("arbeitsplaetze");
        when(desks.find(new Document("_id", "DESK")).first()).thenReturn(
            new Document("_id", "DESK")
                .append("abteilungId", "IT")
                .append("status", "frei")
                .append("position", new Document("x", 20).append("y", 20))
        );
        var error = assertThrows(BadRequestException.class, () ->
            reservierungRepo.createReservation(booking(1, 1, 2))
        );
        assertTrue(error.getMessage().contains("Wochen im Voraus"));
        verify(reservations, never()).insertMany(anyList());
    }

    @Test
    void testLoginAcceptsValidCredentialsAndRejectsWrongPasswords() {
        Document account = user("USER", "USER", true).append(
            "passwortHash",
            "a4cb341a7228f2f4c3ac322a47985ecd6c355037cf41d9f9f47494bb63da77b0"
        );
        when(users.find(new Document("email", "user@test.at")).first()).thenReturn(account);
        assertEquals(
            "USER",
            benutzerRepo
                .loginWithTestAccount(new LoginRequestDTO(" USER@test.at ", "holter123"))
                .id()
        );
        assertThrows(BadRequestException.class, () ->
            benutzerRepo.loginWithTestAccount(new LoginRequestDTO("user@test.at", "incorrect"))
        );
    }

    @Test
    void testLoginRejectsDisabledAccounts() {
        Document account = user("USER", "USER", false).append(
            "passwortHash",
            "a4cb341a7228f2f4c3ac322a47985ecd6c355037cf41d9f9f47494bb63da77b0"
        );
        when(users.find(new Document("email", "user@test.at")).first()).thenReturn(account);
        assertThrows(BadRequestException.class, () ->
            benutzerRepo.loginWithTestAccount(new LoginRequestDTO("user@test.at", "holter123"))
        );
    }

    @Test
    void authenticatedUserCannotBookForAnotherMicrosoftAccount() {
        var identity = mock(SecurityIdentity.class);
        var token = mock(JsonWebToken.class);
        benutzerZugriff.securityIdentity = identity;
        when(identity.getPrincipal()).thenReturn(token);
        when(token.getClaim("email")).thenReturn("other@test.at");
        var filter = new Document(
            "$or",
            java.util.List.of(
                new Document("email", "other@test.at"),
                new Document("entraEmail", "other@test.at")
            )
        );
        when(users.find(filter).first()).thenReturn(user("OTHER", "USER", true));

        assertThrows(ForbiddenException.class, () ->
            reservierungRepo.createReservation(booking(1, 1, 0))
        );
        verify(reservations, never()).insertMany(anyList());
    }

    @Test
    void disabledAccountCannotLoadRoomThroughSharedAccessCheck() {
        var roomRepo = new RaumRepo();
        roomRepo.mongoClient = mongoClient;
        roomRepo.benutzerZugriff = benutzerZugriff;
        when(users.find(new Document("_id", "USER")).first()).thenReturn(
            user("USER", "USER", false)
        );

        assertThrows(ForbiddenException.class, () -> roomRepo.getRoom("ROOM", null, null, "USER"));
    }

    @Test
    void departmentAdminCannotChangeCompanyBranding() {
        var branding = new FirmenEinstellungDTO("HOLTER", "DeskVision", "#a51e2d", "#343638", "");

        assertThrows(BadRequestException.class, () ->
            einstellungRepo.saveCompanySettings("ADMIN", branding)
        );
        verify(settings, never()).updateOne(
            any(Document.class),
            any(Document.class),
            any(com.mongodb.client.model.UpdateOptions.class)
        );
    }

    @Test
    void validExcelRowIsValidatedAndImportedAsEmployee() throws IOException {
        mockImportDepartment();
        var validation = benutzerRepo.validateUserImport(
            "SUPER",
            excel(
                new String[] {
                    "Vorname",
                    "Nachname",
                    "E-Mail",
                    "Abteilung",
                    "Bevorzugter Raum",
                    "Aktiv",
                },
                new String[] { "Max", "Muster", "max.muster@holter.test", "IT", "", "Ja" }
            )
        );

        assertEquals(1, validation.gesamt());
        assertEquals(1, validation.gueltig());
        assertTrue(validation.fehler().isEmpty());
        assertEquals("max.muster@holter.test", validation.vorschau().getFirst().email());

        var result = benutzerRepo.importUsers(
            "SUPER",
            excel(
                new String[] {
                    "Vorname",
                    "Nachname",
                    "E-Mail",
                    "Abteilung",
                    "Bevorzugter Raum",
                    "Aktiv",
                },
                new String[] { "Max", "Muster", "max.muster@holter.test", "IT", "", "Ja" }
            )
        );
        assertEquals(1, result.importiert());
        verify(users).insertMany(
            argThat(
                documents ->
                    documents.size() == 1 &&
                    "USER".equals(documents.getFirst().getString("rolle")) &&
                    "max.muster@holter.test".equals(documents.getFirst().getString("email"))
            )
        );
    }

    @Test
    void duplicateEmailPreventsEntireExcelImport() throws IOException {
        mockImportDepartment();
        var result = benutzerRepo.importUsers(
            "SUPER",
            excel(
                new String[] { "Vorname", "Nachname", "E-Mail", "Abteilung" },
                new String[] { "Max", "Muster", "doppelt@holter.test", "IT" },
                new String[] { "Maria", "Muster", "doppelt@holter.test", "IT" }
            )
        );

        assertEquals(2, result.gesamt());
        assertFalse(result.fehler().isEmpty());
        assertEquals(0, result.importiert());
        verify(users, never()).insertMany(anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void deletedEmailCanBeImportedAgain() throws IOException {
        mockImportDepartment();
        FindIterable<Document> allUsers = mock(FindIterable.class);
        FindIterable<Document> projectedUsers = mock(FindIterable.class);
        when(users.find()).thenReturn(allUsers);
        when(allUsers.projection(any(Document.class))).thenReturn(projectedUsers);
        doAnswer(invocation -> {
            Consumer<Document> consumer = invocation.getArgument(0);
            consumer.accept(new Document("email", "zurueck@holter.test").append("geloescht", true));
            return null;
        })
            .when(projectedUsers)
            .forEach(any());

        var result = benutzerRepo.importUsers(
            "SUPER",
            excel(
                new String[] { "Vorname", "Nachname", "E-Mail", "Abteilung" },
                new String[] { "Wieder", "Da", "zurueck@holter.test", "IT" }
            )
        );

        assertEquals(1, result.importiert());
        assertTrue(result.fehler().isEmpty());
        verify(users).deleteMany(
            new Document("email", new Document("$in", List.of("zurueck@holter.test"))).append(
                "geloescht",
                true
            )
        );
        verify(users).insertMany(
            argThat(
                documents ->
                    documents.size() == 1 &&
                    "zurueck@holter.test".equals(documents.getFirst().getString("email"))
            )
        );
    }

    @SuppressWarnings("unchecked")
    private void mockImportDepartment() {
        FindIterable<Document> result = mock(FindIterable.class);
        when(departments.find()).thenReturn(result);
        doAnswer(invocation -> {
            Consumer<Document> consumer = invocation.getArgument(0);
            consumer.accept(new Document("_id", "IT").append("name", "IT"));
            return null;
        })
            .when(result)
            .forEach(any());
    }

    private ByteArrayInputStream excel(String[] header, String[]... rows) throws IOException {
        try (
            Workbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            Sheet sheet = workbook.createSheet("Mitarbeiter");
            Row headerRow = sheet.createRow(0);
            for (int column = 0; column < header.length; column++) headerRow
                .createCell(column)
                .setCellValue(header[column]);
            for (int index = 0; index < rows.length; index++) {
                Row row = sheet.createRow(index + 1);
                for (int column = 0; column < rows[index].length; column++) row.createCell(
                    column
                ).setCellValue(rows[index][column]);
            }
            workbook.write(output);
            return new ByteArrayInputStream(output.toByteArray());
        }
    }
}
