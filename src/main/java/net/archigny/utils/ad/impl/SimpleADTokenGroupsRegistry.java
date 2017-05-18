package net.archigny.utils.ad.impl;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.InvalidNameException;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.LdapName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.support.LdapUtils;

/**
 * Simple implementation of Token Groups Registry with no caching : each query for a group translation will make a LDAP query to the
 * directory
 * 
 * @author Philippe Marasse
 * 
 */
public class SimpleADTokenGroupsRegistry extends AbstractADTokenGroupsRegistry {

    private final static Logger     log               = LoggerFactory.getLogger(SimpleADTokenGroupsRegistry.class);

    protected final static Pattern  QUERY_PLACEHOLDER = Pattern.compile("\\{0\\}");

    protected final static String   QUERY_SID         = "(objectSid={0})";

    protected final static String[] QUERY_ATTRS       = { "cn" };

    protected final String escapeOctetString(final byte[] octetString) {

        final StringBuilder sb = new StringBuilder(octetString.length * 3);
        for (byte b : octetString) {
            sb.append(String.format("\\%02x", b));
        }
        return sb.toString();
    }

    @Override
    public String getDnFromToken(final String tokenGroup) {

        return getDnFromToken(tokenGroup.getBytes());
    }

    @Override
    public String getDnFromToken(final byte[] tokenGroup) {

        final Matcher queryMatcher = QUERY_PLACEHOLDER.matcher(QUERY_SID);

        final String localFilter = queryMatcher.replaceAll(Matcher.quoteReplacement(escapeOctetString(tokenGroup)));
        if (log.isDebugEnabled()) {
            String sid = "";
            // sid String transform can raise an index out of bounds exception
            try {
                sid = LdapUtils.convertBinarySidToString(tokenGroup);
            } catch (Exception e) {

            }
            log.debug("Querying directory with filter : {} (resolve SID: {})", localFilter, sid);
        }

        final SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setReturningAttributes(QUERY_ATTRS);
        sc.setReturningObjFlag(true);

        try {
            @SuppressWarnings("unchecked")
            final List<String> groupDNs = ldapTemplate.search(baseDN, localFilter, sc, new DnFetcher());

            if (groupDNs.isEmpty()) {
                return null;
            }

            // A single value is expected...
            return groupDNs.get(0);

        } catch (NameNotFoundException e) {
            // in case of object not found, return null
            return null;
        }
    }

    /**
     * A simple ContextMapper to only fetch DN of result objects.
     * 
     * @author Philippe Marasse <philippe.marasse@laposte.net>
     */
    protected class DnFetcher implements ContextMapper {

        private final Logger log = LoggerFactory.getLogger(DnFetcher.class);
        
        @Override
        public Object mapFromContext(Object ctx) {

            final DirContextAdapter context = (DirContextAdapter) ctx;
            if (log.isDebugEnabled()) {
                log.debug("Attributes returned by context : {}", context.getAttributes().toString());
            }

            if (contextSourceBaseDN == null) {
                return context.getDn().toString();
            }

            final LdapName dn = (LdapName) contextSourceBaseDN.clone();

            if (dn.isEmpty()) {
                return context.getDn().toString();
            } else {
                try {
                    dn.addAll(context.getDn());
                    return dn.toString();
                } catch (InvalidNameException e) {
                    // Unexpected... in this case, return only context DN
                    return context.getDn().toString();
                }
            }

        }

    }

}
