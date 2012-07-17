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
     * @param groupBaseDN
     */
    public void setGroupBaseDN(String groupBaseDN);

    /**
     * Getter for group searching base DN
     * 
     * @return actual base DN (group searching)
     */
    public String getGroupBaseDN();
    
    public String getDnFromToken(String tokenGroup);
    
    public String getDnFromToken(byte[] tokenGroup);
    
}
