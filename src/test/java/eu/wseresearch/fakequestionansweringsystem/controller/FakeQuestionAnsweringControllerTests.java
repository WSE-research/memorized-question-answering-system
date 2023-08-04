package eu.wseresearch.fakequestionansweringsystem.controller;

import com.google.gson.JsonObject;
import eu.wseresearch.fakequestionansweringsystem.Application;
import eu.wseresearch.fakequestionansweringsystem.FakeQuestionAnsweringSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class FakeQuestionAnsweringControllerTests {
    public static final String ENDPOINT = "/fake-question-answering-system";

    private final WebApplicationContext applicationContext;

    private MockMvc mockMvc;

    @MockBean
    private FakeQuestionAnsweringSystem mockedFakeQuestionAnsweringSystem;

    FakeQuestionAnsweringControllerTests(
            @Autowired WebApplicationContext applicationContext
    ) {
        this.applicationContext = applicationContext;
    }

    @BeforeEach
    public void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.applicationContext).build();
    }

    @Test
    void testGetResponse() throws Exception {
        mockMvc.perform(get(ENDPOINT))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void testPostEmptyBodyResponse() throws Exception {
        mockMvc.perform(post(ENDPOINT).header("Content-Type", "application/json").content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void testPostMissingArgumentResponse() throws Exception {
        // missing question
        mockMvc.perform(post(ENDPOINT)
                        .header("Content-Type", "application/json")
                        .content("{\"number_of_results_items\":5,\"dataset\":\"QALD-9-plus-test-wikidata\"}"))
                .andExpect(status().isUnprocessableEntity());

        // missing number_of_results_items
        mockMvc.perform(post(ENDPOINT)
                        .header("Content-Type", "application/json")
                        .content("{\"question\":\"What is the revenue of IBM?\",\"dataset\":\"QALD-9-plus-test-wikidata\"}"))
                .andExpect(status().isUnprocessableEntity());

        // missing dataset
        mockMvc.perform(post(ENDPOINT)
                        .header("Content-Type", "application/json")
                        .content("{\"question\":\"What is the revenue of IBM?\",\"number_of_results_items\":5}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void testPostProcessErrorResponse() throws Exception {
        when(mockedFakeQuestionAnsweringSystem
                .process(any(String.class), any(String.class), any(Integer.class), any(String.class)))
                .thenThrow(new IllegalArgumentException("something went wrong"));

        mockMvc.perform(post(ENDPOINT)
                        .header("Content-Type", "application/json")
                        .content("{\"question\":\"What is the revenue of IBM?\",\"language\":\"en\",\"number_of_results_items\":5,\"dataset\":\"QALD-9-plus-test-wikidata\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string("java.lang.IllegalArgumentException - something went wrong"));
    }

    @Test
    void testPostResponse() throws Exception {
        JsonObject responseJsonObject = new JsonObject();
        responseJsonObject.addProperty("some", "answer");

        when(mockedFakeQuestionAnsweringSystem.process(any(String.class), any(String.class), any(Integer.class), any(String.class))).thenReturn(responseJsonObject);

        mockMvc.perform(post(ENDPOINT)
                        .header("Content-Type", "application/json")
                        .content("{\"question\":\"What is the revenue of IBM?\",\"language\":\"en\",\"number_of_results_items\":5,\"dataset\":\"QALD-9-plus-test-wikidata\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"some\":\"answer\"}"));
    }

    @Test
    void testPutResponse() throws Exception {
        mockMvc.perform(put(ENDPOINT))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void testDeleteResponse() throws Exception {
        mockMvc.perform(delete(ENDPOINT))
                .andExpect(status().isMethodNotAllowed());
    }
}
