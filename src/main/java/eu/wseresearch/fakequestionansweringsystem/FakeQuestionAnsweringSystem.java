package eu.wseresearch.fakequestionansweringsystem;

import com.google.gson.JsonObject;
import eu.wseresearch.fakequestionansweringsystem.exception.DatasetNotExist;
import eu.wseresearch.fakequestionansweringsystem.exception.QuestionNotExist;
import eu.wseresearch.fakequestionansweringsystem.messages.response.fakeanswer.FakeAnswer;
import eu.wseresearch.fakequestionansweringsystem.messages.response.fakeanswer.FakeAnswerQuery;
import eu.wseresearch.fakequestionansweringsystem.triplestoreconnector.TripleStoreConnector;
import eu.wseresearch.fakequestionansweringsystem.triplestoreconnector.exception.SparqlQueryFailed;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

@Component
public class FakeQuestionAnsweringSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(FakeQuestionAnsweringSystem.class);

    private final TripleStoreConnector tripleStoreConnector;
    private final Random r = new Random();
    private final String knowledgebase;
    private final String user;

    private String FILENAME_GET_ALL_DATASETS = "/queries/select_dataset_labels.rq";
    private String FILENAME_GET_DATASET_BY_NAME = "/queries/select_dataset_by_name.rq";
    private String FILENAME_GET_QUESTION_BY_QUESTIONTEXT = "/queries/select_question_by_questiontext.rq";
    private String FILENAME_GET_SPARQL_QUERY_FROM_QUESTION_BY_RESOURCE = "/queries/select_sparql_query_from_question_by_resource.rq";
    private String FILENAME_GET_SPARQL_QUERY_FROM_DATASET_WITHOUT_CORRECT_ANSWER = "/queries/select_sparql_query_from_dataset_without_correct_answer.rq";

    public FakeQuestionAnsweringSystem(
            @Autowired TripleStoreConnector tripleStoreConnector,
            @Value("${qado.question.knowledgebase}") String knowledgebase,
            @Value("${qado.question.user}") String user
    ) {
        this.tripleStoreConnector = tripleStoreConnector;
        this.knowledgebase = knowledgebase;
        this.user = user;

        TripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_GET_ALL_DATASETS);
        TripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_GET_DATASET_BY_NAME);
        TripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_GET_QUESTION_BY_QUESTIONTEXT);
        TripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_GET_SPARQL_QUERY_FROM_QUESTION_BY_RESOURCE);
        TripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_GET_SPARQL_QUERY_FROM_DATASET_WITHOUT_CORRECT_ANSWER);
    }

    public JsonObject process(
            String question,
            String language,
            int numberOfResultsItems,
            String dataset

    ) throws DatasetNotExist, IOException, SparqlQueryFailed, QuestionNotExist {
        Resource datasetResource = this.getDatasetByName(dataset);
        Resource questionResource = this.getQuestionByQuestionText(datasetResource, question, language);

        return this.createAnswer(question, language, datasetResource, questionResource, numberOfResultsItems);
    }

    public JsonObject createAnswer(
            String question,
            String language,
            Resource datasetResource,
            Resource questionResource,
            int numberOfResultsItems
    ) throws QuestionNotExist, IOException, SparqlQueryFailed {
        // create list of languages
        ArrayList<String> languages = new ArrayList<>();
        languages.add(language);

        // create list of knowledgebases
        ArrayList<String> knowledgebases = new ArrayList<>();
        knowledgebases.add(this.knowledgebase);

        // create list of queries
        ArrayList<FakeAnswerQuery> queries = new ArrayList<>();

        // add correct answer
        queries.add(this.createCorrectAnswerQuery(questionResource));

        // add incorrect answers if necessary
        if (numberOfResultsItems > 1) {
            List<FakeAnswerQuery> incorrectAnswers = this.createIncorrectAnswers(datasetResource, questionResource, numberOfResultsItems - 1);
            queries.addAll(incorrectAnswers);
        }

        // shuffle the queries
        Collections.shuffle(queries);

        // create the fake answer
        FakeAnswer fakeAnswer = new FakeAnswer(
                question,
                languages,
                knowledgebases,
                queries
        );

        return fakeAnswer.toJsonObject();
    }

    public FakeAnswerQuery createCorrectAnswerQuery(
            Resource questionResource
    ) throws QuestionNotExist, IOException, SparqlQueryFailed {
        String sparql = this.getSparqlQueryFromQuestionByResource(questionResource);
        float confidence = this.getRandomFloat(0.5f, 1.0f);

        return new FakeAnswerQuery(
                sparql,
                true,
                confidence,
                this.knowledgebase,
                this.user
        );
    }

    public List<FakeAnswerQuery> createIncorrectAnswers(
            Resource datasetResource,
            Resource questionResource,
            int numberOfResultsItems
    ) throws IOException, SparqlQueryFailed {
        ArrayList<FakeAnswerQuery> fakeAnswerQueries = new ArrayList<>();
        List<String> wrongAnswers = this.getSparqlQueryFromDatasetWithoutCorrectAnswer(datasetResource, questionResource, numberOfResultsItems);

        for(String wrongAnswerSparql : wrongAnswers) {
            float confidence = this.getRandomFloat(0.0f, 0.5f);

            fakeAnswerQueries.add(new FakeAnswerQuery(
                    wrongAnswerSparql,
                    false,
                    confidence,
                    this.knowledgebase,
                    this.user
            ));
        }

        return fakeAnswerQueries;
    }

    /**
     * Returns a random float between min and max, with two decimal places
     * @param min lower bound
     * @param max upper bound
     * @return float with two decimal places
     */
    public float getRandomFloat(float min, float max) {
        DecimalFormat df = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));

        return Float.parseFloat(df.format(min + r.nextFloat() * (max - min)));
    }

    public Resource getDatasetByName(String datasetName) throws IOException, SparqlQueryFailed, DatasetNotExist {
        QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
        bindingsForInsert.add("datasetName", ResourceFactory.createStringLiteral(datasetName));
        String sparql = TripleStoreConnector.readFileFromResourcesWithMap(FILENAME_GET_DATASET_BY_NAME, bindingsForInsert);
        ResultSet resultset = this.tripleStoreConnector.select(sparql);

        if (!resultset.hasNext()) {
            LOGGER.error("Dataset {} does not exist", datasetName);
            throw new DatasetNotExist("Dataset " + datasetName + " does not exist", getAllDatasets());
        }

        QuerySolution tupel = resultset.next();
        String item = tupel.get("item").toString();

        return ResourceFactory.createResource(item);
    }

    public List<String> getAllDatasets() throws IOException, SparqlQueryFailed {
        String sparqlSelectQuery = TripleStoreConnector.readFileFromResources(FILENAME_GET_ALL_DATASETS);
        ResultSet resultset = this.tripleStoreConnector.select(sparqlSelectQuery);

        List<String> datasets = new ArrayList<>();

        while (resultset.hasNext()) {
            QuerySolution tupel = resultset.next();

            String label = tupel.get("label").toString();
            datasets.add(label);
        }

        return datasets;
    }

    public Resource getQuestionByQuestionText(
            Resource dataset,
            String questionText,
            String language
    ) throws IOException, SparqlQueryFailed, QuestionNotExist {
        QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
        bindingsForInsert.add("dataset", dataset);
        bindingsForInsert.add("questionText", ResourceFactory.createLangLiteral(questionText, language));
        String sparql = TripleStoreConnector.readFileFromResourcesWithMap(FILENAME_GET_QUESTION_BY_QUESTIONTEXT, bindingsForInsert);
        ResultSet resultset = this.tripleStoreConnector.select(sparql);

        if (!resultset.hasNext()) {
            LOGGER.error("Question \"{}\" does not exist", questionText);
            throw new QuestionNotExist("Question \"" + questionText + "\" does not exist");
        }

        QuerySolution tupel = resultset.next();
        String item = tupel.get("item").toString();

        return ResourceFactory.createResource(item);
    }

    public String getSparqlQueryFromQuestionByResource(
            Resource questionResource
    ) throws IOException, SparqlQueryFailed, QuestionNotExist {
        QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
        bindingsForInsert.add("questionResource", questionResource);
        String sparql = TripleStoreConnector.readFileFromResourcesWithMap(FILENAME_GET_SPARQL_QUERY_FROM_QUESTION_BY_RESOURCE, bindingsForInsert);

        ResultSet resultset = this.tripleStoreConnector.select(sparql);

        if (!resultset.hasNext()) {
            LOGGER.error("Question resource \"{}\" has no SPARQL query", questionResource);
            throw new QuestionNotExist("Question resource \"" + questionResource + "\" has no SPARQL query");
        }

        QuerySolution tupel = resultset.next();

        return tupel.get("sparqlQuery").toString();
    }

    public List<String> getSparqlQueryFromDatasetWithoutCorrectAnswer(
            Resource datasetResource,
            Resource correctItemResource,
            int limit
    ) throws IOException, SparqlQueryFailed {
        QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
        bindingsForInsert.add("datasetItem", datasetResource);
        bindingsForInsert.add("correctItem", correctItemResource);
        bindingsForInsert.add("limit", ResourceFactory.createTypedLiteral(String.valueOf(limit), XSDDatatype.XSDinteger));
        String sparql = TripleStoreConnector.readFileFromResourcesWithMap(FILENAME_GET_SPARQL_QUERY_FROM_DATASET_WITHOUT_CORRECT_ANSWER, bindingsForInsert);

        ResultSet resultset = this.tripleStoreConnector.select(sparql);

        // TODO if empty
//        if (!resultset.hasNext()) {
//            LOGGER.error("Question \"{}\" does not exist", questionResource);
//            throw new QuestionNotExist("Question \"" + questionResource + "\" does not exist");
//        }

        List<String> wrongAnswers = new ArrayList<>();

        while (resultset.hasNext()) {
            QuerySolution tupel = resultset.next();

            String sparqlQuery = tupel.get("sparqlQuery").toString();
            wrongAnswers.add(sparqlQuery);
        }

        return wrongAnswers;
    }
}
