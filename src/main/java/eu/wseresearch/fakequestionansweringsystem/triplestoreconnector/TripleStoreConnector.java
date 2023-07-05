package eu.wseresearch.fakequestionansweringsystem.triplestoreconnector;

import eu.wseresearch.fakequestionansweringsystem.triplestoreconnector.exception.SparqlQueryFailed;
import org.apache.jena.query.*;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public abstract class TripleStoreConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(TripleStoreConnector.class);

    public abstract void connect();

    public abstract ResultSet select(String sparql) throws SparqlQueryFailed;

    public abstract void update(String sparql) throws SparqlQueryFailed;

    /**
     * read SPARQL query from files in resources folder
     *
     * @param filenameWithRelativePath
     * @return
     * @throws IOException
     */
    public static String readFileFromResources(String filenameWithRelativePath) throws IOException {
        InputStream in = TripleStoreConnector.class.getResourceAsStream(filenameWithRelativePath);

        // TODO if in is null, throw exception

        LOGGER.debug("filenameWithRelativePath: {}, {}", filenameWithRelativePath, in);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        return reader.lines().collect(Collectors.joining("\n"));
    }

    /**
     * @param filenameWithRelativePath
     * @param bindings
     * @return
     * @throws IOException
     */
    public static String readFileFromResourcesWithMap(String filenameWithRelativePath, QuerySolutionMap bindings) throws IOException {
        String sparqlQueryString = readFileFromResources(filenameWithRelativePath);
        ParameterizedSparqlString pq = new ParameterizedSparqlString(sparqlQueryString, bindings);
        LOGGER.debug("readFileFromResourcesWithMap sparqlQueryString: {}", sparqlQueryString);
        Query query;
        if (!sparqlQueryString.contains("\nSELECT ") && !sparqlQueryString.startsWith("SELECT")) {
            if (!sparqlQueryString.contains("\nASK ") && !sparqlQueryString.startsWith("ASK")) {
                UpdateRequest updateQuery = UpdateFactory.create(pq.toString());
                LOGGER.info("generated UPDATE query:\n{}", updateQuery.toString());

                return updateQuery.toString();

            } else {
                query = QueryFactory.create(pq.toString());
                LOGGER.info("generated ASK query:\n{}", query.toString());

                return query.toString();
            }

        } else {
            query = QueryFactory.create(pq.toString());
            LOGGER.info("generated SELECT query:\n{}", query.toString());

            return query.toString();
        }
    }

    /**
     * @param filenameInResources
     * @throws RuntimeException if file does not exist or is empty
     */
    public static void guardNonEmptyFileFromResources(String filenameInResources) {
        String message = null;

        try {
            String readFileContent = readFileFromResources(filenameInResources);
            if (readFileContent == null) {
                message = "file content was null (does the file exist?): " + filenameInResources;
            } else {
                if (!readFileContent.isBlank()) {
                    return;
                }

                message = "no content: " + filenameInResources;
            }

            LOGGER.error(message);
            throw new RuntimeException(message);
        } catch (IOException var3) {
            message = "not available: " + filenameInResources;
            LOGGER.error(message);
            throw new RuntimeException(message);
        }
    }
}
