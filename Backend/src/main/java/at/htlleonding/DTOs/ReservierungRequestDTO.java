package at.htlleonding.DTOs;

public record ReservierungRequestDTO(
        String benutzerId,
        String arbeitsplatzId,
        String reservierungAnfang,
        String reservierungEnde
) {
}
