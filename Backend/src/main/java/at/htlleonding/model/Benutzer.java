package at.htlleonding.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

@MongoEntity(collection = "benutzer")
public class Benutzer {

    @BsonId
    private String id;

    @BsonProperty("vorname")
    @JsonProperty("vorname")
    private String firstName;

    @BsonProperty("nachname")
    @JsonProperty("nachname")
    private String lastName;

    private String email;

    @BsonProperty("rolle")
    @JsonProperty("rolle")
    private String role;

    @BsonProperty("abteilungId")
    @JsonProperty("abteilungId")
    private String departmentId;

    @BsonProperty("abteilungName")
    @JsonProperty("abteilungName")
    private String departmentName;

    @BsonProperty("bevorzugterRaumId")
    @JsonProperty("bevorzugterRaumId")
    private String preferredRoomId;

    @BsonProperty("aktiv")
    @JsonProperty("aktiv")
    private boolean active;

    public Benutzer() {}

    public Benutzer(
        String id,
        String firstName,
        String lastName,
        String email,
        String role,
        String departmentId,
        String departmentName,
        String preferredRoomId,
        boolean active
    ) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.preferredRoomId = preferredRoomId;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getPreferredRoomId() {
        return preferredRoomId;
    }

    public boolean isActive() {
        return active;
    }
}
