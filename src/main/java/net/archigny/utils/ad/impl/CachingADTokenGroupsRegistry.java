package net.archigny.utils.ad.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.ldap.support.LdapUtils;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.LfuPolicy;
import net.sf.ehcache.store.Policy;

/**
 * Implementation of token group registry using EhCache in order to light the burden of active directory 
 * 
 * @author Philippe MARASSE
 *
 */
public class CachingADTokenGroupsRegistry extends SimpleADTokenGroupsRegistry implements DisposableBean {

    private final Logger       log             = LoggerFactory.getLogger(CachingADTokenGroupsRegistry.class);

    /**
     * Cache name used by all instances of Registry
     */
    public static final String CACHE_NAME      = "net.archigny.utils.ad.impl.cachingadtokengroupsregistry";

    /**
     * Default value for in-memory storage
     */
    public static final int    MAX_ELEMENTS    = 100;

    /**
     * Default time to live for elements : 86400s =&gt; 1 day
     */
    public static final long   DEFAULT_TTL     = 86400;

    /**
     * Default time to idle (maximum time between hits) for elements : 43200 =&gt; 12 hours
     */
    public static final long   DEFAULT_TTI     = 43200;

    /**
     * EhCache cache
     */
    private Cache              cache;

    /**
     * Number of cached elements
     */
    private int                maxElements     = MAX_ELEMENTS;

    /**
     * Cache Eviction Policy (default : LFU)
     */
    private Policy             policy          = new LfuPolicy();

    /**
     * Time to live for elements (seconds)
     */
    private long               timeToLive      = DEFAULT_TTL;

    /**
     * Time to Idle for elements (maximum seconds between accesses)
     */
    private long               timeToIdle      = DEFAULT_TTI;

    /**
     * True if it's allowed to cache null group value (useful if search base is not the base of AD forest in order to avoid hitting
     * AD with unnecessary requests)
     */
    private boolean            cacheNullValues = false;

    /**
     * Method called to initialize the bean
     */
    @Override
    public void afterPropertiesSet() throws Exception {

        super.afterPropertiesSet();

        if (cache == null) {
            log.info("Cache not provided, trying to get one.");

            CacheManager cm = CacheManager.getInstance();
            if ((cache = cm.getCache(CACHE_NAME)) == null) {
                cache = new Cache(CACHE_NAME, maxElements, false, false, timeToLive, timeToIdle);
                cm.addCache(cache);
                cache.setMemoryStoreEvictionPolicy(policy);
            } else {
                log.info("using existing cache instance - ignoring parameters maxElements, timeToLive, timeToIdle");
            }
        }
    }

    /**
     * Method that should be called before destroying references to this bean
     */
    @Override
    public void destroy() {

        final CacheManager cm = CacheManager.getInstance();
        cm.removeCache(CACHE_NAME);
        final String[] cacheNames = cm.getCacheNames();

        // Shutdown cache manager if no other cache is registered
        if (cacheNames.length == 0) {
            cm.shutdown();
        }
    }

    @Override
    public String getDnFromToken(final String tokenGroup) {

        return getDnFromToken(tokenGroup.getBytes());
    }

    @Override
    public String getDnFromToken(final byte[] tokenGroup) {

        String sid;

        try {
            sid = LdapUtils.convertBinarySidToString(tokenGroup);
        } catch (Exception e) {
            log.error("An invalid SID has been passed as tokenGroup : " + toHexString(tokenGroup));
            // non parseable SID => will not attempt to contact LDAP directory
            return null;
        }

        // Cache lookup
        final Element cachedElement;
        if ((cachedElement = cache.get(sid)) != null) {
            // Cache Hit
            return (String) cachedElement.getObjectValue();
        }

        // Cache Miss
        final String groupDN = super.getDnFromToken(tokenGroup);

        if ((groupDN != null) || (cacheNullValues)) {
            cache.put(new Element(sid, groupDN));
        }

        return groupDN;

    }

    private final String toHexString(final byte[] bytes) {

        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Getters and Setters

    public int getMaxElements() {

        return maxElements;
    }

    public void setMaxElements(int maxElements) {

        this.maxElements = maxElements;
    }

    public Policy getPolicy() {

        return policy;
    }

    public void setPolicy(final Policy policy) {

        this.policy = policy;
    }

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

    public Cache getCache() {

        return cache;
    }

    public void setCache(final Cache cache) {

        this.cache = cache;
    }

    public void setCacheNullValues(boolean cacheNullValues) {

        this.cacheNullValues = cacheNullValues;
    }

    public boolean isCacheNullValues() {

        return cacheNullValues;
    }

}
