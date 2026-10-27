package at.htlleonding.repo;

import static org.bson.codecs.configuration.CodecRegistries.*;
import static org.junit.jupiter.api.Assertions.*;

import at.htlleonding.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClientSettings;
import java.util.List;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.junit.jupiter.api.Test;

class ModelSerializationTest {

    private final Arbeitsplatz desk = new Arbeitsplatz(
        "DESK",
        "101",
        "ROOM",
        "LOCATION",
        "IT",
        "IT",
        "Desk",
        "frei",
        new Position(20, 30),
        List.of(new Equipment("Monitor")),
        0,
        12,
        11
    );

    @Test
    void englishAccessorsPreserveExistingJsonFields() {
        var mapper = new ObjectMapper();
        var json = mapper.valueToTree(desk);
        assertEquals("101", json.get("tischnr").asText());
        assertEquals("ROOM", json.get("raumId").asText());
        assertEquals("IT", json.get("abteilungId").asText());
        assertEquals(12, json.get("breite").asDouble());
        assertFalse(json.has("deskNumber"));
        assertFalse(json.has("roomId"));
        assertFalse(json.has("departmentId"));
        assertFalse(json.has("width"));

        var user = mapper.valueToTree(
            new Benutzer(
                "USER",
                "Anna",
                "Test",
                "test@example.test",
                "USER",
                "IT",
                "IT",
                "ROOM",
                true
            )
        );
        assertEquals("Anna", user.get("vorname").asText());
        assertTrue(user.get("aktiv").asBoolean());
        assertFalse(user.has("firstName"));
        assertFalse(user.has("active"));
    }

    @Test
    void englishAccessorsPreserveExistingMongoFields() {
        var registry = fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );
        var document = new BsonDocument();
        registry
            .get(Arbeitsplatz.class)
            .encode(new BsonDocumentWriter(document), desk, EncoderContext.builder().build());
        assertEquals("101", document.getString("tischnr").getValue());
        assertEquals("ROOM", document.getString("raumId").getValue());
        assertEquals("LOCATION", document.getString("standortId").getValue());
        assertEquals(12, document.getDouble("breite").getValue());
        assertFalse(document.containsKey("deskNumber"));
        assertFalse(document.containsKey("roomId"));
        assertFalse(document.containsKey("locationId"));
        assertFalse(document.containsKey("width"));
    }
}
