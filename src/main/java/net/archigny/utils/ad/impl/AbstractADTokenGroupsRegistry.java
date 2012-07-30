package net.archigny.utils.ad.impl;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapTemplate;

import net.archigny.utils.ad.api.IActiveDirectoryTokenGroupsRegistry;

public abstract class AbstractADTokenGroupsRegistry implements IActiveDirectoryTokenGroupsRegistry, InitializingBean {

    /**
     * logger for this class
     */
    private final Logger log = LoggerFactory.getLogger(AbstractADTokenGroupsRegistry.class);

    /**
     * LDAP context source
     */
    protected ContextSource cs;

    /**
     * Base DN when looking for groups (relative to baseDN provided to contextSource)
     */
    protected String        groupBaseDN = "";

    /**
     * baseDN provided to contextSource (as it is impossible to determine it by querying contextSource)
     */
    protected LdapName 		contextSourceBaseDN;
    
    protected LdapTemplate  ldapTemplate;

    @Override
    public void afterPropertiesSet() throws Exception {

        if (log.isDebugEnabled()) {
            log.debug("Validating bean values");
        }
        if (cs == null) {
            throw new BeanCreationException("LDAP ContextSource cannot be null");
        }
        if (log.isDebugEnabled()) {
            log.debug("Validated with group search base : " + groupBaseDN);
        }
        ldapTemplate = new LdapTemplate(cs);
        ldapTemplate.setIgnorePartialResultException(true);
        ldapTemplate.afterPropertiesSet();
        
    }

    // getters et setters

    public void setContextSource(ContextSource cs) {

        this.cs = cs;

    }

    public ContextSource getContextSource() {

        return this.cs;
    }

    @Override
    public void setGroupBaseDN(String groupBaseDN) {

        this.groupBaseDN = groupBaseDN;

    }

    @Override
    public String getGroupBaseDN() {

        return groupBaseDN;
    }
    
    // Wrapper getter around LdapName
    public String getContextSourceBaseDN() {

        return (contextSourceBaseDN == null ? "" : contextSourceBaseDN.toString());
    }

    public void setContextSourceBaseDN(String baseDN) {

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
