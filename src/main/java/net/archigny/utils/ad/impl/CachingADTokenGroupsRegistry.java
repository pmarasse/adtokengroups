package net.archigny.utils.ad.impl;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;

import org.ldaptive.ad.SecurityIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of token group registry using JCache in order to light the burden of active directory
 * 
 * @author Philippe MARASSE
 */
public class CachingADTokenGroupsRegistry extends SimpleADTokenGroupsRegistry {

    /** Logger */
    private static final Logger   log                  = LoggerFactory.getLogger(CachingADTokenGroupsRegistry.class);

    /** Cache name used by all instances of Registry */
    public static final String    CACHE_NAME           = "net.archigny.utils.ad.impl.adtokengroupsregistry";

    /** Default value for in-memory storage */
    public static final int       MAX_ELEMENTS         = 100;

    /** Default time to live for elements : 86400s =&gt; 1 day */
    public static final long      DEFAULT_TTL          = 86400;

    /** Default time to idle (maximum time between hits) for elements : 43200s =&gt; 12 hours */
    public static final long      DEFAULT_TTI          = 43200;

    /** Value used to specify null value caching as JSR107 does not allow this */
    public static final String    NULL                 = "";

    /** JCache {@link Cache} <SID (String), DN (String)> */
    private Cache<String, String> cache;

    /** { @link CacheManager} to use during initialization */
    private CacheManager          cacheManager;

    /** Time to Live for elements (seconds) */
    private long                  timeToLive           = DEFAULT_TTL;

    /** Time to Idle for elements (maximum seconds between accesses) */
    private long                  timeToIdle           = DEFAULT_TTI;

    /**
     * True if it's allowed to cache null group value (useful if search base is not the base of AD forest in order to avoid hitting
     * AD with unnecessary requests)
     */
    private boolean               cacheNullValues      = false;

    /** Cache instance is closed only if we created it */
    private boolean               closeCachePreDestroy = false;

    /**
     * Method called to initialize the bean
     */
    @PostConstruct
    public void afterPropertiesSet() {

        super.afterPropertiesSet();

        if (cache == null) {
            log.info("Cache not provided, trying to get one.");

            if (cacheManager == null) {
                log.info("Trying to get default cache manager");
                cacheManager = Caching.getCachingProvider().getCacheManager();
            } else {
                log.info("Using provided cache manager.");
            }
            if ((cache = cacheManager.getCache(CACHE_NAME, String.class, String.class)) == null) {
                Factory<ExpiryPolicy> policyFactory = CreatedAccessedExpiryPolicy.factoryOf(
                        new Duration(TimeUnit.SECONDS, timeToLive), new Duration(TimeUnit.SECONDS, timeToIdle));

                MutableConfiguration<String, String> config = new MutableConfiguration<String, String>().setTypes(String.class,
                        String.class).setExpiryPolicyFactory(policyFactory);

                cache = cacheManager.createCache(CACHE_NAME, config);
            } else {
                log.info("using existing cache instance - ignoring parameters maxElements, timeToLive, timeToIdle");
            }
            // Cache manager no longer needed
            cacheManager = null;
        }
    }

    /**
     * Method that should be called before destroying references to this bean
     */
    @PreDestroy
    public void destroy() {

        if (closeCachePreDestroy) {
            log.info("Closing locally created cache");
            cache.close();
        }
    }

    @Override
    public String getDnFromToken(final byte[] tokenGroup) {

        String sid;

        try {
            sid = SecurityIdentifier.toString(tokenGroup);
        } catch (Exception e) {
            log.error("An invalid SID has been passed as tokenGroup : {}", LdapUtils.toHexString(tokenGroup));
            // non parseable SID => will not attempt to contact LDAP directory
            return null;
        }

        // Cache lookup
        if (cache.containsKey(sid)) {
            String cachedValue = cache.get(sid);
            if (cacheNullValues && NULL.equals(cachedValue)) {
                return null;
            }
            return cachedValue;
        }

        // Cache Miss
        final String groupDN = super.getDnFromToken(tokenGroup);

        if (groupDN != null) {
            cache.put(sid, groupDN);
        } else if (cacheNullValues && groupDN == null) {
            cache.put(sid, NULL);
        }

        return groupDN;

    }

    // Getters and Setters

    public long getTimeToLive() {

        return timeToLive;
    }

    public void setTimeToLive(long timeToLive) {

        this.timeToLive = timeToLive;
    }

    public long getTimeToIdle() {

        return timeToIdle;
    }

    public void setTimeToIdle(long timeToIdle) {

        this.timeToIdle = timeToIdle;
    }

    public void setCacheNullValues(boolean cacheNullValues) {

        this.cacheNullValues = cacheNullValues;
    }

    public boolean isCacheNullValues() {

        return cacheNullValues;
    }

    public Cache<String, String> getCache() {

        return cache;
    }

    public void setCache(Cache<String, String> cache) {

        this.cache = cache;
    }

    public CacheManager getCacheManager() {

        return cacheManager;
    }

    public void setCacheManager(CacheManager cm) {

        this.cacheManager = cm;
    }

}
