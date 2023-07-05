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

@ConditionalOnProperty(name = {"qado.triplestore.url", "qado.triplestore.database", "qado.triplestore.username", "qado.triplestore.password"}, matchIfMissing = false)
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
            @Value("${qado.triplestore.url}") URI url,//
            @Value("${qado.triplestore.database}") String database, //
            @Value("${qado.triplestore.username}") String username, //
            @Value("${qado.triplestore.password}") String password, //
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
                .minPool(minPool).maxPool(maxPool) // for some reason it causes errors while using some specific values
                .expiration(expirationTime, expirationTimeUnit) //
                .blockAtCapacity(blockCapacityTime, blockCapacityTimeUnit); //
        return poolConfig.create();
    }

    @Override
    public void connect() {
        ConnectionConfiguration connectionConfig = ConnectionConfiguration.to(database)
                .server(url.toASCIIString()).reasoning(reasoningType)
                .credentials(username, password);
        this.connectionPool = createConnectionPool(connectionConfig); // creates the Stardog connection pool
    }

    @Override
    public ResultSet select(String sparql) throws SparqlQueryFailed {
        try {
            if (cacheManager == null) {
                return execSelect(sparql);
            }

            return execSelectWithCache(sparql);
        } catch (Exception e) {
            throw new SparqlQueryFailed(sparql, this.url.toString() + this.database, e);
        }
    }

    @Override
    public void update(String sparql) throws SparqlQueryFailed {
        try (
                Connection connection = connectionPool.obtain()
        ) {
            UpdateQuery query = connection.update(sparql);
            query.execute();
        } catch (Exception e) {
            throw new SparqlQueryFailed(sparql, this.url.toString() + this.database, e);
        }
    }

    private ResultSet execSelect(String sparql) throws SparqlQueryFailed {
        LOGGER.info("no Cache is used");

        Connection connection = connectionPool.obtain();
        Model aModel = SDJenaFactory.createModel(connection);
        Query aQuery = QueryFactory.create(sparql);

        try (QueryExecution aExec = QueryExecutionFactory.create(aQuery, aModel)) {
            ResultSet rs = aExec.execSelect();
            return ResultSetFactory.makeRewindable(rs);
        } catch (Exception e) {
            throw new SparqlQueryFailed(sparql, url.toASCIIString(), e);
        }
    }

    private ResultSet execSelectWithCache(String sparql) throws SparqlQueryFailed {
        Cache cache = cacheManager.getCache(CacheConfig.CACHENAME);
        int hashCode = Objects.hash(sparql.hashCode());

        // TODO check if cache is not null

        if (cache.get(hashCode) != null) {
            LOGGER.info("Cache hit for HashCode: {}", hashCode);

            ResultSetRewindable rsrw = cache.get(hashCode, ResultSetRewindable.class);

            // TODO check rsrw is not null

            rsrw.reset();

            return rsrw;
        } else {
            LOGGER.info("Cache miss for HashCode: {}", hashCode);

            ResultSet rs = this.execSelect(sparql);
            ResultSetRewindable rsrw = ResultSetFactory.makeRewindable(rs);
            cache.put(hashCode, rsrw);

            return rsrw;
        }
    }
}
