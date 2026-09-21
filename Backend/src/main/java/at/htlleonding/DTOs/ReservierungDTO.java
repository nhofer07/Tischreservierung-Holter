package at.htlleonding.DTOs;

public record ReservierungDTO(
        String id,
        String benutzerId,
        String benutzerName,
        String arbeitsplatzId,
        String tischnr,
        String arbeitsplatzName,
        String raumId,
        String raumName,
        String standortId,
        String standortName,
        String reservierungAnfang,
        String reservierungEnde,
        String status,
        String stornierungsgrund
) {
}
