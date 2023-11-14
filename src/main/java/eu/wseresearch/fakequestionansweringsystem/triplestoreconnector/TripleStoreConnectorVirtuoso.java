package eu.wseresearch.fakequestionansweringsystem.triplestoreconnector;

import eu.wseresearch.fakequestionansweringsystem.config.CacheConfig;
import eu.wseresearch.fakequestionansweringsystem.triplestoreconnector.exception.SparqlQueryFailed;
import org.apache.jena.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import virtuoso.jena.driver.*;

import java.util.Objects;

/**
 * @author AnBo-de
 * <p>
 * the component connects to a Virtuoso triplestore for connecting and
 * executing queries
 * <p>
 * the component is initialized if and only if the required information
 * is available
 * <p>
 * required parameters
 *
 * <pre>
 *  <code>
 *      virtuoso.url=
 *      virtuoso.graph=
 *      virtuoso.username=
 *      virtuoso.password=
 *  </code>
 * </pre>
 */
@ConditionalOnProperty(name = {"virtuoso.url", "virtuoso.graph", "virtuoso.username", "virtuoso.password"}, matchIfMissing = false)
@Component
public class TripleStoreConnectorVirtuoso extends TripleStoreConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(TripleStoreConnectorVirtuoso.class);

    private final String virtuosoUrl;
    private final String virtuosoGraph;
    private final String username;
    private final String password;
    private CacheManager cacheManager;
    private VirtGraph connection;

    public TripleStoreConnectorVirtuoso(
            @Value("${virtuoso.url}") String virtuosoUrl, //
            @Value("${virtuoso.graph}") String virtuosoGraph, //
            @Value("${virtuoso.username}") String username, //
            @Value("${virtuoso.password}") String password, //
            @Autowired CacheManager cacheManager //
    ) {
        LOGGER.debug("initialize Virtuoso triplestore connector: {} : {}", virtuosoUrl, virtuosoGraph);
        this.virtuosoUrl = virtuosoUrl;
        this.virtuosoGraph = virtuosoGraph;
        this.username = username;
        this.password = password;
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

    @Override
    public void connect() {
        LOGGER.debug("Virtuoso server connecting to {}", this.getVirtuosoUrl());
        assert this.virtuosoUrl != null && !"".equals(this.virtuosoUrl);
        assert this.virtuosoGraph != null && !"".equals(this.virtuosoGraph);
        assert this.username != null && !"".equals(this.username);
        assert this.password != null && !"".equals(this.password);

        connection = new VirtGraph(this.getVirtuosoGraph(), this.getVirtuosoUrl(), this.getUsername(), this.getPassword());
        LOGGER.info("Virtuoso server connected at {}", this.getVirtuosoUrl());
        assert connection != null;
    }

    @Override
    public ResultSet select(String sparql) throws SparqlQueryFailed {
        try {
            if (this.cacheManager == null || this.cacheManager.getCache(CacheConfig.CACHENAME) == null) {
                return execSelect(sparql);
            }

            return execSelectWithCache(sparql, this.cacheManager.getCache(CacheConfig.CACHENAME));
        } catch (Exception e) {
            throw new SparqlQueryFailed(sparql, this.virtuosoUrl.toString() + this.virtuosoGraph, e);
        }
    }

    @Override
    public void update(String sparql) throws SparqlQueryFailed {
        VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(sparql, this.connection);
        vur.exec();
    }

    private ResultSet execSelect(String sparql) throws SparqlQueryFailed {
        LOGGER.info("no Cache is used");

         Query query = QueryFactory.create(sparql);

        try (VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(query, this.connection)) {
            ResultSet rs = vqe.execSelect();
            ResultSetRewindable resultsRewindable = ResultSetFactory.makeRewindable(rs);

            return resultsRewindable;
        } catch (Exception e) {
            throw new SparqlQueryFailed(sparql, this.virtuosoUrl, e);
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
