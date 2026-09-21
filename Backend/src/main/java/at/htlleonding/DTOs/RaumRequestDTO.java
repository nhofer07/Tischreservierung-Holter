package at.htlleonding.DTOs;

import java.util.List;

public record RaumRequestDTO(
        String name,
        String stockwerk,
        String standortId,
        String abteilungId,
        String abteilungName,
        List<RaumElementDTO> elemente
) {
}
