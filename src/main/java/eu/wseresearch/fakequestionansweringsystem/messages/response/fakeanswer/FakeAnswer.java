package eu.wseresearch.fakequestionansweringsystem.messages.response.fakeanswer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public class FakeAnswer {
    private String question;
    private List<String> languages;
    private List<String> knowledgebases;
    private List<FakeAnswerQuery> queries;

    public FakeAnswer(String question, List<String> languages, List<String> knowledgebases, List<FakeAnswerQuery> queries) {
        this.question = question;
        this.languages = languages;
        this.knowledgebases = knowledgebases;
        this.queries = queries;
    }

    public String getQuestion() {
        return question;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public List<String> getKnowledgebases() {
        return knowledgebases;
    }

    public List<FakeAnswerQuery> getQueries() {
        return queries;
    }

    /**
     * @return FakeAnswer as JsonObject
     */
    public JsonObject toJsonObject() {
        JsonObject answer = new JsonObject();

        JsonArray languagesJsonArray = new JsonArray();
        for (String language : this.getLanguages()) {
            languagesJsonArray.add(language);
        }

        JsonArray knowledgebasesJsonArray = new JsonArray();
        for (String knowledgebase : this.getKnowledgebases()) {
            knowledgebasesJsonArray.add(knowledgebase);
        }

        JsonArray queriesJsonArray = new JsonArray();
        for (FakeAnswerQuery query : this.getQueries()) {
            queriesJsonArray.add(query.toJsonObject());
        }

        answer.addProperty("question", this.getQuestion());
        answer.add("languages", languagesJsonArray);
        answer.add("knowledgebases", knowledgebasesJsonArray);
        answer.add("queries", queriesJsonArray);

        return answer;
    }

    @Override
    public String toString() {
        return "FakeAnswer " + this.toJsonObject().toString();
    }
}
