package eu.wseresearch.memorizedquestionansweringsystem.messages.response.memanswer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public class MemAnswer {
    private String question;
    private List<String> languages;
    private List<String> knowledgebases;
    private List<MemAnswerQuery> queries;

    public MemAnswer(String question, List<String> languages, List<String> knowledgebases, List<MemAnswerQuery> queries) {
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

    public List<MemAnswerQuery> getQueries() {
        return queries;
    }

    /**
     * @return MemAnswer as JsonObject
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
        for (MemAnswerQuery query : this.getQueries()) {
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
        return "MemAnswer " + this.toJsonObject().toString();
    }
}
