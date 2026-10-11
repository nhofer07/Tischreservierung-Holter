package at.htlleonding.repo;

import at.htlleonding.DTOs.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReservierungsFeedbackTest {
    ArbeitsplatzRepo repo;
    MongoCollection<Document> users;
    MongoCollection<Document> reservations;
    MongoCollection<Document> settings;
    final ZoneId zone = ZoneId.of("Europe/Vienna");

    @BeforeEach
    void setup() {
        repo = new ArbeitsplatzRepo();
        repo.mongoClient = mock(MongoClient.class, RETURNS_DEEP_STUBS);
        var db = repo.mongoClient.getDatabase("tischreservierung");
        users = db.getCollection("benutzer");
        reservations = db.getCollection("reservierungen");
        settings = db.getCollection("einstellungen");
        when(users.find(new Document("_id", "ADMIN")).first()).thenReturn(user("ADMIN", "ADMIN", true));
        when(users.find(new Document("_id", "SUPER")).first()).thenReturn(user("SUPER", "SUPERADMIN", true));
        when(users.find(new Document("_id", "USER")).first()).thenReturn(user("USER", "USER", true));
        when(settings.find(new Document("_id", "RESERVIERUNG")).first()).thenReturn(new Document("maxWochen", 2));
        when(reservations.deleteMany(any(Document.class))).thenReturn(DeleteResult.acknowledged(3));
    }

    Document user(String id, String role, boolean active) {
        return new Document("_id", id).append("vorname", "Anna").append("nachname", "Test")
                .append("email", id + "@test.at").append("rolle", role).append("aktiv", active)
                .append("abteilungId", "IT").append("abteilungName", "IT").append("entraEmail", "entra@test.at");
    }

    ReservierungRequestDTO booking(int startDays, int endDays, int repeats) {
        LocalDate today = LocalDate.now(zone);
        return new ReservierungRequestDTO("USER", "DESK",
                today.plusDays(startDays).atTime(8, 0).atZone(zone).toInstant().toString(),
                today.plusDays(endDays).atTime(16, 0).atZone(zone).toInstant().toString(), repeats);
    }

    @Test
    void deactivateDeletesAllReservationsAndPreservesEntraMapping() {
        repo.updateUser("ADMIN", "USER", new BenutzerRequestDTO("Anna", "Test", "user@test.at", "USER", "IT", "IT", "", false));
        verify(reservations).deleteMany(new Document("benutzerId", "USER"));
        verify(users).replaceOne(eq(new Document("_id", "USER")), argThat((Document d) -> "entra@test.at".equals(d.getString("entraEmail")) && !d.getBoolean("aktiv")));
    }

    @Test
    void activeUserUpdateKeepsReservations() {
        repo.updateUser("ADMIN", "USER", new BenutzerRequestDTO("Anna", "Test", "user@test.at", "USER", "IT", "IT", "", true));
        verify(reservations, never()).deleteMany(any(Document.class));
    }

    @Test
    void deleteRemovesReservations() {
        repo.deleteUser("SUPER", "USER");
        verify(reservations).deleteMany(new Document("benutzerId", "USER"));
    }

    @Test
    void inactiveUserCannotBookFromExistingSession() {
        when(users.find(new Document("_id", "USER")).first()).thenReturn(user("USER", "USER", false));
        assertThrows(ForbiddenException.class, () -> repo.createReservation(booking(1, 1, 0)));
        verify(reservations, never()).insertMany(anyList());
    }

    @Test
    void adminCanChangeLimitAndUsersCannot() {
        assertEquals(2, repo.saveBookingSettings("ADMIN", new ReservierungsEinstellungDTO(2, null)).maxWochen());
        assertThrows(BadRequestException.class, () -> repo.saveBookingSettings("USER", new ReservierungsEinstellungDTO(2, null)));
        for (int weeks : new int[]{0, -1, 53})
            assertThrows(BadRequestException.class, () -> repo.saveBookingSettings("ADMIN", new ReservierungsEinstellungDTO(weeks, null)));
    }

    @Test
    void startAndEndBeyondLimitAreRejectedForCreationAndEditing() {
        assertThrows(BadRequestException.class, () -> repo.createReservation(booking(15, 15, 0)));
        assertThrows(BadRequestException.class, () -> repo.createReservation(booking(1, 15, 0)));
        assertThrows(BadRequestException.class, () -> repo.updateReservation("RES", booking(1, 15, 0)));
        verify(reservations, never()).insertMany(anyList());
    }

    @Test
    void lastAllowedDayPassesLimitValidation() {
        // A missing desk is reached only after the boundary validation succeeds.
        assertThrows(jakarta.ws.rs.NotFoundException.class, () -> repo.createReservation(booking(14, 14, 0)));
    }

    @Test
    void recurrenceBeyondLimitDoesNotInsertPartialSeries() {
        var desks = repo.mongoClient.getDatabase("tischreservierung").getCollection("arbeitsplaetze");
        when(desks.find(new Document("_id", "DESK")).first()).thenReturn(new Document("_id", "DESK")
                .append("abteilungId", "IT").append("status", "frei").append("position", new Document("x", 20).append("y", 20)));
        var error = assertThrows(BadRequestException.class, () -> repo.createReservation(booking(1, 1, 2)));
        assertTrue(error.getMessage().contains("Wochen im Voraus"));
        verify(reservations, never()).insertMany(anyList());
    }

    @Test
    void testLoginAcceptsValidCredentialsAndRejectsWrongPasswords() {
        Document account = user("USER", "USER", true)
                .append("passwortHash", "a4cb341a7228f2f4c3ac322a47985ecd6c355037cf41d9f9f47494bb63da77b0");
        when(users.find(new Document("email", "user@test.at")).first()).thenReturn(account);
        assertEquals("USER", repo.loginWithTestAccount(new LoginRequestDTO(" USER@test.at ", "holter123")).id());
        assertThrows(BadRequestException.class, () -> repo.loginWithTestAccount(new LoginRequestDTO("user@test.at", "incorrect")));
    }

    @Test
    void testLoginRejectsDisabledAccounts() {
        Document account = user("USER", "USER", false)
                .append("passwortHash", "a4cb341a7228f2f4c3ac322a47985ecd6c355037cf41d9f9f47494bb63da77b0");
        when(users.find(new Document("email", "user@test.at")).first()).thenReturn(account);
        assertThrows(BadRequestException.class, () -> repo.loginWithTestAccount(new LoginRequestDTO("user@test.at", "holter123")));
    }

}
