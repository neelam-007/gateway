package com.l7tech.uddi;

import java.util.Collection;

import com.l7tech.util.Closeable;

/**
 * This provides a facade around interesting aspects of the following UDDI apis:
 *
 * <ul>
 * <li>Security API</li>
 * <li>Inquiry API</li>
 * <li>Publishing API</li>
 * </ul>
 *
 * @author Steve Jones
 */
public interface UDDIClient extends Closeable {

    /**
     * Authenticate the credentials (if any)
     *
     * <p>This will get a token for the UDDI registry if any credentials are
     * available.</p>
     *
     * <p>If credentials are available and this method succeeds then the
     * credentials are valid.</p>
     *
      * @throws UDDIException if an error occurs
     */
    void authenticate() throws UDDIException;

    /**
     * List services in registry.
     *
     * <p>The service name pattern uses UDDI approximate match syntax:</p>
     *
     * <ul>
     * <li>% - matches any number of characters</li>
     * <li>_ - matches any single character</li>
     * </ul>
     *
     * @param serviceName The service name to match (pattern)
     * @param caseSensitive True for a case sensitive search
     * @param offset The offset into the search results (use when paging)
     * @param maxRows The maximum number of rows to return (can be used for paging)
     * @return The collection of keys/names
     * @throws UDDIException if an error occurs
     */
    Collection<UDDINamedEntity> listServices(String serviceName, boolean caseSensitive, int offset, int maxRows) throws UDDIException;

    /**
     * List endpoints by service in registry.
     *
     * @param serviceName The service name to match (pattern)
     * @param caseSensitive True for a case sensitive search
     * @param offset The offset into the search results (use when paging)
     * @param maxRows The maximum number of rows to return (can be used for paging)
     * @return The collection of keys/names
     * @throws UDDIException if an error occurs
     */
    Collection<UDDINamedEntity> listEndpoints(String serviceName, boolean caseSensitive, int offset, int maxRows) throws UDDIException;

    /**
     * List businesses in a registry
     *
     * @param orgName The organization name to match (pattern)
     * @param caseSensitive True for a case sensitive search
     * @param offset The offset into the search results (use when paging)
     * @param maxRows The maximum number of rows to return (can be used for paging)
     * @return The collection of keys/names
     * @throws UDDIException if an error occurs
     */
    Collection<UDDINamedEntity> listOrganizations(String orgName, boolean caseSensitive, int offset, int maxRows) throws UDDIException;

    /**
     * List services with their WSDL URLs. 
     *
     * @param serviceName The service name to match (pattern)
     * @param caseSensitive True for a case sensitive search
     * @param offset The offset into the search results (use when paging)
     * @param maxRows The maximum number of rows to return (can be used for paging)
     * @return The collection of keys/names/urls
     * @throws UDDIException if an error occurs
     */
    Collection<UDDINamedEntity> listServiceWsdls(String serviceName, boolean caseSensitive, int offset, int maxRows) throws UDDIException;

    /**
     * List policies in registry
     *
     * @param policyName The policy name to match (pattern)
     * @param policyUrl The url to match
     * @return The collection of keys/names
     * @throws UDDIException if an error occurs
     */
    Collection<UDDINamedEntity> listPolicies(String policyName, String policyUrl) throws UDDIException;

    /**
     * Call after a listX method to see if more results are available
     *
     * @return true if more results are available
     */
    boolean listMoreAvailable();

    /**
     * Publish policy to UDDI.
     *
     * @param name The name of the policy
     * @param description The description for the policy
     * @param url The url for the policy
     * @return The key for the newly published model
     * @throws UDDIException if an error occurs
     */
    String publishPolicy(String name, String description, String url) throws UDDIException;

    /**
     * Get the URL for the referenced policy.
     *
     * @param policyKey the key for the policy model
     * @return The URL for the policy (or null if not found)
     * @throws UDDIException if an error occurs
     */
    String getPolicyUrl(String policyKey) throws UDDIException;

    /**
     * List the policy URLs for an organization.
     *
     * @param key the key for the organization
     * @return a collection of strings (may be empty)
     * @throws UDDIException if an error occurs
     */
    Collection<String> listPolicyUrlsByOrganization(String key) throws UDDIException;

    /**
     * List the policy URLs for a service.
     *
     * @param key the key for the service
     * @return a collection of strings (may be empty)
     * @throws UDDIException if an error occurs
     */
    Collection<String> listPolicyUrlsByService(String key) throws UDDIException;

    /**
     * List the policy URLs for an endpoint.
     *
     * @param key the key for the binding
     * @return a collection of strings (may be empty)
     * @throws UDDIException if an error occurs
     */
    Collection<String> listPolicyUrlsByEndpoint(String key) throws UDDIException;

    /**
     * Associate a policy with a service.
     *
     * <p>You can specify a key and url for local/remote policy but usually you
     * would only use one of these.</p>
     *
     * @param serviceKey The service to udpate
     * @param serviceUrl the service endpoint URL (null for no change)
     * @param policyKey The key for the local policy to reference
     * @param policyUrl The URL for the remote policy to reference
     * @param description The reference description
     * @param force Boolean.TRUE to replace an existing reference, Boolean.FALSE to add another reference null for exception on duplicate
     * @throws UDDIException if an error occurs
     * @throws UDDIExistingReferenceException if force is not set and there is an existing (local or remote) reference
     */
    void referencePolicy(String serviceKey, String serviceUrl, String policyKey, String policyUrl, String description, Boolean force) throws UDDIException;
}
