package eu.wseresearch.memorizedquestionansweringsystem.messages.request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MemQARequestTests {

    @Test
    void testCheckRequest() {
        MemQARequest memQARequest = new MemQARequest();

        assertThrows(IllegalArgumentException.class, memQARequest::checkRequest);

        memQARequest.setQuestion("test question");
        assertThrows(IllegalArgumentException.class, memQARequest::checkRequest);

        memQARequest.setLanguage("test language");
        assertDoesNotThrow(memQARequest::checkRequest);

        memQARequest.setNumber_of_results_items(-9);
        assertThrows(IllegalArgumentException.class, memQARequest::checkRequest);

        memQARequest.setNumber_of_results_items(9);
        assertDoesNotThrow(memQARequest::checkRequest);

        memQARequest.setDataset("test dataset");
        assertDoesNotThrow(memQARequest::checkRequest);
    }

    @Test
    void testToString() {
        MemQARequest memQARequest = new MemQARequest();

        assertEquals(
                "MemQuestionAnsweringRequest {question=\"null\" language=\"null\" number_of_results_items=\"null\" dataset=\"null\"}",
                memQARequest.toString()
        );

        memQARequest.setQuestion("test question");
        assertEquals(
                "MemQuestionAnsweringRequest {question=\"test question\" language=\"null\" number_of_results_items=\"null\" dataset=\"null\"}",
                memQARequest.toString()
        );

        memQARequest.setLanguage("test language");
        assertEquals(
                "MemQuestionAnsweringRequest {question=\"test question\" language=\"test language\" number_of_results_items=\"null\" dataset=\"null\"}",
                memQARequest.toString()
        );


        memQARequest.setNumber_of_results_items(9);
        assertEquals(
                "MemQuestionAnsweringRequest {question=\"test question\" language=\"test language\" number_of_results_items=\"9\" dataset=\"null\"}",
                memQARequest.toString()
        );

        memQARequest.setDataset("test dataset");
        assertEquals(
                "MemQuestionAnsweringRequest {question=\"test question\" language=\"test language\" number_of_results_items=\"9\" dataset=\"test dataset\"}",
                memQARequest.toString()
        );
    }
}
