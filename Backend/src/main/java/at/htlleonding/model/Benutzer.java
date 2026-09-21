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
    private boolean aktiv;

    public Benutzer() {
    }

    public Benutzer(String id, String vorname, String nachname, String email, String rolle,
                    String abteilungId, String abteilungName, String bevorzugterRaumId, boolean aktiv) {
        this.id = id;
        this.vorname = vorname;
        this.nachname = nachname;
        this.email = email;
        this.rolle = rolle;
        this.abteilungId = abteilungId;
        this.abteilungName = abteilungName;
        this.bevorzugterRaumId = bevorzugterRaumId;
        this.aktiv = aktiv;
    }

    public String getId() { return id; }
    public String getVorname() { return vorname; }
    public String getNachname() { return nachname; }
    public String getEmail() { return email; }
    public String getRolle() { return rolle; }
    public String getAbteilungId() { return abteilungId; }
    public String getAbteilungName() { return abteilungName; }
    public String getBevorzugterRaumId() { return bevorzugterRaumId; }
    public boolean isAktiv() { return aktiv; }
}
