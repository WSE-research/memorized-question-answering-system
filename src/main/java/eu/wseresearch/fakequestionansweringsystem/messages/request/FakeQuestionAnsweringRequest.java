package eu.wseresearch.fakequestionansweringsystem.messages.request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FakeQuestionAnsweringRequest {
    private static final Logger LOGGER = LoggerFactory.getLogger(FakeQuestionAnsweringRequest.class);

    private String question;
    private String language;
    private Integer number_of_results_items;
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
        if (this.number_of_results_items == null || this.number_of_results_items < 1) {
            LOGGER.error("request is invalid: number_of_results_items is null or less than 1");
            throw new IllegalArgumentException("request is invalid: number_of_results_items is null or less than 1");
        }
        if (this.dataset == null || this.dataset.isBlank()) {
            LOGGER.error("request is invalid: dataset is null or blank");
            throw new IllegalArgumentException("request is invalid: dataset is null or blank");
        }

        LOGGER.debug("request is valid");
    }

    @Override
    public String toString() {
        return "FakeQuestionAnsweringRequest {" + "question=\"" + this.question + "\"" + " language=\"" + this.language + "\"" + " number_of_results_items=\"" + this.number_of_results_items + "\"" + " dataset=\"" + this.dataset + "\"" + "}";
    }
}
