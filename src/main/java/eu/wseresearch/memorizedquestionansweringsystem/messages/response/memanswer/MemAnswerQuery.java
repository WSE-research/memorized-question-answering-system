package eu.wseresearch.memorizedquestionansweringsystem.messages.response.memanswer;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemAnswerQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemAnswerQuery.class);

    private String query;
    private boolean correct;
    private float confidence;
    private String kb;
    private String user;

    public MemAnswerQuery(String query, boolean correct, float confidence, String kb, String user) {

        this.query = query;
        this.correct = correct;
        this.confidence = confidence;
        this.kb = kb;
        this.user = user;
    }

    public String getQuery() {
        return query;
    }

    public boolean getCorrect() {
        return correct;
    }

    public float getConfidence() {
        return confidence;
    }

    public String getKb() {
        return kb;
    }

    public String getUser() {
        return user;
    }

    /**
     * @return MemAnswerQuery as JsonObject
     */
    public JsonObject toJsonObject() {
        JsonObject memAnswerQuery = new JsonObject();

        memAnswerQuery.addProperty("query", this.getQuery());
        memAnswerQuery.addProperty("correct", this.getCorrect());
        memAnswerQuery.addProperty("confidence", this.getConfidence());
        memAnswerQuery.addProperty("kb", this.getKb());
        memAnswerQuery.addProperty("user", this.getUser());

        return memAnswerQuery;
    }

    @Override
    public String toString() {
        return "MemAnswerQuery " + this.toJsonObject().toString();
    }
}
