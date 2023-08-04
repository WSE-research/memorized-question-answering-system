package eu.wseresearch.fakequestionansweringsystem.messages.response.fakeanswer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FakeAnswerQueryTests {
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
        FakeAnswerQuery fakeAnswerQuery = new FakeAnswerQuery(
                this.query,
                this.correct,
                0.66f,
                this.kb,
                this.user
        );

        assertEquals(this.expected.toString(), fakeAnswerQuery.toJsonObject().toString());
    }

    @Test
    void testToString() {
        FakeAnswerQuery fakeAnswerQuery = new FakeAnswerQuery(
                this.query,
                this.correct,
                0.66f,
                this.kb,
                this.user
        );

        assertEquals("FakeAnswerQuery " + this.expected.toString(), fakeAnswerQuery.toString());
    }
}
