package at.htlleonding.model;

public class RaumElement {
    private String id;
    private String typ;
    private double x;
    private double y;
    private double breite;
    private double hoehe;
    private String text;
    private double rotation;

    public RaumElement() {
    }

    public RaumElement(String id, String typ, double x, double y, double breite, double hoehe, String text, double rotation) {
        this.id = id;
        this.typ = typ;
        this.x = x;
        this.y = y;
        this.breite = breite;
        this.hoehe = hoehe;
        this.text = text;
        this.rotation = rotation;
    }

    public String getId() { return id; }
    public String getTyp() { return typ; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getBreite() { return breite; }
    public double getHoehe() { return hoehe; }
    public String getText() { return text; }
    public double getRotation() { return rotation; }
}
