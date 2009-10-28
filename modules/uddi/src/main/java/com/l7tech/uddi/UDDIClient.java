package com.l7tech.uddi;

import com.l7tech.common.uddi.guddiv3.BusinessService;
import com.l7tech.common.uddi.guddiv3.TModel;

import java.io.Closeable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

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
     * List BusinessEntities
     *
     * @param businessName The business name to match (pattern)
     * @param caseSensitive True for a case sensitive search
     * @param offset The offset into the search results (use when paging)
     * @param maxRows The maximum number of rows to return (can be used for paging)
     * @return The collection of keys/names/urls
     * @throws UDDIException if an error occurs
     */
    Collection<UDDINamedEntity> listBusinessEntities(String businessName, boolean caseSensitive, int offset, int maxRows) throws UDDIException;

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
     * Publish a Business Service to UDDI. The Business Service may already exist
     * <p/>
     * If the BusinessService does not already exist it will be created and the serviceKey will be assigned by
     * the UDDI registry and set on the BusinessService following this operation.
     *
     * @param businessService the Business Service to publish
     * @return true if the BusinessService was created, false otherwise
     * @throws UDDIException any problems searching / publishing UDDI
     */
    boolean publishBusinessService(final BusinessService businessService) throws UDDIException;

    /**
     * Publish a tModel to UDDI. May already exist.
     * <p/>
     * If the tModel does not already exist, it will be created and the tModelKey assigned to it by the UDDI
     * registry will be set on the tModel following this operation.
     *
     * If the tModel does exist the overviewDoc URL with use type = "wsdlInterface" will be compared. If they are the
     * same then the tModels match, otherwise they do not match and the tModel will be published
     *
     * When searching for the TModel in the UDDI Registry, all keyedReferences from the tModel's identifierBag
     * and categoryBag must be included in the search. The search should be exact, but case insensitive
     * @param tModelToPublish the tModel to publish.
     * @return true if the TModel was published, false otherwise i.e. it was found already in the UDDI Registry
     * @throws UDDIException any problems searching / publishing UDDI
     */
    boolean publishTModel(final TModel tModelToPublish) throws UDDIException;

    /**
     * Retrieve the tModel with the supplied key
     * @param tModelKey String tModelKey of the tModel to get
     * @return TModel of the supplied key. Null if not found
     * @throws UDDIException if any problem retireving the TModel from the UDDI registry
     */
    TModel getTModel(final String tModelKey) throws UDDIException;

    /**
     * Retrieve the BusinessService with the supplied key
     * @param serviceKey String serviceKey of the BusinessService to get
     * @return BusinessService of the supplied key. Null if not found
     * @throws UDDIException if any problem retireving the BusinessService from the UDDI registry
     */
    BusinessService getBusinessService(final String serviceKey) throws UDDIException;

    /**
     * Delete a TModel from the UDDI Registry.
     *
     * The delete should only be attempted when a search reveiles that no other Business Service references
     * this tModel.
     *
     * //todo this is inefficient - should allow a collection of tModels which only requires a single Business Service search
     * @param tModelKey String tModelKey of the TModel to delete
     * @throws UDDIException if any problem during the find or the attempt to delete
     */
    void deleteTModel(final String tModelKey) throws UDDIException;

    /**
     * Follows the same contract as deleteTModel(tModelKey)
     *
     * @param tModel TModel to delete, iff no BusinessService references it
     * @throws UDDIException if any problem during the find or the attempt to delete
     */
    public void deleteTModel(TModel tModel) throws UDDIException;

    /**
     * Delete all tModels which match the supplied TModel
     * @param prototype the tModel used to find tModels in the UDDI registry, which will then be deleted
     * @throws UDDIException if any problem during the attempt to find / delete 
     */
    void deleteMatchingTModels(final TModel prototype) throws UDDIException;

    /**
     * Delete a BusinessService from the UDDI Registry.
     *
     * Any dependent tModels representing wsdl:portType and wsdl:binding are not deleted. This should be done after
     * the call to deletedBusinessService, as it's possible that more than one Business Service, which originates
     * from the same WSDL, will reference the same tModel. Some UDDI Registries will throw an exception when
     * an attempt is made to delete a tModel which has been previously deleted (CentraSite Gov v7)
     *
     * @param serviceKey String serviceKey of the service to delete
     * @throws UDDIException if any problem during the attempt to delete
     * @return Set<String> contaning the tModelKeys of all referenced tModels
     */
    Set<String> deleteBusinessService(final String serviceKey) throws UDDIException;

    /**
     * Delete all supplied BusinessServices and safely delete all referenced tModels
     *
     * Calls deleteBusinessService(BusinessService) for each BusinessService in the collection, following that the
     * Set<String> of tModelKeys are deleted.
     *
     * @param businessServices Collection<BusinessService> all BusinessServices to delete. Required.
     * @throws UDDIException any problems searching / deleting
     */
    void deleteBusinessServices(final Collection<BusinessService> businessServices) throws UDDIException;

    /**
     * Same contract as deleteBusinessServices
     *
     * @param serviceKeys Collection<String> serviceKeys of BusinessServices to delete. Required.
     * @throws UDDIException any problems searching / deleting
     */
    void deleteBusinessServicesByKey(final Collection<String> serviceKeys) throws UDDIException;

    /**
     * Delete all BusinessServices from the UDDI Registry which contain the value of generalKeyword as the keyValue
     * for the keyedReference to the UDDI general keywords classification scheme, which have the specific Layer7 general
     * keyword name value for it's keyName value
     *
     * A wild card search is performed, due to initial test results with ActiveSOA. Any value supplied as the
     * generalKeyword will be surrounded by the '%' characters.
     *
     * @param generalKeyword String all BusinessServices which have this keyword will be deleted.
     */
    void deleteAllBusinessServicesForGatewayWsdl(final String generalKeyword) throws UDDIException;

    /**
     * Find all BusinessServices which contain the generalKeyword supplied.
     *
     * @param generalKeyword
     * @return
     * @throws UDDIException
     */
    List<BusinessService> findMatchingBusinessServices(final String generalKeyword) throws UDDIException;

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
