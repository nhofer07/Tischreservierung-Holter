package at.htlleonding.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonId;

@MongoEntity(collection = "standorte")
public class Standort {
    @BsonId
    private String id;
    private String name;
    private String adresse;
    private String ort;

    public Standort() {
    }

    public Standort(String id, String name, String adresse, String ort) {
        this.id = id;
        this.name = name;
        this.adresse = adresse;
        this.ort = ort;
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

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getOrt() {
        return ort;
    }

    public void setOrt(String ort) {
        this.ort = ort;
    }
}
