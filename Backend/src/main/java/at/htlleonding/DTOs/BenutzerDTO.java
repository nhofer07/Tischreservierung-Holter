package at.htlleonding.DTOs;

public record BenutzerDTO(
        String id,
        String vorname,
        String nachname,
        String email,
        String rolle,
        String abteilungId,
        String abteilungName,
        String bevorzugterRaumId
) {
}
