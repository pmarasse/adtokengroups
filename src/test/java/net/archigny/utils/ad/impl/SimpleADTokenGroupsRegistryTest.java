package net.archigny.utils.ad.impl;

import static org.junit.Assert.*;

import java.time.Duration;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
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
import org.ldaptive.ad.SecurityIdentifier;
import org.ldaptive.provider.unboundid.UnboundIDProvider;
import org.ldaptive.provider.unboundid.UnboundIDProviderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unboundid.ldap.sdk.LDAPConnectionOptions;

public class SimpleADTokenGroupsRegistryTest implements ConnectionInitializer {

    /** LDAP Connection factory */
    public static ConnectionFactory ldapConnectionFactory;

    public final static String      BASE_DN      = "dc=TEST,dc=CH-POITIERS,dc=FR";

    public final static String      BIND_DN      = "cn=maven,ou=Applications," + BASE_DN;

    public final static String      BIND_PW      = "qdsFpRq9GFZ9e7pD";

    public final static String      SERVER_URL   = "ldap://ad2012test.ch-poitiers.fr";

    public final static BindRequest LDAP_BIND    = new BindRequest(BIND_DN, new Credential(BIND_PW));

    /*
     * Token list taken from ldap search : ldapsearch -x -D "bindDN" -w bindPW -H ldap://ad2012test.ch-poitiers.fr -b
     * "CN=Cathelyn Stark,OU=Utilisateurs,DC=TEST,DC=CH-POITIERS,DC=FR" -s base tokenGroups
     */
    public final static byte[]      TOKEN_1      = Base64.decodeBase64("AQIAAAAAAAUgAAAAIQIAAA==");

    public final static String      GROUP_1_NAME = "CN=Users,CN=Builtin," + BASE_DN;

    public final static byte[]      TOKEN_2      = Base64.decodeBase64("AQUAAAAAAAUVAAAACZNq9g05OboEe8C1bwQAAA==");

    /** Indirect group (Stark => North => Westeros) */
    public final static String      GROUP_2_NAME = "cn=Westeros,ou=Groupes," + BASE_DN;

    public final static byte[]      TOKEN_3      = Base64.decodeBase64("AQUAAAAAAAUVAAAACZNq9g05OboEe8C1UQQAAA==");

    /** direct group */
    public final static String      GROUP_3_NAME = "cn=Stark,ou=Groupes," + BASE_DN;

    public final static byte[]      TOKEN_4      = Base64.decodeBase64("AQUAAAAAAAUVAAAACZNq9g05OboEe8C1YAQAAA==");

    /** Indirect group */
    public final static String      GROUP_4_NAME = "cn=North,ou=Groupes," + BASE_DN;

    public final static byte[]      TOKEN_5      = Base64.decodeBase64("AQUAAAAAAAUVAAAACZNq9g05OboEe8C1AQIAAA==");

    /** Direct group */
    public final static String      GROUP_5_NAME = "cn=Domain Users,cn=Users," + BASE_DN;

    public final static byte[]      TOKEN_6      = Base64.decodeBase64("AQUAAAAAAAUVAAAACZNq9g05OboEe8C1cAQAAA==");

    /** Indirect group Tully => Riverlands */
    public final static String      GROUP_6_NAME = "cn=Riverlands,ou=Groupes," + BASE_DN;

    public final static byte[]      TOKEN_7      = Base64.decodeBase64("AQUAAAAAAAUVAAAACZNq9g05OboEe8C1UwQAAA==");

    /** Direct group */
    public final static String      GROUP_7_NAME = "cn=Tully,ou=Groupes," + BASE_DN;

    public final Logger             log          = LoggerFactory.getLogger(SimpleADTokenGroupsRegistryTest.class);

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

        ldapConnectionFactory = new DefaultConnectionFactory(cc,lp);
    }

    @Test
    public void getDNFromTokenTest() throws Exception {

        SimpleADTokenGroupsRegistry tokenRegistry = new SimpleADTokenGroupsRegistry();
        tokenRegistry.setLdapConnectionFactory(ldapConnectionFactory);
        tokenRegistry.setBaseDN(BASE_DN);
        tokenRegistry.afterPropertiesSet();

        try {

            log.debug("Resolving first token : " + SecurityIdentifier.toString(TOKEN_1));
            String group1DN = tokenRegistry.getDnFromToken(TOKEN_1);
            log.debug("Found group DN : " + group1DN);
            log.debug("Expected DN    : " + GROUP_1_NAME);
            LdapName name1 = new LdapName(group1DN);

            assertTrue(name1.equals(new LdapName(GROUP_1_NAME)));

            log.debug("Resolving second token : " + SecurityIdentifier.toString(TOKEN_2));
            String group2DN = tokenRegistry.getDnFromToken(TOKEN_2);
            log.debug("Found 2nd group DN " + group2DN);
            LdapName name2 = new LdapName(group2DN);

            assertTrue(name2.equals(new LdapName(GROUP_2_NAME)));

            log.debug("Resolving third token : " + SecurityIdentifier.toString(TOKEN_3));
            String group3DN = tokenRegistry.getDnFromToken(TOKEN_3);
            log.debug("Found 3rd group DN " + group3DN);
            LdapName name3 = new LdapName(group3DN);

            assertTrue(name3.equals(new LdapName(GROUP_3_NAME)));

            log.debug("Resolving fourth token : " + SecurityIdentifier.toString(TOKEN_4));
            String group4DN = tokenRegistry.getDnFromToken(TOKEN_4);
            log.debug("Found 4th group DN " + group4DN);
            LdapName name4 = new LdapName(group4DN);

            assertTrue(name4.equals(new LdapName(GROUP_4_NAME)));

            log.debug("Resolving fifth token : " + SecurityIdentifier.toString(TOKEN_5));
            String group5DN = tokenRegistry.getDnFromToken(TOKEN_5);
            log.debug("Found 5th group DN " + group5DN);
            LdapName name5 = new LdapName(group5DN);

            assertTrue(name5.equals(new LdapName(GROUP_5_NAME)));

            log.debug("Resolving sixth token : " + SecurityIdentifier.toString(TOKEN_6));
            String group6DN = tokenRegistry.getDnFromToken(TOKEN_6);
            log.debug("Found 6th group DN " + group6DN);
            LdapName name6 = new LdapName(group6DN);

            assertTrue(name6.equals(new LdapName(GROUP_6_NAME)));

            log.debug("Resolving seventh token : " + SecurityIdentifier.toString(TOKEN_7));
            String group7DN = tokenRegistry.getDnFromToken(TOKEN_7);
            log.debug("Found 7th group DN " + group7DN);
            LdapName name7 = new LdapName(group7DN);

            assertTrue(name7.equals(new LdapName(GROUP_7_NAME)));

            log.debug("try an unresolvable token");
            String noGroup = tokenRegistry.getDnFromToken("test-marchera-pas");
            assertNull(noGroup);

        } catch (InvalidNameException e) {
            fail("Unexpected InvalidNameException thrown");
        }
    }

    @Override
    public Response<Void> initialize(Connection conn) throws LdapException {

        BindOperation bind = new BindOperation(conn);
        bind.execute(LDAP_BIND);

        // TODO Auto-generated method stub
        return null;
    }

}
