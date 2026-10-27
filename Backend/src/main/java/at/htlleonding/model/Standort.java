package at.htlleonding.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

@MongoEntity(collection = "standorte")
public class Standort {

    @BsonId
    private String id;

    private String name;

    @BsonProperty("adresse")
    @JsonProperty("adresse")
    private String address;

    @BsonProperty("ort")
    @JsonProperty("ort")
    private String city;

    public Standort() {}

    public Standort(String id, String name, String address, String city) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.city = city;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
