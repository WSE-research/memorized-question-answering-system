package eu.wseresearch.memorizedquestionansweringsystem.messages.response.memanswer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MemAnswerQueryTests {
    private String query;
    private boolean correct;
    private String kb;
    private String user;
    private JsonObject expected;

    @BeforeEach
    void init() {
        this.query = "some test query";
        this.correct = true;
        this.kb = "wikidata";
        this.user = "open";


        this.expected = JsonParser.parseString(
                "{\"query\":\"some test query\",\"correct\":true, \"confidence\":0.66,\"kb\":\"wikidata\",\"user\":\"open\"}"
        ).getAsJsonObject();
    }

    @Test
    void testToJsonObject() {
        MemAnswerQuery memAnswerQuery = new MemAnswerQuery(
                this.query,
                this.correct,
                0.66f,
                this.kb,
                this.user
        );

        assertEquals(this.expected.toString(), memAnswerQuery.toJsonObject().toString());
    }

    @Test
    void testToString() {
        MemAnswerQuery memAnswerQuery = new MemAnswerQuery(
                this.query,
                this.correct,
                0.66f,
                this.kb,
                this.user
        );

        assertEquals("MemAnswerQuery " + this.expected.toString(), memAnswerQuery.toString());
    }
}
