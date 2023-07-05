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
    private float confidence;
    private String kb;
    private String user;
    private JsonObject expected;

    @BeforeEach
    void init() {
        this.query = "some test query";
        this.confidence = 0.5f;
        this.kb = "wikidata";
        this.user = "open";


        this.expected = JsonParser.parseString(
                "{\"query\":\"some test query\",\"confidence\":0.5,\"kb\":\"wikidata\",\"user\":\"open\"}"
        ).getAsJsonObject();
    }

    @Test
    void testConfidenceBetween0And1() {
        assertThrows(IllegalArgumentException.class, () -> {
            FakeAnswerQuery fakeAnswerQuery = new FakeAnswerQuery(
                    this.query,
                    -0.1F,
                    this.kb,
                    this.user
            );
        });

        assertThrows(IllegalArgumentException.class, () -> {
            FakeAnswerQuery fakeAnswerQuery = new FakeAnswerQuery(
                    this.query,
                    -1.0F,
                    this.kb,
                    this.user
            );
        });

        assertThrows(IllegalArgumentException.class, () -> {
            FakeAnswerQuery fakeAnswerQuery = new FakeAnswerQuery(
                    this.query,
                    1.1F,
                    this.kb,
                    this.user
            );
        });

        assertThrows(IllegalArgumentException.class, () -> {
            FakeAnswerQuery fakeAnswerQuery = new FakeAnswerQuery(
                    this.query,
                    2.0F,
                    this.kb,
                    this.user
            );
        });
    }

    @Test
    void testToJsonObject() {
        FakeAnswerQuery fakeAnswerQuery = new FakeAnswerQuery(
                this.query,
                this.confidence,
                this.kb,
                this.user
        );

        assertEquals(this.expected.toString(), fakeAnswerQuery.toJsonObject().toString());
    }

    @Test
    void testToString() {
        FakeAnswerQuery fakeAnswerQuery = new FakeAnswerQuery(
                this.query,
                this.confidence,
                this.kb,
                this.user
        );

        assertEquals("FakeAnswerQuery " + this.expected.toString(), fakeAnswerQuery.toString());
    }
}
