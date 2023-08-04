package eu.wseresearch.fakequestionansweringsystem.messages.response.fakeanswer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FakeAnswerTests {
    private String question;
    private List<String> languages;
    private List<String> knowledgebases;
    private List<FakeAnswerQuery> queries;
    private JsonObject expected;

    @BeforeEach
    void init() {
        this.question = "Test Question ?";

        ArrayList<String> languages = new ArrayList<>();
        languages.add("en");
        this.languages = languages;

        ArrayList<String> knowledgebases = new ArrayList<>();
        knowledgebases.add("wikidata");
        this.knowledgebases = knowledgebases;

        ArrayList<FakeAnswerQuery> queries = new ArrayList<>();
        FakeAnswerQuery fakeAnswerQuery0 = new FakeAnswerQuery("some test query", true , 0.66f, "wikidata", "open");
        FakeAnswerQuery fakeAnswerQuery1 = new FakeAnswerQuery("some other test query", false, 0.33f, "wikidata", "open");
        queries.add(fakeAnswerQuery0);
        queries.add(fakeAnswerQuery1);
        this.queries = queries;

        this.expected = JsonParser.parseString(
                "{\"question\":\"Test Question ?\",\"languages\":[\"en\"],\"knowledgebases\":[\"wikidata\"],\"queries\":[{\"query\":\"some test query\",\"correct\":true, \"confidence\":0.66,\"kb\":\"wikidata\",\"user\":\"open\"},{\"query\":\"some other test query\",\"correct\":false, \"confidence\":0.33, \"kb\":\"wikidata\",\"user\":\"open\"}]}"
        ).getAsJsonObject();
    }

    @Test
    void testToJsonObject() {
        FakeAnswer fakeAnswer = new FakeAnswer(
                this.question,
                this.languages,
                this.knowledgebases,
                this.queries
        );

        assertEquals(this.expected.toString(), fakeAnswer.toJsonObject().toString());
    }

    @Test
    void testToString() {
        FakeAnswer fakeAnswer = new FakeAnswer(
                this.question,
                this.languages,
                this.knowledgebases,
                this.queries
        );

        assertEquals("FakeAnswer " + this.expected.toString(), fakeAnswer.toString());
    }
}
