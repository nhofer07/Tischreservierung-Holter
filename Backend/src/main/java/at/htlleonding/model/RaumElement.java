package at.htlleonding.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.codecs.pojo.annotations.BsonProperty;

public class RaumElement {

    private String id;

    @BsonProperty("typ")
    @JsonProperty("typ")
    private String type;

    private double x;
    private double y;

    @BsonProperty("breite")
    @JsonProperty("breite")
    private double width;

    @BsonProperty("hoehe")
    @JsonProperty("hoehe")
    private double height;

    private String text;
    private double rotation;

    public RaumElement() {}

    public RaumElement(
        String id,
        String type,
        double x,
        double y,
        double width,
        double height,
        String text,
        double rotation
    ) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.text = text;
        this.rotation = rotation;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public String getText() {
        return text;
    }

    public double getRotation() {
        return rotation;
    }
}
