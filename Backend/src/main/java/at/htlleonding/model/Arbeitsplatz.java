package at.htlleonding.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonId;

import java.util.List;

@MongoEntity(collection = "arbeitsplaetze")
public class Arbeitsplatz {
    @BsonId
    private String id;
    private int tischnr;
    private String raumId;
    private String standortId;
    private String abteilungId;
    private String abteilungName;
    private String name;
    private String status;
    private Position position;
    private List<Equipment> equipment;

    public Arbeitsplatz() {
    }

    public Arbeitsplatz(String id, int tischnr, String raumId, String standortId, String abteilungId,
                        String abteilungName, String name, String status, Position position,
                        List<Equipment> equipment) {
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
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getTischnr() {
        return tischnr;
    }

    public void setTischnr(int tischnr) {
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
