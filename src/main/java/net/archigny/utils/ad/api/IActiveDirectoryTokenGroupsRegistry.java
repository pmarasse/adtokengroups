package net.archigny.utils.ad.api;

/**
 * Registry for Active Directory Token Groups
 * 
 * @author philippe
 */
public interface IActiveDirectoryTokenGroupsRegistry {

    /**
     * Set the group base DN for querying groups eg: cn=Groups,dc=mydomain,dc=net
     * 
     * @param baseDN
     */
    public void setBaseDN(final String baseDN);

    /**
     * Getter for group searching base DN
     * 
     * @return actual base DN (group searching)
     */
    public String getBaseDN();
    
    /**
     * Resolve a string represented binary SID to its Distinguished Name
     *
     * @param tokenGroup String representation that will be converted to byte[]
     * @return Distinguished name of token or null if non existent
     */
    public String getDnFromToken(final String tokenGroup);
    
    /**
     * Resolve a binary SID to its Distinguished Name
     *
     * @param tokenGroup byte array representing the SID in binary format (fetched from AD)
     * @return Distinguished name of token or null if non existent
     */
    public String getDnFromToken(final byte[] tokenGroup);
    
}
