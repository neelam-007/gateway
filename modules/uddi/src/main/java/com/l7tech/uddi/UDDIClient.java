package com.l7tech.uddi;

import com.l7tech.util.Triple;

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
 * @author darmstrong
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
     * @param serviceName   The service name to match (pattern)
     * @param caseSensitive True for a case sensitive search
     * @param offset        The offset into the search results (use when paging)
     * @param maxRows       The maximum number of rows to return (can be used for paging)
     * @param getWsdlInfo   boolean. If true then WSDL info will be returned. Option for when the UDDI performance is very poor.
     * @return The collection of WsdlPortInfo. Only the businessServiceName is guaranteed to be non null and contain non empty strings.
     *         If getWsdlInfo is true, then the wsdlUrl property will be non null. Null if the Thread was interrupted
     * @throws UDDIException if an error occurs
     */
    Collection<WsdlPortInfo> listServiceWsdls(String serviceName, boolean caseSensitive, int offset, int maxRows, boolean getWsdlInfo) throws UDDIException;

    /**
     * List all wsdl:ports modelled by a BusinessService in UDDI
     * <p/>
     * Only valid bindingTemplates will be contained in the results. Non valid bindingTemplates are omitted
     *
     * Only soap bindings are returned
     *
     * @param serviceKey   String serviceKey of the BusinessService to list wsdl:port info for
     * @param getFirstOnly if true, the first found valid wsdl:port will be returned. Useful when only a WSDL URL is required
     * @return The collection of WsdlPortInfo. All properties are guaranteed to be non null. Can be empty.
     *         Null if the Thread was interrupted
     * @throws UDDIException if an error occurs
     */
    Collection<WsdlPortInfo> listWsdlPortsForService(String serviceKey, boolean getFirstOnly) throws UDDIException;

    /**
     * List BusinessEntities
     *
     * @param businessName The business name to match (pattern)
     * @param caseSensitive True for a case sensitive search
     * @param offset The offset into the search results (use when paging)
     * @param maxRows The maximum number of rows to return (can be used for paging)
     * @return The collection of keys/names/urls. Null if the search was cnacelled
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
     * Get a binding key for the given service.
     * <p/>
     * This should be used to retrieve a bindingKey for a subscription notification service published by a gateway.
     * The bindingKey returned will be the first found which contains an accessPoint element with a value who's protocol
     * element matches one in schemePreference.
     *
     * @param uddiServiceKey   The key for the service
     * @param schemePreference Schemes in preference order or null/empty for any
     * @return The binding key or null
     * @throws UDDIException if an error occurs
     */
    String getBindingKeyForService(String uddiServiceKey, Collection<String> schemePreference) throws UDDIException;

    /**
     * Publish policy to UDDI.
     *
     * @param tModelKey The tModel key (optional, null for initial publish)
     * @param name The name of the policy
     * @param description The description for the policy
     * @param url The url for the policy
     * @return The key for the newly published model
     * @throws UDDIException if an error occurs
     */
    String publishPolicy(String tModelKey, String name, String description, String url) throws UDDIException;

    /**
     * Get the name of a BusinessEntity from UDDI
     *
     * @param businessKey String key of the BusinessEntity in UDDI
     * @return String name of business, or null if not found
     * @throws UDDIException an problems searching UDDI
     */
    String getBusinessEntityName(String businessKey) throws UDDIException;

    /**
     * Publish a TModel to UDDI.
     *
     * @param tModelKey The tModel key (optional, null for initial publish)
     * @param name The name for the TModel
     * @param description The description for the TModel (optional)
     * @param keyedReferences The keyed references for the TModel
     * @return The tModel key
     * @throws UDDIException If an error occurs
     */
    public String publishTModel( String tModelKey,
                                 String name,
                                 String description,
                                 Collection<UDDIKeyedReference> keyedReferences ) throws UDDIException;

    /**
     * Delete a TModel from the UDDI Registry.
     *
     * @param tModelKey String tModelKey of the TModel to delete
     * @throws UDDIException if any problem during the find or the attempt to delete
     */
    void deleteTModel(final String tModelKey) throws UDDIException;

    /**
     * Delete all reference tModels. Delete should be done in one UDDI update
     * @param tModelKeys Set String of tModelKeys to delete
     * @throws UDDIException any problems deleting.
     */
    void deleteTModel(final Set<String> tModelKeys) throws UDDIException;

    /**
     * Delete a bindingTemplate. This should also attempt to delete its referenced tModels
     * 
     * @param bindingKey String bindingKey of the bindingTemplate to delete
     * @throws UDDIException any problems deleting from UDDI
     */
    void deleteBindingTemplate(final String bindingKey) throws UDDIException;

    /**
     * Delete all bindingTemplates with a key in bindingKeys.
     *
     * The delete should be done in one call to UDDI
     *
     * All keys MUST have the same serviceKey
     *
     * @param bindingKeys String bindingTemplate keys to delete
     * @throws UDDIException if a bindingTemplate cannot be deleted
     */
    void deleteBindingTemplateFromSingleService(final Set<String> bindingKeys) throws UDDIException;

    /**
     * Delete a single BusinessService from UDDI and all of it's referenced tModels
     * <p/>
     * This does a best effort delete of the business service and all its tmodels. First each referenced tModel is
     * retrieved, then the BusinessService is deleted. Regardless of success, each tModel is independently deleted.
     *
     * @param serviceKey String serviceKey of the BusinessService to delete
     * @throws UDDIException if there was any problems deleting the BusinessService or any tModels. Although each delete
     * is independently attempted, this exception will be thrown firstly if there was a problem deleting the business service
     * and secondly if there was any problem deleting any referenced tModel. The last exception which occured for deleting
     * tModels is the exception which will be thrown
     */
    void deleteBusinessServiceByKey(final String serviceKey) throws UDDIException;

    /**
     * Find all BusinessServices with the supplied keys
     *
     * @param serviceKeys Set String of serviceKeys to find. Cannot be null.
     * @param allowInvalidKeys if true then an invalid key will not caus a UDDIException to be thrown. This does not
     * stop other UDDIExceptions from being thrown.
     * @return List of BusinessServices which were found. Not guaranteed to be the same size as the serviceKeys set
     * @throws UDDIException if problem searching UDDI
     */
    List<UDDIBusinessService> getUDDIBusinessServices(final Set<String> serviceKeys, final boolean allowInvalidKeys) throws UDDIException;

    /**
     * Get the UDDIBusinessServices for the serviceKey
     *
     * Will not throw a UDDIException if the key is invalid
     * 
     * @param serviceKey String service key to search for
     * @return UDDIBusinessService if found. Null if not found
     * @throws UDDIException any problems search from UDDI
     */
    UDDIBusinessService getUDDIBusinessServiceInvalidKeyOk(final String serviceKey) throws UDDIException;

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
     * @param serviceKey The service to update
     * @param serviceUrl the service endpoint URL (null for no change)
     * @param policyKey The key for the local policy to reference
     * @param policyUrl The URL for the remote policy to reference
     * @param description The reference description
     * @param force Boolean.TRUE to replace an existing reference, Boolean.FALSE to add another reference null for exception on duplicate
     * @throws UDDIException if an error occurs
     * @throws UDDIExistingReferenceException if force is not set and there is an existing (local or remote) reference
     */
    void referencePolicy(String serviceKey, String serviceUrl, String policyKey, String policyUrl, String description, Boolean force) throws UDDIException;

    /**
     * Remove the association of a policy and a service.
     *
     * @param serviceKey The service to update.
     * @param policyKey The key for the local policy to reference (may be null)
     * @param policyUrl The URL for the remote policy to reference (may be null)
     * @returns true if a matching policy reference was found and removed
     * @throws UDDIException if an error occurs
     */
    boolean removePolicyReference(String serviceKey, String policyKey, String policyUrl) throws UDDIException;

    /**
     * Add a keyed reference to a business service.
     *
     * @param serviceKey The service to update (required)
     * @param keyedReferenceKey The TModel key for the reference (required)
     * @param keyedReferenceName The name for the reference (required)
     * @param keyedReferenceValue The value for the reference (required)
     * @throws UDDIException if an error occurs
     */
    void addKeyedReference(String serviceKey, String keyedReferenceKey, String keyedReferenceName, String keyedReferenceValue) throws UDDIException;

    /**
     * Remove a keyed reference from a business service.
     *
     * @param serviceKey The service to update (required)
     * @param keyedReferenceKey The TModel key for the reference (required)
     * @param keyedReferenceName The name for the reference (optional, null to match any)
     * @param keyedReferenceValue The value for the reference (optional, null to match any)
     * @returns true if a matching keyed reference was found and removed
     * @throws UDDIException if an error occurs
     */
    boolean removeKeyedReference(String serviceKey, String keyedReferenceKey, String keyedReferenceName, String keyedReferenceValue) throws UDDIException;

    /**
     * Get the operational information for a UDDI entity.
     *
     * @param entityKey The key for the entity
     * @return The UDDIOperationalInfo for the entity.
     */
    UDDIOperationalInfo getOperationalInfo(String entityKey) throws UDDIException;

    /**
     * Get the operational information for multiple UDDI entities.
     *
     * @param entityKey The key for the entity
     * @return The UDDIOperationalInfo for the entity.
     */
    Collection<UDDIOperationalInfo> getOperationalInfos(String... entityKey) throws UDDIException;

    /**
     * Subscribe for updates from UDDI.
     *
     * <p>For asynchronous notifications the notificationInterval and bindingKey
     * should be provided.</p>
     *
     * @param expiryTime The expiry time for the subscription
     * @param notificationInterval The notification interval in milliseconds
     * @param bindingKey The binding key for the notification mechanism
     * @return The subscription key
     * @throws UDDIException If an error occurs
     */
    String subscribe( long expiryTime, long notificationInterval, String bindingKey ) throws UDDIException;

    /**
     * Delete the subscription for the given key.
     *
     * @param subscriptionKey The subscription to delete.
     * @throws UDDIException If an error occurs
     */
    void deleteSubscription( String subscriptionKey ) throws UDDIException;

    /**
     * Poll subscriptions for updates.
     *
     * @param startTime The start date for the subscription poll
     * @param endTime The end date for the subscription poll
     * @param subscriptionKey The subscription key to check.
     * @return the subscription results (never null)
     * @throws UDDIException If an error occurs
     */
    UDDISubscriptionResults pollSubscription( long startTime, long endTime, String subscriptionKey ) throws UDDIException;

    /**
     * Represents a UDDI KeyedReference.
     */
    static final class UDDIKeyedReference extends Triple<String,String,String> {

        /**
         * Create a new keyed reference with the given values.
         *
         * @param key The key (required and must not be empty)
         * @param name The name (optional)
         * @param value The value (required)
         */
        public UDDIKeyedReference( final String key,
                                   final String name,
                                   final String value ) {
            super( key, name, value );
        }

        public String getKey() {
            return this.left;
        }

        public String getName() {
            return this.middle;
        }

        public String getValue() {
            return this.right;
        }
    }

    static final class UDDIKeyedReferenceGroup{
        private final String tModelKey;
        private final Collection<UDDIKeyedReference> keyedReferences;

        /**
         * @param tModelKey required
         * @param keyedReferences required, must not be empty
         */
        public UDDIKeyedReferenceGroup(final String tModelKey,
                                       final Collection<UDDIKeyedReference> keyedReferences) {
            if(tModelKey == null || tModelKey.trim().isEmpty()) throw new IllegalArgumentException("tModelKey must not be null or empty");
            if(keyedReferences == null || keyedReferences.isEmpty()) throw new IllegalArgumentException("keyedReferences must not be null or empty");
            
            this.tModelKey = tModelKey;
            this.keyedReferences = keyedReferences;
        }

        /**
         *
         * @return String tModelKey, never null or empty
         */
        public String getTModelKey() {
            return tModelKey;
        }

        /**
         *
         * @return Collection UDDIKeyedReference never null or empty
         */
        public Collection<UDDIKeyedReference> getKeyedReferences() {
            return keyedReferences;
        }

        @SuppressWarnings({ "RedundantIfStatement" })
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UDDIKeyedReferenceGroup that = (UDDIKeyedReferenceGroup) o;

            if (!keyedReferences.equals(that.keyedReferences)) return false;
            if (!tModelKey.equals(that.tModelKey)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = tModelKey.hashCode();
            result = 31 * result + keyedReferences.hashCode();
            return result;
        }
    }
}
