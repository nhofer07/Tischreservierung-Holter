package at.htlleonding.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonId;

@MongoEntity(collection = "benutzer")
public class Benutzer {
    @BsonId
    private String id;
    private String vorname;
    private String nachname;
    private String email;
    private String rolle;
    private String abteilungId;
    private String abteilungName;
    private String bevorzugterRaumId;

    public Benutzer() {
    }

    public Benutzer(String id, String vorname, String nachname, String email, String rolle,
                    String abteilungId, String abteilungName, String bevorzugterRaumId) {
        this.id = id;
        this.vorname = vorname;
        this.nachname = nachname;
        this.email = email;
        this.rolle = rolle;
        this.abteilungId = abteilungId;
        this.abteilungName = abteilungName;
        this.bevorzugterRaumId = bevorzugterRaumId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVorname() {
        return vorname;
    }

    public void setVorname(String vorname) {
        this.vorname = vorname;
    }

    public String getNachname() {
        return nachname;
    }

    public void setNachname(String nachname) {
        this.nachname = nachname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRolle() {
        return rolle;
    }

    public void setRolle(String rolle) {
        this.rolle = rolle;
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

    public String getBevorzugterRaumId() {
        return bevorzugterRaumId;
    }

    public void setBevorzugterRaumId(String bevorzugterRaumId) {
        this.bevorzugterRaumId = bevorzugterRaumId;
    }
}
