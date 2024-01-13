package eu.wseresearch.memorizedquestionansweringsystem.messages.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemQARequest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemQARequest.class);

    @Schema(example = "What is the revenue of IBM?", description = "The question to be answered", required = true)
    private String question;
    @Schema(example = "en", description = "The language of the question", required = true)
    private String language;
    @Schema(example = "5", description = "The number of results items", required = false)
    private Integer number_of_results_items;
    @Schema(example = "QALD-9-plus-test-wikidata", description = "The dataset that should be used", required = false)
    private String dataset;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getNumber_of_results_items() {
        return number_of_results_items;
    }

    public void setNumber_of_results_items(Integer number_of_results_items) {
        this.number_of_results_items = number_of_results_items;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    /**
     * Checks if the request is valid.
     *
     * @throws IllegalArgumentException if the request is invalid
     */
    public void checkRequest() {
        if (this.question == null || this.question.isBlank()) {
            LOGGER.error("request is invalid: question is null or blank");
            throw new IllegalArgumentException("request is invalid: question is null or blank");
        }
        if (this.language == null || this.language.isBlank()) {
            LOGGER.error("request is invalid: language is null or blank");
            throw new IllegalArgumentException("request is invalid: language is null or blank");
        }
        if (this.number_of_results_items != null && this.number_of_results_items < 0) {
            LOGGER.error("request is invalid: number_of_results_items is less than 0");
            throw new IllegalArgumentException("request is invalid: number_of_results_items is less than 0");
        }

        LOGGER.debug("request is valid");
    }

    @Override
    public String toString() {
        return "MemQuestionAnsweringRequest {" + "question=\"" + this.question + "\"" + " language=\"" + this.language + "\"" + " number_of_results_items=\"" + this.number_of_results_items + "\"" + " dataset=\"" + this.dataset + "\"" + "}";
    }
}
