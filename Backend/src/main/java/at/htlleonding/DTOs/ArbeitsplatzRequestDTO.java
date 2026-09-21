package at.htlleonding.DTOs;

import java.util.List;

public record ArbeitsplatzRequestDTO(
        String tischnr,
        String name,
        String raumId,
        String standortId,
        String abteilungId,
        String abteilungName,
        int x,
        int y,
        double rotation,
        double breite,
        double hoehe,
        List<String> equipment
) {
}
