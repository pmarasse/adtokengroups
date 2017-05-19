package net.archigny.utils.ad.impl;

import static org.junit.Assert.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.ExpiryPolicy;
import org.apache.commons.codec.binary.Base64;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ldaptive.BindOperation;
import org.ldaptive.BindRequest;
import org.ldaptive.Connection;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.ConnectionInitializer;
import org.ldaptive.Credential;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.LdapException;
import org.ldaptive.Response;
import org.ldaptive.provider.unboundid.UnboundIDProvider;
import org.ldaptive.provider.unboundid.UnboundIDProviderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unboundid.ldap.sdk.LDAPConnectionOptions;

public class CachingADTokenGroupsRegistryTest implements ConnectionInitializer {

    /** Logger */
    private static final Logger          log                = LoggerFactory.getLogger(CachingADTokenGroupsRegistryTest.class);

    public final static String           BASE_DN            = "dc=TEST,dc=CH-POITIERS,dc=FR";

    public final static String           BIND_DN            = "cn=maven,ou=Applications," + BASE_DN;

    public final static String           BIND_PW            = "qdsFpRq9GFZ9e7pD";

    public final static String           SERVER_URL         = "ldap://ad2012test.ch-poitiers.fr";

    public final static BindRequest      LDAP_BIND          = new BindRequest(BIND_DN, new Credential(BIND_PW));

    /* Token list taken from ldap search : ldapsearch -x -D "bindDN" -w bindPW -H ldap://ad2012test.ch-poitiers.fr -b
     * "CN=Cathelyn Stark,OU=Utilisateurs,DC=TEST,DC=CH-POITIERS,DC=FR" -s base tokenGroups */
    public final static byte[]           TOKEN_1            = Base64.decodeBase64("AQIAAAAAAAUgAAAAIQIAAA==");

    public final static String           GROUP_1_NAME       = "CN=Users,CN=Builtin," + BASE_DN;

    public final static byte[]           TOKEN_2            = Base64.decodeBase64("AQUAAAAAAAUVAAAACZNq9g05OboEe8C1bwQAAA==");

    /** Indirect group (Stark => North => Westeros) */
    public final static String           GROUP_2_NAME       = "cn=Westeros,ou=Groupes," + BASE_DN;

    public final static byte[]           TOKEN_3            = Base64.decodeBase64("AQUAAAAAAAUVAAAACZNq9g05OboEe8C1UQQAAA==");

    /** direct group */
    public final static String           GROUP_3_NAME       = "cn=Stark,ou=Groupes," + BASE_DN;

    public final static byte[]           TOKEN_4            = Base64.decodeBase64("AQUAAAAAAAUVAAAACZNq9g05OboEe8C1YAQAAA==");

    /** Indirect group */
    public final static String           GROUP_4_NAME       = "cn=North,ou=Groupes," + BASE_DN;

    public final static byte[]           TOKEN_5            = Base64.decodeBase64("AQUAAAAAAAUVAAAACZNq9g05OboEe8C1AQIAAA==");

    /** Direct group */
    public final static String           GROUP_5_NAME       = "cn=Domain Users,cn=Users," + BASE_DN;

    public final static byte[]           TOKEN_6            = Base64.decodeBase64("AQUAAAAAAAUVAAAACZNq9g05OboEe8C1cAQAAA==");

    /** Indirect group Tully => Riverlands */
    public final static String           GROUP_6_NAME       = "cn=Riverlands,ou=Groupes," + BASE_DN;

    public final static byte[]           TOKEN_7            = Base64.decodeBase64("AQUAAAAAAAUVAAAACZNq9g05OboEe8C1UwQAAA==");

    /** Direct group */
    public final static String           GROUP_7_NAME       = "cn=Tully,ou=Groupes," + BASE_DN;

    /** non existent token */
    public final static byte[]           NON_EXISTENT_TOKEN = LdapUtils
            .convertStringSidToBinary("S-1-5-21-4134179593-3124312333-3049290520-513");

    /** LDAP Connection factory */
    public static ConnectionFactory      ldapConnectionFactory;

    /** HazelCast JCache Manager */
    private static CacheManager          hazelCm;

    /** HazelCast JCache */
    private static Cache<String, String> hazelCache;

    /** EhCache JCache Manager */  
    private static CacheManager          ehCm;

    /** EhCache JCache */
    private static Cache<String, String> ehCache;

