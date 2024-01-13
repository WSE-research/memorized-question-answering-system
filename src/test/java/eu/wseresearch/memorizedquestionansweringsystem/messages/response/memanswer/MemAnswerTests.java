package eu.wseresearch.memorizedquestionansweringsystem.messages.response.memanswer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MemAnswerTests {
    private String question;
    private List<String> languages;
    private List<String> knowledgebases;
    private List<MemAnswerQuery> queries;
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

        ArrayList<MemAnswerQuery> queries = new ArrayList<>();
        MemAnswerQuery memAnswerQuery = new MemAnswerQuery("some test query", true , 0.66f, "wikidata", "open");
        MemAnswerQuery memAnswerQuery1 = new MemAnswerQuery("some other test query", false, 0.33f, "wikidata", "open");
        queries.add(memAnswerQuery);
        queries.add(memAnswerQuery1);
        this.queries = queries;

        this.expected = JsonParser.parseString(
                "{\"question\":\"Test Question ?\",\"languages\":[\"en\"],\"knowledgebases\":[\"wikidata\"],\"queries\":[{\"query\":\"some test query\",\"correct\":true, \"confidence\":0.66,\"kb\":\"wikidata\",\"user\":\"open\"},{\"query\":\"some other test query\",\"correct\":false, \"confidence\":0.33, \"kb\":\"wikidata\",\"user\":\"open\"}]}"
        ).getAsJsonObject();
    }

    @Test
    void testToJsonObject() {
        MemAnswer memAnswer = new MemAnswer(
                this.question,
                this.languages,
                this.knowledgebases,
                this.queries
        );

        assertEquals(this.expected.toString(), memAnswer.toJsonObject().toString());
    }

    @Test
    void testToString() {
        MemAnswer memAnswer = new MemAnswer(
                this.question,
                this.languages,
                this.knowledgebases,
                this.queries
        );

        assertEquals("MemAnswer " + this.expected.toString(), memAnswer.toString());
    }
}
