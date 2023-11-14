package eu.wseresearch.fakequestionansweringsystem.triplestoreconnector;

import com.complexible.stardog.api.*;
import com.complexible.stardog.jena.SDJenaFactory;
import eu.wseresearch.fakequestionansweringsystem.config.CacheConfig;
import eu.wseresearch.fakequestionansweringsystem.triplestoreconnector.exception.SparqlQueryFailed;
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
    private final boolean reasoningType = false;
    private final int minPool = 0;
    private final int maxPool = 1000;
    private final int expirationTime = 60;
    private final int blockCapacityTime = 5;

    private CacheManager cacheManager;
    private ConnectionPool connectionPool;

    public TripleStoreConnectorStardog(
            @Value("${stardog.url}") URI url,//
            @Value("${stardog.database}") String database, //
            @Value("${stardog.username}") String username, //
            @Value("${stardog.password}") String password, //
            @Autowired CacheManager cacheManager //
    ) {
        this.url = url;
        this.database = database;
        this.username = username;
        this.password = password;
        this.cacheManager = cacheManager;

        LOGGER.debug("SPARQL Connection initialized: url:{}, username:{}, password:{}, database:{}", url, username, password, database);
        this.connect();
        LOGGER.info("SPARQL Connection created on endpoint {}{}", url, database);
    }

    private ConnectionPool createConnectionPool(ConnectionConfiguration connectionConfig) {
        TimeUnit expirationTimeUnit = TimeUnit.SECONDS;
        TimeUnit blockCapacityTimeUnit = TimeUnit.SECONDS;

        // c.f.,
        // https://docs.stardog.com/archive/7.5.0/developing/programming-with-stardog/java#using-sesame
        ConnectionPoolConfig poolConfig = ConnectionPoolConfig.using(connectionConfig) //
                .minPool(this.minPool).maxPool(this.maxPool) // for some reason it causes errors while using some specific values
                .expiration(this.expirationTime, expirationTimeUnit) //
                .blockAtCapacity(this.blockCapacityTime, blockCapacityTimeUnit); //
        return poolConfig.create();
    }

    @Override
    public void connect() {
        ConnectionConfiguration connectionConfig = ConnectionConfiguration.to(this.database)
                .server(this.url.toASCIIString()).reasoning(this.reasoningType)
                .credentials(this.username, this.password);
        this.connectionPool = createConnectionPool(connectionConfig); // creates the Stardog connection pool
    }

    @Override
    public ResultSet select(String sparql) throws SparqlQueryFailed {
        try {
            if (this.cacheManager == null || this.cacheManager.getCache(CacheConfig.CACHENAME) == null) {
                return execSelect(sparql);
            }

            return execSelectWithCache(sparql, this.cacheManager.getCache(CacheConfig.CACHENAME));
        } catch (Exception e) {
            throw new SparqlQueryFailed(sparql, this.url.toString() + this.database, e);
        }
    }

    @Override
    public void update(String sparql) throws SparqlQueryFailed {
        Connection connection = this.connectionPool.obtain();

        try {
            UpdateQuery query = connection.update(sparql);
            query.execute();
        } catch (Exception e) {
            throw new SparqlQueryFailed(sparql, this.url.toASCIIString(), e);
        }


        connection.close();
    }

    private ResultSet execSelect(String sparql) throws SparqlQueryFailed {
        LOGGER.info("no Cache is used");

        Connection connection = this.connectionPool.obtain();
        Model aModel = SDJenaFactory.createModel(connection);
        Query aQuery = QueryFactory.create(sparql);

        try (QueryExecution aExec = QueryExecutionFactory.create(aQuery, aModel)) {
            ResultSet rs = aExec.execSelect();
            connection.close();

            return ResultSetFactory.makeRewindable(rs);
        } catch (Exception e) {
            throw new SparqlQueryFailed(sparql, url.toASCIIString(), e);
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
