package at.htlleonding.DTOs;

public record MitarbeiterImportZeileDTO(
    int zeile,
    String vorname,
    String nachname,
    String email,
    String abteilung,
    String bevorzugterRaum,
    boolean aktiv
) {}