    @BeforeClass
    public static void prepare() {

        hazelCm = Caching.getCachingProvider("com.hazelcast.cache.HazelcastCachingProvider").getCacheManager();
        MutableConfiguration<String, String> config = new MutableConfiguration<String, String>().setTypes(String.class,
                String.class);
        config.setExpiryPolicyFactory(new FactoryBuilder.SingletonFactory<ExpiryPolicy>(new CreatedExpiryPolicy(
                new javax.cache.expiry.Duration(TimeUnit.SECONDS, 86400))));
        config.setStoreByValue(false);

        hazelCache = hazelCm.createCache(CachingADTokenGroupsRegistry.CACHE_NAME, config);
        hazelCm.enableStatistics(CachingADTokenGroupsRegistry.CACHE_NAME, true);

        // FQDN is in file javax.cache.spi.CachingProvider in META-INF/services of each JSR107 cache provider
        // Thanks to https://dzone.com/articles/random-jcache-stuff-multiple-providers-and-jmx-bea
        ehCm = Caching.getCachingProvider("org.ehcache.jsr107.EhcacheCachingProvider").getCacheManager();
        ehCache = ehCm.createCache(CachingADTokenGroupsRegistry.CACHE_NAME, config);
        
    }

    @Before
    public void setUp() throws Exception {

        LDAPConnectionOptions lco = new LDAPConnectionOptions();
        lco.setAbandonOnTimeout(true);

        UnboundIDProviderConfig lpc = new UnboundIDProviderConfig();
        lpc.setConnectionOptions(lco);

        UnboundIDProvider lp = new UnboundIDProvider();
        lp.setProviderConfig(lpc);

        ConnectionConfig cc = new ConnectionConfig(SERVER_URL);
        cc.setConnectTimeout(Duration.ofMillis(3000L));
        cc.setResponseTimeout(Duration.ofMillis(30000L));
        cc.setConnectionInitializer(this);

        ldapConnectionFactory = new DefaultConnectionFactory(cc, lp);

    }

    @AfterClass
    public static void tearDown() {

        hazelCache.close();
        hazelCm.close();
    }

    @Test
    public void getDNFromTokenWithHazelCast() throws Exception {
        getDNFromTokenTest(hazelCache);
    }
    
    @Test
    public void getDNFromTokenWithEhCache() throws Exception {
        getDNFromTokenTest(ehCache);
    }
        
    public void getDNFromTokenTest(Cache<String,String> cache) throws Exception {

        CachingADTokenGroupsRegistry tokenRegistry = new CachingADTokenGroupsRegistry();
        tokenRegistry.setLdapConnectionFactory(ldapConnectionFactory);
        tokenRegistry.setTimeToLive(1); // 1 seconde.
        tokenRegistry.setBaseDN(BASE_DN);
        tokenRegistry.setCache(cache);
        tokenRegistry.afterPropertiesSet();

        try {

            // Cache Miss
            log.info("Resolving first token : {}", LdapUtils.convertBinarySidToString(TOKEN_1));
            long now = System.currentTimeMillis();
            String group1DN = tokenRegistry.getDnFromToken(TOKEN_1);
            long now2 = System.currentTimeMillis();
            log.info("Found group DN : {} (time : {} ms)", group1DN, (now2 - now));
            LdapName name1 = new LdapName(group1DN);

            assertTrue("Error ! expected group " + GROUP_1_NAME, name1.equals(new LdapName(GROUP_1_NAME)));

            // Cache Hit
            now = System.currentTimeMillis();
            @SuppressWarnings("unused")
            String dummy = tokenRegistry.getDnFromToken(TOKEN_1);
            now2 = System.currentTimeMillis();
            log.info("Second fetch time : " + (now2 - now) + " ms.");

            // Cache Miss
            log.info("Resolving second token : {}", LdapUtils.convertBinarySidToString(TOKEN_2));
            now = System.currentTimeMillis();
            String group2DN = tokenRegistry.getDnFromToken(TOKEN_2);
            now2 = System.currentTimeMillis();
            log.info("Found 2nd group DN " + group2DN + " (time : " + (now2 - now) + " ms)");
            LdapName name2 = new LdapName(group2DN);

            assertTrue(name2.equals(new LdapName(GROUP_2_NAME)));

            // Cache Miss
            log.info("Resolving third token : " + LdapUtils.convertBinarySidToString(TOKEN_3));
            String group3DN = tokenRegistry.getDnFromToken(TOKEN_3);
            log.info("Found 3rd group DN " + group3DN);
            LdapName name3 = new LdapName(group3DN);

            assertTrue(name3.equals(new LdapName(GROUP_3_NAME)));

            // Cache Miss + eviction
            log.info("Resolving fourth token : " + LdapUtils.convertBinarySidToString(TOKEN_4));
            String group4DN = tokenRegistry.getDnFromToken(TOKEN_4);
            log.info("Found 4th group DN " + group4DN);
            LdapName name4 = new LdapName(group4DN);

            assertTrue(name4.equals(new LdapName(GROUP_4_NAME)));

            // Cache Miss + eviction
            log.info("Resolving fifth token : " + LdapUtils.convertBinarySidToString(TOKEN_5));
            String group5DN = tokenRegistry.getDnFromToken(TOKEN_5);
            log.info("Found 5th group DN " + group5DN);
            LdapName name5 = new LdapName(group5DN);

            assertTrue(name5.equals(new LdapName(GROUP_5_NAME)));

            // Neither hit nor miss... unconvertible to SID => dropped
            log.info("try an unresolvable token");
            String noGroup = tokenRegistry.getDnFromToken("test-marchera-pas");
            assertNull(noGroup);

            log.info("Waiting more than timeToIdle");
            Thread.sleep(900);
            // Cache Hit
            dummy = tokenRegistry.getDnFromToken(TOKEN_1);
            Thread.sleep(900);
            // Cache Miss (TTL = 1s)
            dummy = tokenRegistry.getDnFromToken(TOKEN_1);

        } catch (InvalidNameException e) {
            fail("Unexpected InvalidNameException thrown");
        }
    }

