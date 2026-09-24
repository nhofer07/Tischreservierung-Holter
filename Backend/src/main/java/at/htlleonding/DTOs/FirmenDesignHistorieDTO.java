package at.htlleonding.DTOs;

public record FirmenDesignHistorieDTO(
        String id,
        String firmenname,
        String produktname,
        String primaerfarbe,
        String akzentfarbe,
        String logoUrl,
        String zeitpunkt
) {}
