package at.htlleonding.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.mongodb.panache.common.MongoEntity;
import java.util.Date;
import java.util.List;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

@MongoEntity(collection = "arbeitsplaetze")
public class Arbeitsplatz {

    @BsonId
    private String id;

    @BsonProperty("tischnr")
    @JsonProperty("tischnr")
    private String deskNumber;

    @BsonProperty("raumId")
    @JsonProperty("raumId")
    private String roomId;

    @BsonProperty("standortId")
    @JsonProperty("standortId")
    private String locationId;

    @BsonProperty("abteilungId")
    @JsonProperty("abteilungId")
    private String departmentId;

    @BsonProperty("abteilungName")
    @JsonProperty("abteilungName")
    private String departmentName;

    private String name;
    private String status;

    @BsonProperty("reserviertVon")
    @JsonProperty("reserviertVon")
    private String reservedByName;

    @BsonProperty("reservierungBenutzerId")
    @JsonProperty("reservierungBenutzerId")
    private String reservationUserId;

    @BsonProperty("reservierungAnfang")
    @JsonProperty("reservierungAnfang")
    private Date reservationStart;

    @BsonProperty("reservierungEnde")
    @JsonProperty("reservierungEnde")
    private Date reservationEnd;

    @BsonProperty("naechsteReservierungAnfang")
    @JsonProperty("naechsteReservierungAnfang")
    private Date nextReservationStart;

    @BsonProperty("naechsteReservierungEnde")
    @JsonProperty("naechsteReservierungEnde")
    private Date nextReservationEnd;

    private double rotation;

    @BsonProperty("breite")
    @JsonProperty("breite")
    private double width;

    @BsonProperty("hoehe")
    @JsonProperty("hoehe")
    private double height;

    private Position position;
    private List<Equipment> equipment;

    public Arbeitsplatz() {}

    public Arbeitsplatz(
        String id,
        String deskNumber,
        String roomId,
        String locationId,
        String departmentId,
        String departmentName,
        String name,
        String status,
        Position position,
        List<Equipment> equipment,
        double rotation,
        double width,
        double height
    ) {
        this.id = id;
        this.deskNumber = deskNumber;
        this.roomId = roomId;
        this.locationId = locationId;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.name = name;
        this.status = status;
        this.position = position;
        this.equipment = equipment;
        this.rotation = rotation;
        this.width = width;
        this.height = height;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeskNumber() {
        return deskNumber;
    }

    public void setDeskNumber(String deskNumber) {
        this.deskNumber = deskNumber;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReservedByName() {
        return reservedByName;
    }

    public void setReservedByName(String reservedByName) {
        this.reservedByName = reservedByName;
    }

    public String getReservationUserId() {
        return reservationUserId;
    }

    public void setReservationUserId(String id) {
        this.reservationUserId = id;
    }

    public Date getReservationStart() {
        return reservationStart;
    }

    public void setReservationStart(Date wert) {
        this.reservationStart = wert;
    }

    public Date getReservationEnd() {
        return reservationEnd;
    }

    public void setReservationEnd(Date wert) {
        this.reservationEnd = wert;
    }

    public Date getNextReservationStart() {
        return nextReservationStart;
    }

    public void setNextReservationStart(Date wert) {
        this.nextReservationStart = wert;
    }

    public Date getNextReservationEnd() {
        return nextReservationEnd;
    }

    public void setNextReservationEnd(Date wert) {
        this.nextReservationEnd = wert;
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public List<Equipment> getEquipment() {
        return equipment;
    }

    public void setEquipment(List<Equipment> equipment) {
        this.equipment = equipment;
    }
}
