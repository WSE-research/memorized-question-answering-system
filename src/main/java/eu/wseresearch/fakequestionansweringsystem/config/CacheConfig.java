package eu.wseresearch.fakequestionansweringsystem.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@SuppressWarnings("rawtypes")
public class CacheConfig {
    public static final String CACHENAME = "fakequestionansweringsystem.sparql.cache";

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    public Caffeine caffeineConfig() {
        return Caffeine.newBuilder().recordStats();
    }

    @SuppressWarnings("unchecked")
    @Bean
    public CacheManager cacheManager(Caffeine caffeine, @Value("${fakequestionansweringsystem.sparql.cache.specs:}") String caffeineSpec) {
        if (caffeineSpec == null || caffeineSpec == "") {
            caffeineSpec = "maximumSize=1,expireAfterAccess=0s"; // default value
        }
        LOGGER.info("cacheManager configuration: {}", caffeineSpec);
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(caffeine);
        caffeineCacheManager.setCacheSpecification(caffeineSpec);
        return caffeineCacheManager;
    }

}
