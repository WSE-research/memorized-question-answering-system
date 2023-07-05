package eu.wseresearch.fakequestionansweringsystem.messages.request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FakeQuestionAnsweringRequestTests {

    @Test
    void testCheckRequest() {
        FakeQuestionAnsweringRequest fakeQuestionAnsweringRequest = new FakeQuestionAnsweringRequest();

        assertThrows(IllegalArgumentException.class, fakeQuestionAnsweringRequest::checkRequest);

        fakeQuestionAnsweringRequest.setQuestion("test question");
        assertThrows(IllegalArgumentException.class, fakeQuestionAnsweringRequest::checkRequest);

        fakeQuestionAnsweringRequest.setNumber_of_results_items(9);
        assertThrows(IllegalArgumentException.class, fakeQuestionAnsweringRequest::checkRequest);

        fakeQuestionAnsweringRequest.setDataset("test dataset");
        assertDoesNotThrow(fakeQuestionAnsweringRequest::checkRequest);
    }

    @Test
    void testToString() {
        FakeQuestionAnsweringRequest fakeQuestionAnsweringRequest = new FakeQuestionAnsweringRequest();

        assertEquals(
                "FakeQuestionAnsweringRequest {question=\"null\"number_of_results_items=\"null\"dataset=\"null\"}",
                fakeQuestionAnsweringRequest.toString()
        );

        fakeQuestionAnsweringRequest.setQuestion("test question");
        assertEquals(
                "FakeQuestionAnsweringRequest {question=\"test question\"number_of_results_items=\"null\"dataset=\"null\"}",
                fakeQuestionAnsweringRequest.toString()
        );


        fakeQuestionAnsweringRequest.setNumber_of_results_items(9);
        assertEquals(
                "FakeQuestionAnsweringRequest {question=\"test question\"number_of_results_items=\"9\"dataset=\"null\"}",
                fakeQuestionAnsweringRequest.toString()
        );

        fakeQuestionAnsweringRequest.setDataset("test dataset");
        assertEquals(
                "FakeQuestionAnsweringRequest {question=\"test question\"number_of_results_items=\"9\"dataset=\"test dataset\"}",
                fakeQuestionAnsweringRequest.toString()
        );
    }
}
