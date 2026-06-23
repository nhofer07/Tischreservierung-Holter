package at.htlleonding.DTOs;

import java.util.List;

public record ArbeitsplatzDTO(
        String id,
        int tischnr,
        String name,
        String status,
        String abteilungId,
        String abteilungName,
        int x,
        int y,
        List<EquipmentDTO> equipment
) {
}
