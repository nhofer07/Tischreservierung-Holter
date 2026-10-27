package at.htlleonding.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import java.util.Date;
import org.bson.codecs.pojo.annotations.BsonId;

@MongoEntity(collection = "reservierungen")
public class Reservierung {

    @BsonId
    private String id;

    private String benutzerId;
    private String arbeitsplatzId;
    private String raumId;
    private String standortId;
    private String abteilungId;
    private Date reservierungAnfang;
    private Date reservierungEnde;
    private String status;

    public Reservierung() {}
}
