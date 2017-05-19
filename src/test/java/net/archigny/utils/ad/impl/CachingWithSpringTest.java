package net.archigny.utils.ad.impl;

import static org.junit.Assert.*;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.ldaptive.ad.SecurityIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import net.archigny.utils.ad.api.IActiveDirectoryTokenGroupsRegistry;

public class CachingWithSpringTest {

    private static final Logger log            = LoggerFactory.getLogger(CachingADTokenGroupsRegistryTest.class);

    public final static byte[]  TOKEN_2        = Base64.decodeBase64("AQUAAAAAAAUVAAAACZNq9g05OboEe8C1bwQAAA==");

    public final static String  GROUP_2_PREFIX = "cn=Westeros,ou=Groupes,";

    @Test
    public void test() throws InvalidNameException {

        @SuppressWarnings("resource")
        ApplicationContext ctx = new ClassPathXmlApplicationContext("app-test.xml");

        IActiveDirectoryTokenGroupsRegistry tokenRegistry = ctx.getBean(IActiveDirectoryTokenGroupsRegistry.class);

        assertNotNull("Registry should not be null !!", tokenRegistry);

        String baseDN = ctx.getBean("ActiveDirectoryBaseDN", String.class);
        String groupName = GROUP_2_PREFIX + baseDN;

        log.info("Resolving token : {}", SecurityIdentifier.toString(TOKEN_2));
        String group2DN = tokenRegistry.getDnFromToken(TOKEN_2);
        LdapName name2 = new LdapName(group2DN);

        assertTrue(name2.equals(new LdapName(groupName)));

    }

}
