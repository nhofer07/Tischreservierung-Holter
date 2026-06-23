package at.htlleonding.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonId;

import java.util.List;

@MongoEntity(collection = "raeume")
public class Raum {
    @BsonId
    private String id;
    private String name;
    private String stockwerk;
    private String standortId;
    private String abteilungId;
    private String abteilungName;
    private List<Arbeitsplatz> arbeitsplaetze;

    public Raum() {
    }

    public Raum(String id, String name, String stockwerk, String standortId, String abteilungId,
                String abteilungName, List<Arbeitsplatz> arbeitsplaetze) {
        this.id = id;
        this.name = name;
        this.stockwerk = stockwerk;
        this.standortId = standortId;
        this.abteilungId = abteilungId;
        this.abteilungName = abteilungName;
        this.arbeitsplaetze = arbeitsplaetze;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStockwerk() {
        return stockwerk;
    }

    public void setStockwerk(String stockwerk) {
        this.stockwerk = stockwerk;
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

    public List<Arbeitsplatz> getArbeitsplaetze() {
        return arbeitsplaetze;
    }

    public void setArbeitsplaetze(List<Arbeitsplatz> arbeitsplaetze) {
        this.arbeitsplaetze = arbeitsplaetze;
    }
}
