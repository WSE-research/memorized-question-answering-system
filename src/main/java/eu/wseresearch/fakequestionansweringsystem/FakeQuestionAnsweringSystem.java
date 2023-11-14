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
    private final String knowledgebase = "";
    private final String user;
    private final int maxRows;

    private String FILENAME_GET_ALL_DATASETS = "/queries/select_dataset_labels.rq";
    private String FILENAME_GET_DATASET_BY_NAME = "/queries/select_dataset_by_name.rq";
    private String FILENAME_GET_QUESTION_BY_QUESTIONTEXT = "/queries/select_question_by_questiontext.rq";
    private String FILENAME_GET_QUESTION_BY_QUESTIONTEXT_WITHOUT_DATASET = "/queries/select_question_by_questiontext_without_dataset.rq";
    private String FILENAME_GET_SPARQL_QUERY_FROM_QUESTION_BY_RESOURCE = "/queries/select_sparql_query_from_question_by_resource.rq";
    private String FILENAME_GET_SPARQL_QUERY_FROM_DATASET_WITHOUT_CORRECT_ANSWER = "/queries/select_sparql_query_from_dataset_without_correct_answer.rq";
    private String FILENAME_GET_SPARQL_QUERY_WITHOUT_CORRECT_ANSWER = "/queries/select_sparql_query_without_correct_answer.rq";
    private String FILENAME_GET_SPARQL_COUNT_ANSWER = "/queries/select_sparql_count_answer.rq";

    public FakeQuestionAnsweringSystem(
            @Autowired TripleStoreConnector tripleStoreConnector,
            @Value("${qado.question.user}") String user,
            @Value("${qado.resultset.maxrows}") String resultSetMaxRows
    ) {
        this.tripleStoreConnector = tripleStoreConnector;
        this.user = user;
        this.maxRows = Integer.parseInt(resultSetMaxRows);

        TripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_GET_ALL_DATASETS);
        TripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_GET_DATASET_BY_NAME);
        TripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_GET_QUESTION_BY_QUESTIONTEXT);
        TripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_GET_QUESTION_BY_QUESTIONTEXT_WITHOUT_DATASET);
        TripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_GET_SPARQL_QUERY_FROM_QUESTION_BY_RESOURCE);
        TripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_GET_SPARQL_QUERY_FROM_DATASET_WITHOUT_CORRECT_ANSWER);
        TripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_GET_SPARQL_QUERY_WITHOUT_CORRECT_ANSWER);
    }

    public JsonObject createAnswer(
            String question,
            String language,
            Integer numberOfResultsItems,
            String dataset
    ) throws QuestionNotExist, IOException, SparqlQueryFailed, DatasetNotExist {
        // create list of languages
        ArrayList<String> languages = new ArrayList<>();
        languages.add(language);

        // create list of knowledgebases
        ArrayList<String> knowledgebases = new ArrayList<>();
//        knowledgebases.add(this.knowledgebase);

        // create list of queries
        List<FakeAnswerQuery> queries = createAnswerQueries(question, language, numberOfResultsItems, dataset);

        // create the fake answer
        FakeAnswer fakeAnswer = new FakeAnswer(
                question,
                languages,
                knowledgebases,
                queries
        );

        return fakeAnswer.toJsonObject();
    }

    public List<FakeAnswerQuery> createAnswerQueries(
            String question,
            String language,
            Integer numberOfResultsItems,
            String dataset
    ) throws DatasetNotExist, IOException, SparqlQueryFailed, QuestionNotExist {
        ArrayList<FakeAnswerQuery> queries = new ArrayList<>();

        Resource datasetResource = null;
        Resource questionResource;

        if (dataset != null) {
            datasetResource = this.getDatasetByName(dataset);
            questionResource = this.getQuestionByQuestionText(datasetResource, question, language);
        } else {
            questionResource = this.getQuestionByQuestionText(question, language);
        }

        // add correct answer
        queries.add(this.createCorrectAnswerQuery(questionResource));

        // add incorrect answers if necessary
        List<FakeAnswerQuery> incorrectAnswers = this.createIncorrectAnswers(datasetResource, question, language, numberOfResultsItems);
        for (FakeAnswerQuery incorrectAnswer : incorrectAnswers) {
            queries.add(incorrectAnswer);
        }

        // shuffle the queries
        Collections.shuffle(queries);

        return queries;
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
            String questionText,
            String language,
            Integer numberOfResultsItems
    ) throws IOException, SparqlQueryFailed {
        ArrayList<FakeAnswerQuery> fakeAnswerQueries = new ArrayList<>();
        List<String> wrongAnswers;
        int limit = 0;

        // set limit for results
        if (numberOfResultsItems == null || numberOfResultsItems == 0) {
            int allRows = this.getMaxNumberOfItems(questionText, language);

            limit = Math.min(allRows, this.maxRows);
        } else {
            limit = numberOfResultsItems - 1;
        }

        // get answers
        if (datasetResource == null) {
            wrongAnswers = this.getSparqlQueryFromDatasetWithoutCorrectAnswer(questionText, language, limit);
        } else {
            wrongAnswers = this.getSparqlQueryFromDatasetWithoutCorrectAnswer(datasetResource, questionText, language, limit);
        }

        // create fake answers
        for (String wrongAnswerSparql : wrongAnswers) {
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
     *
     * @param min lower bound
     * @param max upper bound
     * @return float with two decimal places
     */
    public float getRandomFloat(float min, float max) {
        DecimalFormat df = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));

        return Float.parseFloat(df.format(min + r.nextFloat() * (max - min)));
    }

    public Resource getDatasetByName(String datasetName) throws IOException, SparqlQueryFailed, DatasetNotExist {
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("datasetName", ResourceFactory.createStringLiteral(datasetName));
        String sparql = TripleStoreConnector.readFileFromResourcesWithMap(FILENAME_GET_DATASET_BY_NAME, bindings);
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
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("dataset", dataset);
        bindings.add("questionText", ResourceFactory.createLangLiteral(questionText, language));
        String sparql = TripleStoreConnector.readFileFromResourcesWithMap(FILENAME_GET_QUESTION_BY_QUESTIONTEXT, bindings);
        ResultSet resultset = this.tripleStoreConnector.select(sparql);

        if (!resultset.hasNext()) {
            LOGGER.error("Question \"{}\" does not exist", questionText);
            throw new QuestionNotExist("Question \"" + questionText + "\" does not exist");
        }

        QuerySolution tupel = resultset.next();
        String item = tupel.get("item").toString();

        return ResourceFactory.createResource(item);
    }

    public Resource getQuestionByQuestionText(
            String questionText,
            String language
    ) throws IOException, SparqlQueryFailed, QuestionNotExist {
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("questionText", ResourceFactory.createLangLiteral(questionText, language));
        String sparql = TripleStoreConnector.readFileFromResourcesWithMap(FILENAME_GET_QUESTION_BY_QUESTIONTEXT_WITHOUT_DATASET, bindings);
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
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("questionResource", questionResource);
        String sparql = TripleStoreConnector.readFileFromResourcesWithMap(FILENAME_GET_SPARQL_QUERY_FROM_QUESTION_BY_RESOURCE, bindings);

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
            String questionText,
            String language,
            int limit
    ) throws IOException, SparqlQueryFailed {
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("datasetItem", datasetResource);
        bindings.add("limit", ResourceFactory.createTypedLiteral(String.valueOf(limit), XSDDatatype.XSDinteger));
        bindings.add("questionText", ResourceFactory.createLangLiteral(questionText, language));
        String sparql = TripleStoreConnector.readFileFromResourcesWithMap(FILENAME_GET_SPARQL_QUERY_FROM_DATASET_WITHOUT_CORRECT_ANSWER, bindings);

        ResultSet resultset = this.tripleStoreConnector.select(sparql);

        List<String> wrongAnswers = new ArrayList<>();

        while (resultset.hasNext()) {
            QuerySolution tupel = resultset.next();

            String sparqlQuery = tupel.get("sparqlQuery").toString();
            wrongAnswers.add(sparqlQuery);
        }

        return wrongAnswers;
    }

    public List<String> getSparqlQueryFromDatasetWithoutCorrectAnswer(
            String questionText,
            String language,
            int limit
    ) throws IOException, SparqlQueryFailed {
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("limit", ResourceFactory.createTypedLiteral(String.valueOf(limit), XSDDatatype.XSDinteger));
        bindings.add("questionText", ResourceFactory.createLangLiteral(questionText, language));
        String sparql = TripleStoreConnector.readFileFromResourcesWithMap(FILENAME_GET_SPARQL_QUERY_WITHOUT_CORRECT_ANSWER, bindings);

        ResultSet resultset = this.tripleStoreConnector.select(sparql);

        List<String> wrongAnswers = new ArrayList<>();

        while (resultset.hasNext()) {
            QuerySolution tupel = resultset.next();

            String sparqlQuery = tupel.get("sparqlQuery").toString();
            wrongAnswers.add(sparqlQuery);
        }

        return wrongAnswers;
    }

    public int getMaxNumberOfItems(
            String questionText,
            String language
    ) throws IOException, SparqlQueryFailed {
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("questionText", ResourceFactory.createLangLiteral(questionText, language));
        String sparql = TripleStoreConnector.readFileFromResourcesWithMap(FILENAME_GET_SPARQL_COUNT_ANSWER, bindings);

        ResultSet resultset = this.tripleStoreConnector.select(sparql);

        QuerySolution tupel = resultset.next();
        return tupel.get("count").asLiteral().getInt();
    }
}
