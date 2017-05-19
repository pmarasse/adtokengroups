package net.archigny.utils.ad.impl;

import java.util.regex.Pattern;

import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchOperation;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchResult;
import org.ldaptive.ad.SecurityIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple implementation of Token Groups Registry with no caching : each query for a group translation will make a LDAP query to the
 * directory
 * 
 * @author Philippe Marasse
 * 
 */
public class SimpleADTokenGroupsRegistry extends AbstractADTokenGroupsRegistry {

    private final static Logger     log               = LoggerFactory.getLogger(SimpleADTokenGroupsRegistry.class);

    /** Pattern used to replace placeholder */
    protected final static Pattern  QUERY_PLACEHOLDER = Pattern.compile("\\{0\\}");

    /** LDAP Query for SID */
    protected final static String   QUERY_SID         = "(objectSid={sid})";

    /** LDAP Attribute to retrieve */
    protected final static String[] QUERY_ATTRS       = { "cn" };

    @Override
    public String getDnFromToken(final String tokenGroup) {

        return getDnFromToken(tokenGroup.getBytes());
    }

    @Override
    public String getDnFromToken(final byte[] tokenGroup) {

        final SearchFilter localFilter = new SearchFilter(QUERY_SID);
        localFilter.setParameter("sid", tokenGroup);

        if (log.isDebugEnabled()) {
            String sid = "";
            // sid String transform can raise an index out of bounds exception
            try {
                sid = SecurityIdentifier.toString(tokenGroup);
            } catch (Exception e) {

            }
            log.debug("Querying directory with filter : {} (resolve SID: {})", localFilter.toString(), sid);
        }

        initLdap();
        SearchOperation search = new SearchOperation(ldapConnection);

        try {
            SearchResult result = search.execute(new SearchRequest(baseDN, localFilter, QUERY_ATTRS)).getResult();
            LdapEntry entry = result.getEntry();

            if (entry == null) {
                return null;
            }

            // A single value is expected...
            return entry.getDn();

        } catch (LdapException e) {
            log.error("LDAP Exception raised : {}", e.getMessage());
            // in case of object not found, return null
            return null;
        }
    }

}
