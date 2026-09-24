package at.htlleonding.DTOs;

import java.util.List;

public record ArbeitsplatzDTO(
        String id,
        String tischnr,
        String name,
        String status,
        String abteilungId,
        String abteilungName,
        boolean reservierbar,
        String hinweis,
        String reserviertVon,
        boolean eigeneReservierung,
        String reservierungAnfang,
        String reservierungEnde,
        String naechsteReservierungAnfang,
        String naechsteReservierungEnde,
        double rotation,
        double breite,
        double hoehe,
        int x,
        int y,
        List<EquipmentDTO> equipment
) {
}
