package net.archigny.utils.ad.impl;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.ldaptive.Connection;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.LdapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import net.archigny.utils.ad.api.IActiveDirectoryTokenGroupsRegistry;

/**
 * Abstract class implementing base functions.
 * 
 * @author Philippe MARASSE
 */
public abstract class AbstractADTokenGroupsRegistry implements IActiveDirectoryTokenGroupsRegistry, InitializingBean {

    /** logger for this class */
    private final Logger        log    = LoggerFactory.getLogger(AbstractADTokenGroupsRegistry.class);

    /** Base DN when looking for groups (relative to baseDN provided to contextSource) */
    protected String            baseDN = "";

    /** LDAP Connection Factory */
    protected ConnectionFactory ldapConnectionFactory;

    /** Current LDAP Connection */
    protected Connection        ldapConnection;

    @Override
    public void afterPropertiesSet() throws Exception {

        log.debug("Validating bean values");
        if (ldapConnectionFactory == null) {
            throw new BeanCreationException("LDAP connection cannot be null !");
        }
        log.debug("Validated with group search base : {}", baseDN);

    }

    /**
     * assures that a LDAP connection is opened or throws an IllegalStateException
     */
    protected void initLdap() {

        if (ldapConnection == null) {
            try {
                ldapConnection = ldapConnectionFactory.getConnection();
                ldapConnection.open();
            } catch (LdapException e) {
                throw new IllegalStateException("Error opening LDAP connection", e);
            }
        }
    }

    /**
     * Closes LDAP connection if opened
     */
    protected void cleanUpLdap() {

        if (ldapConnection != null) {
            ldapConnection.close();
            ldapConnection = null;
        }
    }

    // getters et setters

    @Override
    public void setBaseDN(final String baseDN) {

        this.baseDN = baseDN;

    }

    @Override
    public String getBaseDN() {

        return baseDN;
    }

    public ConnectionFactory getLdapConnectionFactory() {

        return ldapConnectionFactory;
    }

    public void setLdapConnectionFactory(ConnectionFactory ldapConnectionFactory) {

        this.ldapConnectionFactory = ldapConnectionFactory;
    }

}
