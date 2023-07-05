package eu.wseresearch.fakequestionansweringsystem.messages.response.fakeanswer;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FakeAnswerQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger(FakeAnswerQuery.class);

    private String query;
    private float confidence;
    private String kb;
    private String user;

    public FakeAnswerQuery(String query, float confidence, String kb, String user) {
        if (confidence < 0 || confidence > 1) {
            LOGGER.error("Confidence must be between 0 and 1");
            throw new IllegalArgumentException("Confidence must be between 0 and 1");
        }

        this.query = query;
        this.confidence = confidence;
        this.kb = kb;
        this.user = user;
    }

    public String getQuery() {
        return query;
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
     * @return FakeAnswerQuery as JsonObject
     */
    public JsonObject toJsonObject() {
        JsonObject fakeAnswerQuery = new JsonObject();

        fakeAnswerQuery.addProperty("query", this.getQuery());
        fakeAnswerQuery.addProperty("confidence", this.getConfidence());
        fakeAnswerQuery.addProperty("kb", this.getKb());
        fakeAnswerQuery.addProperty("user", this.getUser());

        return fakeAnswerQuery;
    }

    @Override
    public String toString() {
        return "FakeAnswerQuery " + this.toJsonObject().toString();
    }
}