    @Test
    public void DualRegistrySharedCache() throws Exception {

        // Test that fails on version 0.1.0

        CachingADTokenGroupsRegistry tokenRegistry = new CachingADTokenGroupsRegistry();
        tokenRegistry.setLdapConnectionFactory(ldapConnectionFactory);
        tokenRegistry.setTimeToLive(1); // 1 seconde.
        tokenRegistry.setBaseDN("");
        tokenRegistry.setCache(hazelCache);
        tokenRegistry.afterPropertiesSet();

        CachingADTokenGroupsRegistry tokenRegistry2 = new CachingADTokenGroupsRegistry();
        tokenRegistry2.setLdapConnectionFactory(ldapConnectionFactory);
        tokenRegistry2.setTimeToLive(1); // 1 seconde.
        tokenRegistry2.setBaseDN("");
        tokenRegistry2.setCache(hazelCache);
        tokenRegistry2.afterPropertiesSet();
    }

    @Test
    public void cacheNullWithHazelCastTest() throws Exception {
        cacheNullTest(hazelCache);
    }
    
    @Test
    public void cacheNullWithEhCacheTest() throws Exception {
        cacheNullTest(ehCache);
    }
    
    public void cacheNullTest(Cache<String,String> cache) throws Exception {

        CachingADTokenGroupsRegistry tokenRegistry = new CachingADTokenGroupsRegistry();
        tokenRegistry.setLdapConnectionFactory(ldapConnectionFactory);
        tokenRegistry.setTimeToLive(1); // 1 seconde.
        tokenRegistry.setBaseDN(BASE_DN);
        tokenRegistry.setCache(cache);
        tokenRegistry.setCacheNullValues(true);
        tokenRegistry.afterPropertiesSet();

        Thread.sleep(2000);

        // Cache Miss
        log.info("Resolving unknown token : {}", LdapUtils.convertBinarySidToString(NON_EXISTENT_TOKEN));
        long now = System.currentTimeMillis();
        String group1DN = tokenRegistry.getDnFromToken(NON_EXISTENT_TOKEN);
        long now2 = System.currentTimeMillis();
        log.info("Querying group DN time : {} ms trouvé : {}", (now2 - now), group1DN);

        assertNull("Expected NULL, found : " + group1DN, group1DN);

        now = System.currentTimeMillis();
        group1DN = tokenRegistry.getDnFromToken(NON_EXISTENT_TOKEN);
        now2 = System.currentTimeMillis();
        log.info("Querying (cached) group DN time : {} ms", (now2 - now));

        assertNull("Expected NULL, found : " + group1DN, group1DN);

    }

    @Override
    public Response<Void> initialize(Connection conn) throws LdapException {

        BindOperation bind = new BindOperation(conn);
        bind.execute(LDAP_BIND);

        // TODO Auto-generated method stub
        return null;
    }

}
