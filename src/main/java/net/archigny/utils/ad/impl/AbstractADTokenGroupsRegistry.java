package net.archigny.utils.ad.impl;

import javax.annotation.PostConstruct;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapTemplate;

import net.archigny.utils.ad.api.IActiveDirectoryTokenGroupsRegistry;

/**
 * Abstract class implementing base functions.
 * 
 * @author Philippe MARASSE
 *
 */
public abstract class AbstractADTokenGroupsRegistry implements IActiveDirectoryTokenGroupsRegistry {

    /**
     * logger for this class
     */
    private final Logger    log    = LoggerFactory.getLogger(AbstractADTokenGroupsRegistry.class);

    /**
     * Base DN when looking for groups (relative to baseDN provided to contextSource)
     */
    protected String        baseDN = "";

    /**
     * baseDN provided to contextSource (as it is impossible to determine it by querying contextSource)
     */
    protected LdapName      contextSourceBaseDN;

    /**
     * LDAP template used to query the directory
     */
    protected LdapTemplate  ldapTemplate;

    @PostConstruct
    public void afterPropertiesSet() throws Exception {

        log.debug("Validating bean values");
        if (ldapTemplate == null) {
            throw new IllegalStateException("LDAP ContextSource cannot be null");
        }
        log.debug("Validated with group search base : {}", baseDN);
        ldapTemplate.setIgnorePartialResultException(true);
        ldapTemplate.afterPropertiesSet();

    }

    // getters et setters

    public void setContextSource(final ContextSource cs) {

        ldapTemplate = new LdapTemplate(cs);

    }

    @Override
    public void setBaseDN(final String baseDN) {

        this.baseDN = baseDN;

    }

    @Override
    public String getBaseDN() {

        return baseDN;
    }

    // Wrapper getter around LdapName
    public String getContextSourceBaseDN() {

        return (contextSourceBaseDN == null ? "" : contextSourceBaseDN.toString());
    }

    public void setContextSourceBaseDN(final String baseDN) {

        if (baseDN == null) {
            throw new IllegalArgumentException("baseDN cannot be null");
        }
        try {
            this.contextSourceBaseDN = new LdapName(baseDN);
        } catch (InvalidNameException e) {
            throw new IllegalArgumentException(e);
        }

    }

}
