package at.htlleonding.DTOs;

public record RaumElementDTO(
        String id,
        String typ,
        double x,
        double y,
        double breite,
        double hoehe,
        String text,
        double rotation
) {
}
