package eu.wseresearch.memorizedquestionansweringsystem.triplestoreconnector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.stream.Collectors;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.wseresearch.memorizedquestionansweringsystem.triplestoreconnector.exception.SparqlQueryFailed;

public abstract class TripleStoreConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(TripleStoreConnector.class);

    private static final long MAX_ACCEPTABLE_QUERY_EXECUTION_TIME = 10000;

    public abstract void connect();

    public abstract ResultSet select(String sparql) throws SparqlQueryFailed;

    public abstract ResultSet select(String sparql, boolean noCache) throws SparqlQueryFailed;

    public abstract boolean ask(String sparql) throws SparqlQueryFailed;

    public abstract void update(String sparql, URI graph) throws SparqlQueryFailed;

    public abstract void update(String sparql) throws SparqlQueryFailed;

    	/**
	 * get current time in milliseconds
	 */
	protected static long getTime() {
		return System.currentTimeMillis();
	}

    /**
     * @param description
     * @param duration
     */
    protected void logTime(long duration, String description) {
        if (duration > MAX_ACCEPTABLE_QUERY_EXECUTION_TIME) {
            LOGGER.warn("runtime measurement: {} ms for {} (was very long)", duration, description);
        } else {
            LOGGER.info("runtime measurement: {} ms for {}", duration, description);
        }
    }

    /**
     * @param duration
     * @param description
     */
    protected void logTime(long duration, String description, String endpoint) {
        LOGGER.info("runtime measurement: {} ms on {} for {}", duration, endpoint, description);
    }


    /**
	 * read SPARQL query from files in resources folder
	 *
	 * @param filenameWithRelativePath
	 * @return
	 * @throws IOException
	 */
	public static String readFileFromResources(String filenameWithRelativePath) throws IOException {
		InputStream in = TripleStoreConnector.class.getResourceAsStream(filenameWithRelativePath);
		LOGGER.debug("filenameWithRelativePath: {}, {}", filenameWithRelativePath, in);

		if (in == null) {
			return null;
		} else {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			return reader.lines().collect(Collectors.joining("\n"));
		}
	}

    /**
     * read query from file and apply bindings
     *
     * @param filenameWithRelativePath
     * @param bindings
     * @return
     * @throws IOException
     */
    public static String readFileFromResourcesWithMap(
            String filenameWithRelativePath,
            QuerySolutionMap bindings
    ) throws IOException {
        String sparqlQueryString = readFileFromResources(filenameWithRelativePath);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("readFileFromResourcesWithMap sparqlQueryString: {}", sparqlQueryString);
            String debugMessage = "Try to apply the variables to the SPARQL query template:";
            for (String varName : bindings.asMap().keySet()) {
                debugMessage += String.format("\n\t%s -> %s", varName, bindings.asMap().get(varName));
            }
            LOGGER.debug(debugMessage);
        }

        ParameterizedSparqlString pq = new ParameterizedSparqlString(sparqlQueryString, bindings);

        LOGGER.debug("create SPARQL query text before QueryFactory: {}", pq.toString());

        if ((sparqlQueryString).contains("\nSELECT ") || sparqlQueryString.startsWith("SELECT")) {
            Query query = QueryFactory.create(pq.toString());
            LOGGER.info("generated SELECT query:\n{}", query.toString());
            return query.toString();
        } else if (sparqlQueryString.contains("\nASK ") || sparqlQueryString.startsWith("ASK")) {
            Query query = QueryFactory.create(pq.toString());
            LOGGER.info("generated ASK query:\n{}", query.toString());
            return query.toString();
        } else {
            UpdateRequest query = UpdateFactory.create(pq.toString());
            LOGGER.info("generated UPDATE query:\n{}", query.toString());
            query.toString();
            return query.toString();
        }

    }

    /**
     * ensures that files exists in the resources and are non-empty
     * <p>
     * e.g., useful for component constructors to ensure that SPRARQL query template
     * files (*.rq) are valid
     *
     * @param filenameInResources
     */
    public static void guardNonEmptyFileFromResources(String filenameInResources) {
        String message = null;
        try {
            String readFileContent = readFileFromResources(filenameInResources);

            if (readFileContent == null) {
                message = "file content was null (does the file exist?): " + filenameInResources;
            } else if (readFileContent.isBlank()) {
                message = "no content: " + filenameInResources;
            } else {
                return; // ok
            }
            LOGGER.error(message);
            throw new RuntimeException(message);

        } catch (IOException e) {
            // should not happen as readFileContent should always be readable (as null)
            message = "not available: " + filenameInResources;
            LOGGER.error(message);
            throw new RuntimeException(message);
        }
    }

}
