package net.archigny.utils.ad.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

import net.archigny.utils.ad.api.IActiveDirectoryTokenGroupsRegistry;

public abstract class AbstractADTokenGroupsRegistry implements IActiveDirectoryTokenGroupsRegistry, InitializingBean {

    /**
     * logger for this class
     */
    private static Logger LOG = LoggerFactory.getLogger(AbstractADTokenGroupsRegistry.class);

    /**
     * LDAP context source
     */
    protected LdapContextSource cs;

    /**
     * Base DN when looking for groups
     */
    protected String        groupBaseDN;

    protected LdapTemplate  ldapTemplate;

    @Override
    public void afterPropertiesSet() throws Exception {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Validating bean values");
        }
        if (cs == null) {
            throw new BeanCreationException("LDAP ContextSource cannot be null");
        }
        if (groupBaseDN == null) {
            throw new BeanCreationException("Group Base DN cannot be null");
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Validated with group search base : " + groupBaseDN);
        }
        ldapTemplate = new LdapTemplate(cs);
        ldapTemplate.setIgnorePartialResultException(true);
        ldapTemplate.afterPropertiesSet();

    }

    // getters et setters

    public void setContextSource(LdapContextSource cs) {

        this.cs = cs;

    }

    public LdapContextSource getContextSource() {

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

}
