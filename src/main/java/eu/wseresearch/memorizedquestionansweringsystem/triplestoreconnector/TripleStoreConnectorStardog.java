package eu.wseresearch.memorizedquestionansweringsystem.triplestoreconnector;

import com.complexible.stardog.api.*;
import com.complexible.stardog.jena.SDJenaFactory;
import eu.wseresearch.memorizedquestionansweringsystem.config.CacheConfig;
import eu.wseresearch.memorizedquestionansweringsystem.triplestoreconnector.exception.SparqlQueryFailed;
import org.apache.jena.query.*;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@ConditionalOnProperty(name = {"stardog.url", "stardog.database", "stardog.username", "stardog.password"}, matchIfMissing = false)
@Component
public class TripleStoreConnectorStardog extends TripleStoreConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(TripleStoreConnectorStardog.class);

    private final URI url;
    private final String database;
    private final String username;
    private final String password;
    private final boolean reasoningType;
    private final int minPool;
    private final int maxPool;
    private final int expirationTime;
    private final int blockCapacityTime;

    private CacheManager cacheManager;
    private ConnectionPool connectionPool;

    public TripleStoreConnectorStardog(
            @Value("${stardog.url}") URI url, //
            @Value("${stardog.username}") String username, //
            @Value("${stardog.password}") String password, //
            @Value("${stardog.database:qanary}") String database, //
            @Value("${stardog.reasoningType:false}") boolean reasoningType, //
            @Value("${stardog.minPool:0}") int minPool, // default from docs
            @Value("${stardog.maxPool:1000}") int maxPool, // default from docs
            @Value("${stardog.expirationTime:60}") int expirationTime, // expiration time in seconds
            @Value("${stardog.blockCapacityTime:5}") int blockCapacityTime // block wait time in seconds
    ) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.database = database;
        this.reasoningType = reasoningType;
        this.minPool = minPool;
        this.maxPool = maxPool;
        this.expirationTime = expirationTime;
        this.blockCapacityTime = blockCapacityTime;
        LOGGER.debug(
                "Stardog Connection initialized: url:{}, username:{}, password:{}, database:{}, reasoningType:{}, minPool:{}, maxPool:{}, expirationTime:{}s, blockCapacityTime:{}s",
                url, username, password, database, reasoningType, minPool, maxPool, expirationTime, blockCapacityTime
        );
        this.connect();
        LOGGER.info("Stardog Connection created on endpoint {}", url);
    }

    public URI getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDatabase() {
        return database;
    }

    public boolean isReasoningType() {
        return reasoningType;
    }

    public ConnectionPool getConnectionPool() {
        return connectionPool;
    }

    public void setConnectionPool(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    private ConnectionPool createConnectionPool(ConnectionConfiguration connectionConfig) {
        TimeUnit expirationTimeUnit = TimeUnit.SECONDS;
        TimeUnit blockCapacityTimeUnit = TimeUnit.SECONDS;

        // c.f.,
        // https://docs.stardog.com/archive/7.5.0/developing/programming-with-stardog/java#using-sesame
        ConnectionPoolConfig poolConfig = ConnectionPoolConfig.using(connectionConfig) //
                .minPool(minPool).maxPool(maxPool) // for some reason it causes errors while using some specific values
                .expiration(expirationTime, expirationTimeUnit) //
                .blockAtCapacity(blockCapacityTime, blockCapacityTimeUnit); //
        return poolConfig.create();
    }

    @Override
    public void connect() {
        ConnectionConfiguration connectionConfig = ConnectionConfiguration.to(this.getDatabase())
                .server(this.getUrl().toASCIIString()).reasoning(this.isReasoningType())
                .credentials(this.getUsername(), this.getPassword());
        this.setConnectionPool(createConnectionPool(connectionConfig)); // creates the Stardog connection pool
    }

    @Override
    public void update(String sparql, URI graph) {
        LOGGER.debug("execute update on {}: {}", graph, sparql);
        long start = getTime();
        Connection connection = this.getConnectionPool().obtain();
        UpdateQuery query;
        String graphReadable;
        if (graph != null) {
            graphReadable = graph.toASCIIString();
            query = connection.update(sparql, graphReadable);
        } else {
            graphReadable = null;
            query = connection.update(sparql);
        }
        query.execute();
        this.logTime(getTime() - start,
                "UPDATE on " + this.getUrl().toASCIIString() + " with Graph " + graphReadable + ": " + sparql);
        this.getConnectionPool().release(connection);
    }

    @Override
    public void update(String sparql) {
        this.update(sparql, null);
    }

    @Override
    public boolean ask(String sparql) throws SparqlQueryFailed {
        long start = getTime();

        Connection connection = this.getConnectionPool().obtain();
        Model aModel = SDJenaFactory.createModel(connection);
        Query aQuery = QueryFactory.create(sparql);

        try (QueryExecution aExec = QueryExecutionFactory.create(aQuery, aModel)) {
            boolean result = aExec.execAsk();
            this.logTime(getTime() - start, "ASK on " + this.getUrl().toASCIIString() + ": " + sparql);
            this.getConnectionPool().release(connection);
            return result;
        } catch (Exception e) {
            throw new SparqlQueryFailed(sparql, this.getUrl().toASCIIString(), e);
        }
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
            throw new SparqlQueryFailed(sparql, this.url.toString() + this.database, e);
        }
    }

    private ResultSet execSelect(String sparql) throws SparqlQueryFailed {
        long start = getTime();
        LOGGER.info("no Cache is used");

        Connection connection = this.getConnectionPool().obtain();
        Model aModel = SDJenaFactory.createModel(connection);
        Query aQuery = QueryFactory.create(sparql);

        try (QueryExecution aExec = QueryExecutionFactory.create(aQuery, aModel)) {
            ResultSetRewindable resultsRewindable = ResultSetFactory.makeRewindable(aExec.execSelect());
            this.logTime(getTime() - start, "SELECT on " + this.getUrl().toASCIIString() + ": " + sparql);
            this.getConnectionPool().release(connection);
            return resultsRewindable;
        } catch (Exception e) {
            throw new SparqlQueryFailed(sparql, this.getUrl().toASCIIString(), e);
        }
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
