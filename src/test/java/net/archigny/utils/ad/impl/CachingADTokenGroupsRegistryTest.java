package net.archigny.utils.ad.impl;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import org.apache.commons.codec.binary.Base64;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.support.LdapUtils;

public class CachingADTokenGroupsRegistryTest {

    /** Logger */
    public final Logger                  log                = LoggerFactory.getLogger(CachingADTokenGroupsRegistryTest.class);

    public static LdapContextSource      cs;

    public final static String           BASE_DN            = "dc=TEST,dc=CH-POITIERS,dc=FR";

    public final static String           BIND_DN            = "cn=maven,ou=Applications," + BASE_DN;

    public final static String           BIND_PW            = "qdsFpRq9GFZ9e7pD";

    public final static String           SERVER_URL         = "ldap://ad2012test.ch-poitiers.fr";

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
    public final static byte[]           NON_EXISTENT_TOKEN = LdapUtils.convertStringSidToBinary("S-1-5-21-4134179593-3124312333-3049290520-513");

    private static CacheManager          cm;

    private static Cache<String, String> cache;

    @BeforeClass
    public static void prepare() {

        cm = Caching.getCachingProvider().getCacheManager();
        MutableConfiguration<String, String> config = new MutableConfiguration<String, String>().setTypes(String.class,
                String.class);
        config.setExpiryPolicyFactory(new FactoryBuilder.SingletonFactory<ExpiryPolicy>(new CreatedExpiryPolicy(
                new Duration(TimeUnit.SECONDS, 86400))));
        config.setStoreByValue(false);

        cache = cm.createCache(CachingADTokenGroupsRegistry.CACHE_NAME, config);
        cm.enableStatistics(CachingADTokenGroupsRegistry.CACHE_NAME, true);

    }

    @Before
    public void setUp() throws Exception {

        if (cs == null) {
            log.info("Create an instance of LdapContextSource");
            cs = new LdapContextSource();
            cs.setBase(BASE_DN);
            cs.setPooled(false);
            cs.setUserDn(BIND_DN);
            cs.setPassword(BIND_PW);
            cs.setUrl(SERVER_URL);
            cs.afterPropertiesSet();
        }

    }

    @AfterClass
    public static void tearDown() {

        cache.close();
        cm.close();
    }

    @Test
    public void getDNFromTokenTest() throws Exception {

        CachingADTokenGroupsRegistry tokenRegistry = new CachingADTokenGroupsRegistry();
        tokenRegistry.setMaxElements(3);
        tokenRegistry.setTimeToLive(1); // 1 seconde.
        tokenRegistry.setContextSource(cs);
        tokenRegistry.setContextSourceBaseDN(BASE_DN);
        tokenRegistry.afterPropertiesSet();

        try {

            // Cache Miss
            log.info("Resolving first token : {}", LdapUtils.convertBinarySidToString(TOKEN_1));
            long now = System.currentTimeMillis();
            String group1DN = tokenRegistry.getDnFromToken(TOKEN_1);
            long now2 = System.currentTimeMillis();
            log.info("Found group DN : {} (time : {} ms)", group1DN, (now2 - now));
            LdapName name1 = new LdapName(group1DN);

            assertTrue("Erreur ! Groupe attendu " + GROUP_1_NAME,name1.equals(new LdapName(GROUP_1_NAME)));

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
        tokenRegistry.setMaxElements(3);
        tokenRegistry.setTimeToLive(1); // 1 seconde.
        tokenRegistry.setContextSource(cs);
        tokenRegistry.setBaseDN("");
        tokenRegistry.afterPropertiesSet();

        @SuppressWarnings("unused")
        CachingADTokenGroupsRegistry tokenRegistry2 = new CachingADTokenGroupsRegistry();
        tokenRegistry.setMaxElements(3);
        tokenRegistry.setTimeToLive(1); // 1 seconde.
        tokenRegistry.setContextSource(cs);
        tokenRegistry.setBaseDN("");
        tokenRegistry.afterPropertiesSet();

    }

    @Test
    public void cacheNullTest() throws Exception {

        CachingADTokenGroupsRegistry tokenRegistry = new CachingADTokenGroupsRegistry();
        tokenRegistry.setMaxElements(3);
        tokenRegistry.setTimeToLive(1); // 1 seconde.
        tokenRegistry.setContextSource(cs);
        tokenRegistry.setBaseDN("ou=Utilisateurs");
        tokenRegistry.afterPropertiesSet();
        tokenRegistry.setCacheNullValues(true);

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

}
