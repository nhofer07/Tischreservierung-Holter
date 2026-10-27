package at.htlleonding.DTOs;

import java.util.List;

public record MitarbeiterImportErgebnisDTO(
    int gesamt,
    int gueltig,
    int importiert,
    List<MitarbeiterImportZeileDTO> vorschau,
    List<MitarbeiterImportFehlerDTO> fehler
) {}
