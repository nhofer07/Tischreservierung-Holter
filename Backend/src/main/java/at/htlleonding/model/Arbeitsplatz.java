package at.htlleonding.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonId;

import java.util.List;
import java.util.Date;

@MongoEntity(collection = "arbeitsplaetze")
public class Arbeitsplatz {
    @BsonId
    private String id;
    private String tischnr;
    private String raumId;
    private String standortId;
    private String abteilungId;
    private String abteilungName;
    private String name;
    private String status;
    private String reserviertVon;
    private String reservierungBenutzerId;
    private Date reservierungAnfang;
    private Date reservierungEnde;
    private double rotation;
    private double breite;
    private double hoehe;
    private Position position;
    private List<Equipment> equipment;

    public Arbeitsplatz() {
    }

    public Arbeitsplatz(String id, String tischnr, String raumId, String standortId, String abteilungId,
                        String abteilungName, String name, String status, Position position,
                        List<Equipment> equipment, double rotation, double breite, double hoehe) {
        this.id = id;
        this.tischnr = tischnr;
        this.raumId = raumId;
        this.standortId = standortId;
        this.abteilungId = abteilungId;
        this.abteilungName = abteilungName;
        this.name = name;
        this.status = status;
        this.position = position;
        this.equipment = equipment;
        this.rotation = rotation;
        this.breite = breite;
        this.hoehe = hoehe;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTischnr() {
        return tischnr;
    }

    public void setTischnr(String tischnr) {
        this.tischnr = tischnr;
    }

    public String getRaumId() {
        return raumId;
    }

    public void setRaumId(String raumId) {
        this.raumId = raumId;
    }

    public String getStandortId() {
        return standortId;
    }

    public void setStandortId(String standortId) {
        this.standortId = standortId;
    }

    public String getAbteilungId() {
        return abteilungId;
    }

    public void setAbteilungId(String abteilungId) {
        this.abteilungId = abteilungId;
    }

    public String getAbteilungName() {
        return abteilungName;
    }

    public void setAbteilungName(String abteilungName) {
        this.abteilungName = abteilungName;
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

    public String getReserviertVon() { return reserviertVon; }

    public void setReserviertVon(String reserviertVon) { this.reserviertVon = reserviertVon; }
    public String getReservierungBenutzerId() { return reservierungBenutzerId; }
    public void setReservierungBenutzerId(String id) { this.reservierungBenutzerId = id; }
    public Date getReservierungAnfang() { return reservierungAnfang; }
    public void setReservierungAnfang(Date wert) { this.reservierungAnfang = wert; }
    public Date getReservierungEnde() { return reservierungEnde; }
    public void setReservierungEnde(Date wert) { this.reservierungEnde = wert; }
    public double getRotation() { return rotation; }
    public void setRotation(double rotation) { this.rotation = rotation; }
    public double getBreite() { return breite; }
    public double getHoehe() { return hoehe; }

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
