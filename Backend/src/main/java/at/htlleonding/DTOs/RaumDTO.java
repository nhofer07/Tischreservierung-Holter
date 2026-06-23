package at.htlleonding.DTOs;

import java.util.List;

public record RaumDTO(
        String id,
        String name,
        String stockwerk,
        String standortId,
        String abteilungId,
        String abteilungName,
        List<ArbeitsplatzDTO> arbeitsplaetze
) {
}
