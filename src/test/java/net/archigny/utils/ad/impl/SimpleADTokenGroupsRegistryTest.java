package net.archigny.utils.ad.impl;

import static org.junit.Assert.*;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.support.LdapUtils;

public class SimpleADTokenGroupsRegistryTest {

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

    public final Logger             log          = LoggerFactory.getLogger(SimpleADTokenGroupsRegistryTest.class);

    @Before
    public void setUp() throws Exception {

        if (cs == null) {
            log.debug("Create an instance of LdapContextSource");
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

        SimpleADTokenGroupsRegistry tokenRegistry = new SimpleADTokenGroupsRegistry();
        tokenRegistry.setContextSource(cs);
        tokenRegistry.setContextSourceBaseDN(BASE_DN);
        tokenRegistry.afterPropertiesSet();

        try {

            log.debug("Resolving first token : " + LdapUtils.convertBinarySidToString(TOKEN_1));
            String group1DN = tokenRegistry.getDnFromToken(TOKEN_1);
            log.debug("Found group DN : " + group1DN);
            LdapName name1 = new LdapName(group1DN);

            assertTrue(name1.equals(new LdapName(GROUP_1_NAME)));

            log.debug("Resolving second token : " + LdapUtils.convertBinarySidToString(TOKEN_2));
            String group2DN = tokenRegistry.getDnFromToken(TOKEN_2);
            log.debug("Found 2nd group DN " + group2DN);
            LdapName name2 = new LdapName(group2DN);

            assertTrue(name2.equals(new LdapName(GROUP_2_NAME)));

            log.debug("Resolving third token : " + LdapUtils.convertBinarySidToString(TOKEN_3));
            String group3DN = tokenRegistry.getDnFromToken(TOKEN_3);
            log.debug("Found 3rd group DN " + group3DN);
            LdapName name3 = new LdapName(group3DN);

            assertTrue(name3.equals(new LdapName(GROUP_3_NAME)));

            log.debug("Resolving fourth token : " + LdapUtils.convertBinarySidToString(TOKEN_4));
            String group4DN = tokenRegistry.getDnFromToken(TOKEN_4);
            log.debug("Found 4th group DN " + group4DN);
            LdapName name4 = new LdapName(group4DN);

            assertTrue(name4.equals(new LdapName(GROUP_4_NAME)));

            log.debug("Resolving fifth token : " + LdapUtils.convertBinarySidToString(TOKEN_5));
            String group5DN = tokenRegistry.getDnFromToken(TOKEN_5);
            log.debug("Found 5th group DN " + group5DN);
            LdapName name5 = new LdapName(group5DN);

            assertTrue(name5.equals(new LdapName(GROUP_5_NAME)));

            log.debug("try an unresolvable token");
            String noGroup = tokenRegistry.getDnFromToken("test-marchera-pas");
            assertNull(noGroup);

        } catch (InvalidNameException e) {
            fail("Unexpected InvalidNameException thrown");
        }
    }

    @Test
    public void getDNFromTokenTestWithNoBase() throws Exception {

        // Verifie l'incohérence si l'on indique une DN de base dans la ContextSource mais pas ici.
        SimpleADTokenGroupsRegistry tokenRegistry = new SimpleADTokenGroupsRegistry();
        tokenRegistry.setContextSource(cs);
        tokenRegistry.afterPropertiesSet();

        LdapName base = new LdapName(BASE_DN);
        
        log.debug("Resolving token : " + LdapUtils.convertBinarySidToString(TOKEN_1));
        String group1DN = tokenRegistry.getDnFromToken(TOKEN_1);
        log.debug("Found group DN : " + group1DN);
        LdapName name1 = new LdapName(group1DN);

        // Vérifie que le nom du groupe 1 ne possède pas le suffixe de base.
        assertTrue(name1.equals( (new LdapName(GROUP_1_NAME)).getSuffix(base.size()) ));        
        
    }

    
}
