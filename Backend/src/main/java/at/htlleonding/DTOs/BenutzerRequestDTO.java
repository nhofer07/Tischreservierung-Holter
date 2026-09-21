package at.htlleonding.DTOs;

public record BenutzerRequestDTO(
        String vorname,
        String nachname,
        String email,
        String rolle,
        String abteilungId,
        String abteilungName,
        String bevorzugterRaumId,
        boolean aktiv
) {}
