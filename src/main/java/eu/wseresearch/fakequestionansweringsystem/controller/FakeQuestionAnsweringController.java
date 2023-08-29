package eu.wseresearch.fakequestionansweringsystem.controller;

import com.google.gson.JsonObject;
import eu.wseresearch.fakequestionansweringsystem.FakeQuestionAnsweringSystem;
import eu.wseresearch.fakequestionansweringsystem.messages.request.FakeQuestionAnsweringRequest;

import eu.wseresearch.fakequestionansweringsystem.messages.response.fakeanswer.FakeAnswer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Fake Question Answering System", description = "Fake Question Answering System APIs")
@RestController
@CrossOrigin(origins = "${cross.origin}")
public class FakeQuestionAnsweringController {
    public static final String ENDPOINT = "/fake-question-answering-system";
    private static final Logger LOGGER = LoggerFactory.getLogger(FakeQuestionAnsweringController.class);

    private final FakeQuestionAnsweringSystem fakeQuestionAnsweringSystem;

    public FakeQuestionAnsweringController(
            @Autowired FakeQuestionAnsweringSystem fakeQuestionAnsweringSystem
    ) {
        this.fakeQuestionAnsweringSystem = fakeQuestionAnsweringSystem;
    }

    @Operation(
            hidden = true
    )
    @GetMapping(ENDPOINT)
    @ResponseStatus(code = HttpStatus.METHOD_NOT_ALLOWED, reason = "This method is not intend to be used.")
    void indexGet() {
        LOGGER.error("GET method not allowed for {} endpoint", ENDPOINT);
    }

    @Operation(
            summary = "Create a fake answer for a question",
            description = "Create a fake answer for a question, form the given request. Using the dataset and the number of results items."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {@Content(schema = @Schema(implementation = FakeAnswer.class), mediaType = "application/json", examples = {@ExampleObject(name = "What is the revenue of IBM?", description = "Send a request with a request body like: { \"question\": \"What is the revenue of IBM?\", \"language\":\"en\", \"number_of_results_items\": 5, \"dataset\": \"QALD-9-plus-test-wikidata\" }", value = "{ \"question\": \"What is the revenue of IBM?\", \"languages\": [ \"en\" ], \"knowledgebases\": [ \"wikidata\" ], \"queries\": [ { \"query\": \" SELECT ?o1 WHERE { <http://www.wikidata.org/entity/Q37156>  <http://www.wikidata.org/prop/direct/P2139>  ?o1 .  }\", \"correct\": true, \"confidence\": 0.86, \"kb\": \"wikidata\", \"user\": \"open\" }, { \"query\": \"SELECT ?o1 WHERE { <http://www.wikidata.org/entity/Q214341>  <http://www.wikidata.org/prop/direct/P2196>  ?o1 .  }\", \"correct\": false, \"confidence\": 0.05, \"kb\": \"wikidata\", \"user\": \"open\" }, { \"query\": \"PREFIX wdt: <http://www.wikidata.org/prop/direct/> PREFIX wd: <http://www.wikidata.org/entity/> SELECT ( COUNT( DISTINCT ?uri ) AS ?c ) WHERE { ?uri wdt:P112 wd:Q36215 . ?uri wdt:P31/wdt:P279* wd:Q783794 . } \", \"correct\": false, \"confidence\": 0.34, \"kb\": \"wikidata\", \"user\": \"open\" }, { \"query\": \"PREFIX wd: <http://www.wikidata.org/entity/> PREFIX wdt: <http://www.wikidata.org/prop/direct/> SELECT ?date WHERE { wd:Q3266236 wdt:P170 ?author . ?author wdt:P570 ?date }\", \"correct\": false, \"confidence\": 0.01, \"kb\": \"wikidata\", \"user\": \"open\" }, { \"query\": \"SELECT DISTINCT ?uri WHERE { <http://www.wikidata.org/entity/Q1124023> <http://www.wikidata.org/prop/direct/P559> ?uri}\", \"correct\": false, \"confidence\": 0.1, \"kb\": \"wikidata\", \"user\": \"open\" } ] }")})}),
            @ApiResponse(responseCode = "201", content = {@Content(schema = @Schema())}, description = "When the created fake answer is null"),
            @ApiResponse(responseCode = "422", content = {@Content(schema = @Schema(), mediaType = "text/plain", examples = {@ExampleObject(name = "Empty Question", description = "Send a request with an empty question", value = "java.lang.IllegalArgumentException - request is invalid: question is null or blank")})}),
            @ApiResponse(responseCode = "500", content = {@Content(schema = @Schema())})
    })
    @PostMapping(value = ENDPOINT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> indexPost(
            @RequestBody FakeQuestionAnsweringRequest myFakeQuestionAnsweringRequest
    ) {
        LOGGER.debug("request body: {}", myFakeQuestionAnsweringRequest);

        // check if the request is valid
        try {
            myFakeQuestionAnsweringRequest.checkRequest();
        } catch (Exception e) {
            LOGGER.error("{} - {}", e.getClass().getName(), e.getMessage());
            return new ResponseEntity<>(String.format("%s - %s", e.getClass().getName(), e.getMessage()), HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // process the request
        try {
            // create the fake answer
            JsonObject createdAnswer = this.fakeQuestionAnsweringSystem.createAnswer(
                    myFakeQuestionAnsweringRequest.getQuestion(),
                    myFakeQuestionAnsweringRequest.getLanguage(),
                    myFakeQuestionAnsweringRequest.getNumber_of_results_items(),
                    myFakeQuestionAnsweringRequest.getDataset()
            );

            if (createdAnswer == null) {
                LOGGER.warn(
                        "createdAnswer was null.\n question: {}\n language: {}\n number of results items: {}\n dataset: {}",
                        myFakeQuestionAnsweringRequest.getQuestion(),
                        myFakeQuestionAnsweringRequest.getLanguage(),
                        myFakeQuestionAnsweringRequest.getNumber_of_results_items(),
                        myFakeQuestionAnsweringRequest.getDataset()
                );
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            // return the answer
            return new ResponseEntity<>(createdAnswer.toString(), HttpStatus.OK);

        } catch (Exception e) {
            LOGGER.error("{} - {}", e.getClass().getName(), e.getMessage());
            return new ResponseEntity<>(String.format("%s - %s", e.getClass().getName(), e.getMessage()), HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    @Operation(
            hidden = true
    )
    @PutMapping(ENDPOINT)
    @ResponseStatus(code = HttpStatus.METHOD_NOT_ALLOWED, reason = "This method is not intend to be used.")
    void indexPut() {
        LOGGER.error("PUT method not allowed for {} endpoint", ENDPOINT);
    }

    @Operation(
            hidden = true
    )
    @DeleteMapping(ENDPOINT)
    @ResponseStatus(code = HttpStatus.METHOD_NOT_ALLOWED, reason = "This method is not intend to be used.")
    void indexDelete() {
        LOGGER.error("DELETE method not allowed for {} endpoint", ENDPOINT);
    }
}
