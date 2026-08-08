package eu.wseresearch.memorizedquestionansweringsystem.triplestoreconnector;

import eu.wseresearch.memorizedquestionansweringsystem.triplestoreconnector.exception.SparqlQueryFailed;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The component used to connect inside the constructor and let the failure escape, which
 * took the whole Spring context down:
 *
 * <pre>
 * Error creating bean with name 'memQASystem' ... nested exception is ...
 * Failed to instantiate [TripleStoreConnectorVirtuoso]: Constructor threw exception
 * ... Caused by: virtuoso.jdbc4.VirtuosoException: Connection failed: [] Connection refused
 * </pre>
 *
 * A triplestore that is temporarily unavailable therefore turned into a container restart
 * loop that only ended when somebody noticed. A missing configuration on the other hand
 * will never fix itself and still has to stop the application immediately.
 */
class TripleStoreConnectorVirtuosoTest {

    /** nothing listens here, so connecting always fails */
    private static final String UNREACHABLE = "jdbc:virtuoso://127.0.0.1:1";

    private TripleStoreConnectorVirtuoso unreachableConnector() {
        return new TripleStoreConnectorVirtuoso(UNREACHABLE, "urn:test:graph", "user", "password", 10, null);
    }

    @Test
    void startsWhileTheTriplestoreIsUnreachableAndFailsOnlyTheQuery() {
        TripleStoreConnectorVirtuoso connector = assertDoesNotThrow(
                this::unreachableConnector,
                "an unreachable triplestore must not prevent the component from starting"
        );
        assertNotNull(connector);

        // the failure has to surface per request instead of taking the application down
        SparqlQueryFailed failure = assertThrows(
                SparqlQueryFailed.class,
                () -> connector.select("SELECT * WHERE { ?s ?p ?o } LIMIT 1"),
                "a query without a connection must fail with SparqlQueryFailed"
        );
        assertTrue(failure.getTriplestore().contains(UNREACHABLE), failure.getTriplestore());
    }

    @Test
    void missingUrlStopsTheApplication() {
        assertThrows(
                IllegalStateException.class,
                () -> new TripleStoreConnectorVirtuoso("", "urn:test:graph", "user", "password", 10, null)
        );
    }

    @Test
    void missingCredentialsStopTheApplication() {
        assertThrows(
                IllegalStateException.class,
                () -> new TripleStoreConnectorVirtuoso(UNREACHABLE, "urn:test:graph", null, "password", 10, null)
        );
        assertThrows(
                IllegalStateException.class,
                () -> new TripleStoreConnectorVirtuoso(UNREACHABLE, "urn:test:graph", "user", " ", 10, null)
        );
    }

    @Test
    void nonPositiveQueryTimeoutStopsTheApplication() {
        assertThrows(
                IllegalStateException.class,
                () -> new TripleStoreConnectorVirtuoso(UNREACHABLE, "urn:test:graph", "user", "password", 0, null)
        );
    }
}
