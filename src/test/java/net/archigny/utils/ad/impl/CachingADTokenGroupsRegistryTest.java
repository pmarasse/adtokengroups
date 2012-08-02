package net.archigny.utils.ad.impl;

import static org.junit.Assert.*;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Statistics;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.support.LdapUtils;

public class CachingADTokenGroupsRegistryTest {

    public static LdapContextSource cs;

    public final static String      BASE_DN      = "dc=in,dc=archigny,dc=org";

    public final static String      BIND_DN      = "cn=Application Test,ou=Applications,ou=Utilisateurs,dc=in,dc=archigny,dc=org";

    public final static String      BIND_PW      = "123456";

    public final static String      SERVER_URL   = "ldap://win2k8.in.archigny.org";

    /*
     * Token list taken from ldap search : ldapsearch -x -D "bindDN" -w bindPW -H ldaps://win2k8.in.archigny.org:636 -b
     * "cn=philippe marasse,ou=personnes,ou=Utilisateurs,dc=in,dc=archigny,dc=org" "(objectclass=*)" -s base tokenGroups
     */
    public final static byte[]      TOKEN_1      = Base64.decodeBase64("AQIAAAAAAAUgAAAAIQIAAA==");

    public final static String      GROUP_1_NAME = "CN=Utilisateurs,CN=Builtin,DC=in,DC=archigny,DC=org";

    public final static byte[]      TOKEN_2      = Base64.decodeBase64("AQUAAAAAAAUVAAAA04tVpgfmcYBuYZhlWQQAAA==");

    public final static String      GROUP_2_NAME = "cn=Groupe local indirect,ou=Groupes,dc=in,dc=archigny,dc=org";

    public final static byte[]      TOKEN_3      = Base64.decodeBase64("AQUAAAAAAAUVAAAA04tVpgfmcYBuYZhlWAQAAA==");

    public final static String      GROUP_3_NAME = "cn=groupe indirect,ou=Groupes,dc=in,dc=archigny,dc=org";

    public final static byte[]      TOKEN_4      = Base64.decodeBase64("AQUAAAAAAAUVAAAA04tVpgfmcYBuYZhlAQIAAA==");

    public final static String      GROUP_4_NAME = "cn=Utilisateurs du domaine,cn=Users,dc=in,dc=archigny,dc=org";

    public final static byte[]      TOKEN_5      = Base64.decodeBase64("AQUAAAAAAAUVAAAA04tVpgfmcYBuYZhlVwQAAA==");

    public final static String      GROUP_5_NAME = "cn=groupe direct,ou=Groupes,dc=in,dc=archigny,dc=org";

    public final Logger             log          = LoggerFactory.getLogger(CachingADTokenGroupsRegistryTest.class);

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

    @Test
    public void getDNFromTokenTest() throws Exception {

        CachingADTokenGroupsRegistry tokenRegistry = new CachingADTokenGroupsRegistry();
        tokenRegistry.setMaxElements(3);
        tokenRegistry.setTimeToLive(1); // 1 seconde.
        tokenRegistry.setContextSource(cs);
        tokenRegistry.setContextSourceBaseDN(BASE_DN);
        tokenRegistry.afterPropertiesSet();
        Cache cache = tokenRegistry.getCache();
        cache.setStatisticsEnabled(true);

        try {

            // Cache Miss
            log.info("Resolving first token : " + LdapUtils.convertBinarySidToString(TOKEN_1));
            long now = System.currentTimeMillis();
            String group1DN = tokenRegistry.getDnFromToken(TOKEN_1);
            long now2 = System.currentTimeMillis();
            log.info("Found group DN : " + group1DN + "(time : " + (now2 - now) + " ms)");
            LdapName name1 = new LdapName(group1DN);

            assertTrue(name1.equals(new LdapName(GROUP_1_NAME)));

            // Cache Hit
            now = System.currentTimeMillis();
            @SuppressWarnings("unused")
            String dummy = tokenRegistry.getDnFromToken(TOKEN_1);
            now2 = System.currentTimeMillis();
            log.info("Second fetch time : " + (now2 - now) + " ms.");

            // Cache Miss
            log.info("Resolving second token : " + LdapUtils.convertBinarySidToString(TOKEN_2));
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

            Statistics stats = cache.getStatistics();
            log.info("Hits : " + stats.getCacheHits() + " / Miss : " + stats.getCacheMisses() + " / objets en cache : "
                    + stats.getObjectCount() + " / evictions du cache : " + stats.getEvictionCount());

            assertEquals(1, stats.getCacheHits());
            assertEquals(5, stats.getCacheMisses());
            assertEquals(3, stats.getObjectCount());
            assertEquals(2, stats.getEvictionCount());

            log.info("Waiting more than timeToIdle");
            Thread.sleep(900);
            // Cache Hit
            dummy = tokenRegistry.getDnFromToken(TOKEN_1);
            Thread.sleep(900);
            // Cache Miss (TTL = 1s)
            dummy = tokenRegistry.getDnFromToken(TOKEN_1);

            stats = cache.getStatistics();
            log.info("Hits : " + stats.getCacheHits() + " / Miss : " + stats.getCacheMisses() + " / objets en cache : "
                    + stats.getObjectCount() + " / evictions du cache : " + stats.getEvictionCount());

            assertEquals(2, stats.getCacheHits());
            assertEquals(6, stats.getCacheMisses());
            assertEquals(3, stats.getObjectCount());
            assertEquals(2, stats.getEvictionCount());

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
        Cache cache = tokenRegistry.getCache();
        cache.setStatisticsEnabled(true);

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
        Cache cache = tokenRegistry.getCache();
        cache.setStatisticsEnabled(true);

        Thread.sleep(2000);
        
        // Cache Miss
        log.info("Resolving first token : " + LdapUtils.convertBinarySidToString(TOKEN_1));
        long now = System.currentTimeMillis();
        String group1DN = tokenRegistry.getDnFromToken(TOKEN_1);
        long now2 = System.currentTimeMillis();
        log.info("Querying group DN time : " + (now2 - now) + " ms)");

        assertNull(group1DN);

        long hits = cache.getStatistics().getInMemoryHits();
        
        now = System.currentTimeMillis();
        group1DN = tokenRegistry.getDnFromToken(TOKEN_1);
        now2 = System.currentTimeMillis();
        log.info("Querying (cached) group DN time : " + (now2 - now) + " ms)");

        assertEquals(1, cache.getStatistics().getInMemoryHits() - hits);
        
    }

}
