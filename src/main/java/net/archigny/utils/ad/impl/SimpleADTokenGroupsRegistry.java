package net.archigny.utils.ad.impl;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.InvalidNameException;
import javax.naming.directory.SearchControls;

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
 * @author Philippe Marasse <philippe.marasse@laposte.net>
 * 
 */
public class SimpleADTokenGroupsRegistry extends AbstractADTokenGroupsRegistry {

    private final static Logger LOG = LoggerFactory.getLogger(SimpleADTokenGroupsRegistry.class);
    
    protected final static Pattern  QUERY_PLACEHOLDER = Pattern.compile("\\{0\\}");

    protected final static String   QUERY_SID         = "(objectSid={0})";

    protected final static String[] QUERY_ATTRS       = { "cn" };

    protected final String escapeOctetString(byte[] octetString) {

        StringBuilder sb = new StringBuilder(octetString.length * 3);
        for (byte b : octetString) {
            sb.append(String.format("\\%02x", b));
        }
        return sb.toString();
    }

    @Override
    public String getDnFromToken(String tokenGroup) {

        return getDnFromToken(tokenGroup.getBytes());
    }

    @Override
    public String getDnFromToken(byte[] tokenGroup) {

        Matcher queryMatcher = QUERY_PLACEHOLDER.matcher(QUERY_SID);

        String localFilter = queryMatcher.replaceAll(Matcher.quoteReplacement(escapeOctetString(tokenGroup)));
        if (LOG.isDebugEnabled()) {
            String sid = "";
            // sid String transform can raise an index out of bounds exception
            try {
                sid = LdapUtils.convertBinarySidToString(tokenGroup);
            } catch (Exception e) {

            }
            LOG.debug("Querying directory with filter : " + localFilter + " (resolve SID: " + sid + ")");
        }

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setReturningAttributes(QUERY_ATTRS);
        sc.setReturningObjFlag(true);

        try {
            @SuppressWarnings("unchecked")
            List<String> groupDNs = ldapTemplate.search(groupBaseDN, localFilter, sc, new DnFetcher());

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

        @Override
        public Object mapFromContext(Object ctx) {

            DirContextAdapter context = (DirContextAdapter) ctx;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Attributes returned by context : " + context.getAttributes().toString());
            }

            DistinguishedName dn = (DistinguishedName) cs.getBaseLdapPath().clone();

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
