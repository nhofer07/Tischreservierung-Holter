package at.htlleonding.DTOs;

public record ReservierungRequestDTO(
        String arbeitsplatzId,
        String reservierungAnfang,
        String reservierungEnde
) {
}
