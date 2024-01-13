package eu.wseresearch.memorizedquestionansweringsystem.triplestoreconnector;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import eu.wseresearch.memorizedquestionansweringsystem.triplestoreconnector.exception.SparqlQueryFailed;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;
import virtuoso.jena.driver.VirtuosoUpdateFactory;
import virtuoso.jena.driver.VirtuosoUpdateRequest;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import eu.wseresearch.memorizedquestionansweringsystem.config.CacheConfig;


@ConditionalOnProperty(name = {"virtuoso.url", "virtuoso.graph", "virtuoso.username", "virtuoso.password"}, matchIfMissing = false)
@Component
public class TripleStoreConnectorVirtuoso extends TripleStoreConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(TripleStoreConnectorVirtuoso.class);
    private static final CharSequence VIRTUOSO_PROBLEM_STRING = "Read timed out";

    private final String virtuosoUrl;
    private final String virtuosoGraph;
    private final String username;
    private final String password;
    private final int queryTimeout;
    private final short maxTriesConnectionTimeout = 3;
    private CacheManager cacheManager;
    private VirtGraph connection;

    public TripleStoreConnectorVirtuoso(
            @Value("${virtuoso.url}") String virtuosoUrl, //
            @Value("${virtuoso.graph}") String virtuosoGraph, //
            @Value("${virtuoso.username}") String username, //
            @Value("${virtuoso.password}") String password, //
            @Value("${virtuoso.query.timeout:10}") int queryTimeout, //
            @Autowired CacheManager cacheManager //
    ) {
        LOGGER.debug("initialize Virtuoso triplestore connector as {} to {}:{}", username, virtuosoUrl, virtuosoGraph);
        this.virtuosoUrl = virtuosoUrl;
        this.virtuosoGraph = virtuosoGraph;
        this.username = username;
        this.password = password;
        this.queryTimeout = queryTimeout;
        this.cacheManager = cacheManager;
        this.connect();
    }

    public String getVirtuosoUrl() {
        return this.virtuosoUrl;
    }

    public String getVirtuosoGraph() {
        return this.virtuosoGraph;
    }

    private String getUsername() {
        return this.username;
    }

    private String getPassword() {
        return this.password;
    }

    private int getTimeout() {
        return this.queryTimeout;
    }

    @Override
    public void connect() {
        LOGGER.debug("Virtuoso server connecting to {}", this.getVirtuosoUrl());
        assert this.virtuosoUrl != null && !"".equals(this.virtuosoUrl);
        assert this.username != null && !"".equals(this.username);
        assert this.password != null && !"".equals(this.password);
        assert this.getTimeout() > 0;
        this.initConnection();
        assert connection != null;
    }

    private void initConnection() {
        if (this.connection != null) {
            LOGGER.warn("Virtuoso server trying to re-connected at {}", this.getVirtuosoUrl());
        } else {
            LOGGER.debug("Virtuoso server trying to connected at {}", this.getVirtuosoUrl());
        }

        int numberOfReconnectingTries = 0;
        while (this.maxTriesConnectionTimeout > numberOfReconnectingTries) {
            try {
                connection = new VirtGraph(this.getVirtuosoGraph(), this.getVirtuosoUrl(), this.getUsername(), this.getPassword());
                connection.setQueryTimeout(getTimeout());
                LOGGER.info("Virtuoso server connected at {}", this.getVirtuosoUrl());
                return;
            } catch (Exception e) {
                LOGGER.warn("Tried to establish connection ({}), but failed: {}", numberOfReconnectingTries, e.getMessage());
                if (LOGGER.isDebugEnabled()) {
                    e.printStackTrace();
                }
                numberOfReconnectingTries++;

                if (this.maxTriesConnectionTimeout <= numberOfReconnectingTries) {
                    LOGGER.error("Failed to establish connection. Max tries exceeded!");
                    throw new RuntimeException(e);
                }

                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (Exception e2) {
                    LOGGER.warn("Failed to wait for 5 seconds: {}", e2.getMessage());
                    if (LOGGER.isDebugEnabled()) {
                        e2.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void update(String sparql, URI graph) throws SparqlQueryFailed {
        short numberOfTries = 0;
        // try N times if there was a timeout
        while (numberOfTries < this.maxTriesConnectionTimeout || numberOfTries == 0) {
            try {
                this.update(sparql, graph, numberOfTries);
                return;
            } catch (Exception e) {
                LOGGER.error("Error while executing a UPDATE query: {}", e.getMessage());
                if(LOGGER.isDebugEnabled()) {
                    e.printStackTrace();
                }

                if (e.getMessage().contains(VIRTUOSO_PROBLEM_STRING)) { // not nice
                    LOGGER.error("Connection was a timeout. Possible retry ({} tries already).", numberOfTries);
                    this.initConnection();
                    numberOfTries++;
                } else {
                    LOGGER.error("Error happened. Returns SparqlQueryFailed exception.");
                    throw new SparqlQueryFailed(sparql, this.virtuosoUrl, e);
                }
            }
        }
    }

    private void update(String sparql, URI graph, short numberOfTries) throws SparqlQueryFailed {
        long start = getTime();
        LOGGER.info("execute UPDATE query on graph {} (try: {}): {}", graph, numberOfTries, sparql);
        VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(sparql, this.connection);
        vur.exec();
        this.logTime(getTime() - start, "UPDATE on " + this.getVirtuosoUrl() + ": " + sparql);
    }

    @Override
    public void update(String sparql) throws SparqlQueryFailed {
        this.update(sparql, null);
    }

    @Override
    public boolean ask(String sparql) throws SparqlQueryFailed {
        short numberOfTries = 0;
        // try N times if there was a timeout
        while (numberOfTries < this.maxTriesConnectionTimeout || numberOfTries == 0) {
            try {
                return this.ask(sparql, numberOfTries);
            } catch (Exception e) {
                LOGGER.error("Error while executing a ASK query: {}", e.getMessage());
                if(LOGGER.isDebugEnabled()) {
                    e.printStackTrace();
                }

                if (e.getMessage().contains(VIRTUOSO_PROBLEM_STRING)) { // not nice
                    LOGGER.error("Connection was a timeout. Possible retry ({} tries already).", numberOfTries);
                    this.initConnection();
                    numberOfTries++;
                } else {
                    LOGGER.error("Error happened. Returns SparqlQueryFailed exception.");
                    throw new SparqlQueryFailed(sparql, this.virtuosoUrl, e);
                }
            }
        }
        return false; // should never happen
    }

    private boolean ask(String sparql, short numberOfTries) throws SparqlQueryFailed {
        long start = getTime();
        LOGGER.info("execute ASK query (try: {}): {}", numberOfTries, sparql);
        VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(QueryFactory.create(sparql), this.connection);
        boolean result = vqe.execAsk();
        this.logTime(getTime() - start, "ASK on " + this.getVirtuosoUrl() + ": " + sparql);
        return result;
    }

    @Override
    public ResultSet select(String sparql) throws SparqlQueryFailed {
        return this.select(sparql, false);
    }

    @Override
    public ResultSet select(String sparql, boolean noCache) throws SparqlQueryFailed {
        try {
            if (noCache || this.cacheManager == null || this.cacheManager.getCache(CacheConfig.CACHENAME) == null) {
                return execSelect(sparql);
            }

            return execSelectWithCache(sparql, this.cacheManager.getCache(CacheConfig.CACHENAME));
        } catch (Exception e) {
            throw new SparqlQueryFailed(sparql, this.virtuosoUrl.toString() + getVirtuosoGraph(), e);
        }
    }

    private ResultSet execSelect(String sparql) throws SparqlQueryFailed {
        short numberOfTries = 0;
        LOGGER.info("no Cache is used");
        // try N times if there was a timeout
        while (numberOfTries < this.maxTriesConnectionTimeout || numberOfTries == 0) {
            Query query = QueryFactory.create(sparql);

            try (VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(query, this.connection)) {
                long start = getTime();
                LOGGER.info("execute SELECT query (try: {}): {}", numberOfTries, sparql);
                ResultSetRewindable resultsRewindable = ResultSetFactory.makeRewindable(vqe.execSelect());
                this.logTime(getTime() - start, "SELECT on " + this.getVirtuosoUrl() + " resulted in " + resultsRewindable.size() + " rows: " + sparql);

                return resultsRewindable;

            } catch (Exception e) {
                LOGGER.error("Error while executing a SELECT query: {}", e.getMessage());
                if(LOGGER.isDebugEnabled()) {
                    e.printStackTrace();
                }

                if (e.getMessage().contains(VIRTUOSO_PROBLEM_STRING)) { // not nice
                    LOGGER.error("Connection was a timeout. Possible retry ({} tries already).", numberOfTries);
                    this.initConnection();
                    numberOfTries++;
                } else {
                    LOGGER.error("Error happened. Returns SparqlQueryFailed exception.");
                    throw new SparqlQueryFailed(sparql, this.virtuosoUrl, e);
                }
            }
        }

        return null; // should never happen
    }

    private ResultSet execSelectWithCache(String sparql, Cache cache) throws SparqlQueryFailed {
        int hashCode = Objects.hash(sparql.hashCode());

        if (cache.get(hashCode) != null) {
            LOGGER.info("Cache hit for HashCode: {}", hashCode);

            ResultSetRewindable rsrw = cache.get(hashCode, ResultSetRewindable.class);

            if (rsrw != null) {
                rsrw.reset();

                return rsrw;
            } else {
                LOGGER.warn("Cache hit for HashCode: {} but ResultSetRewindable is null", hashCode);
            }
        }

        LOGGER.info("Cache miss for HashCode: {}", hashCode);

        ResultSet rs = this.execSelect(sparql);
        ResultSetRewindable rsrw = ResultSetFactory.makeRewindable(rs);
        cache.put(hashCode, rsrw);

        return rsrw;
    }

}
