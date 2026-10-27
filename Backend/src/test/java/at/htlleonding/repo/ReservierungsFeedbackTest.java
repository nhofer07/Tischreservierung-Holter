package at.htlleonding.repo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import at.htlleonding.DTOs.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import java.time.LocalDate;
import java.time.ZoneId;
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
}
