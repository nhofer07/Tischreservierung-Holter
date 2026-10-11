package at.htlleonding.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@MongoEntity(collection = "raeume")
public class Raum {
    @BsonId
    private String id;
    private String name;
    @BsonProperty("stockwerk")
    @JsonProperty("stockwerk")
    private String floor;
    @BsonProperty("standortId")
    @JsonProperty("standortId")
    private String locationId;
    @BsonProperty("abteilungId")
    @JsonProperty("abteilungId")
    private String departmentId;
    @BsonProperty("abteilungName")
    @JsonProperty("abteilungName")
    private String departmentName;
    @BsonProperty("elemente")
    @JsonProperty("elemente")
    private List<RaumElement> elements;
    @BsonProperty("arbeitsplaetze")
    @JsonProperty("arbeitsplaetze")
    private List<Arbeitsplatz> workstations;

    public Raum() {
    }

    public Raum(String id, String name, String floor, String locationId, String departmentId,
                String departmentName, List<RaumElement> elements, List<Arbeitsplatz> workstations) {
        this.id = id;
        this.name = name;
        this.floor = floor;
        this.locationId = locationId;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.elements = elements;
        this.workstations = workstations;
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

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
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

    public List<RaumElement> getElements() { return elements; }
    public void setElements(List<RaumElement> elements) { this.elements = elements; }

    public List<Arbeitsplatz> getWorkstations() {
        return workstations;
    }

    public void setWorkstations(List<Arbeitsplatz> workstations) {
        this.workstations = workstations;
    }
}
