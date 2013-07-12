package com.l7tech.gateway.common.service;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.admin.AliasAdmin;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.uddi.UDDINamedEntity;
import com.l7tech.uddi.UDDIRegistryInfo;
import com.l7tech.uddi.WsdlPortInfo;
import com.l7tech.util.CollectionUpdate;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;

/**
 * Provides a remote interface for publishing searching and updating published services
 * and service policies.
 *
 * @see PublishedService
 * @see ServiceHeader
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types=EntityType.SERVICE)
@Administrative
public interface ServiceAdmin extends AsyncAdminMethods, AliasAdmin<PublishedServiceAlias> {
    String ROLE_NAME_TYPE_SUFFIX = "Service";
    String ROLE_NAME_PATTERN = RbacAdmin.ROLE_NAME_PREFIX + " {0} " + ROLE_NAME_TYPE_SUFFIX + RbacAdmin.ROLE_NAME_OID_SUFFIX;
    enum DownloadDocumentType {WSDL, SCHEMA, XSL, UNKNOWN, MOD_ASS}

    /**
     * Retrieve all available published services.
     *
     * @return array of entity headers for all existing published services.  May be empty but never null.
     * @throws FindException   if there was a problem accessing the requested information.
     */
    @Secured(stereotype=MethodStereotype.FIND_HEADERS)
    @Transactional(readOnly=true)
    @Administrative(licensed=false, background = true)
    ServiceHeader[] findAllPublishedServices() throws FindException;

    /**
     * Overloaded findAllPublishedServices to all caller to explicitly choose whether aliases are returned in
     * the results or not. This is the only findAll method which will return aliases
     * @param includeAliases true if the returned array should contain aliases
     * @return ServiceHeader []. If includeAliases is true then this array can contain aliases. Call isAlias
     * on each ServiceHeader to determine if it is an alias.
     * @throws FindException
     */
    @Secured(stereotype=MethodStereotype.FIND_HEADERS)
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    ServiceHeader[] findAllPublishedServices(boolean includeAliases) throws FindException;

    /**
     * Retrieve a specified published service given its service ID.
     *
     * @param oid the unique identifier of the service
     * @return the requested {@link PublishedService}, or null if no service with that service ID was found
     * @throws FindException   if there was a problem accessing the requested information.
     */
    @Secured(stereotype=MethodStereotype.FIND_ENTITY)
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    PublishedService findServiceByID(String oid) throws FindException;

    /**
     * Get a wsdl document from a URL. The WSDL document will be resolved by the gateway so that the manager
     * can get at services that are 'hidden' behind the gateway.
     * This is meant to be used when a service is originally published.
     * <p/>
     * URL may be http or https with or without client auth, and may contain authentication information
     *
     * @param url the url that the gateway will use to resolve the wsdl document. this may contain
     *            userinfo type credentials
     * @return the contents resolved by this url
     * @throws java.io.IOException           thrown on I/O error accessing the WSDL url
     * @throws java.net.MalformedURLException thrown on malformed WSDL url
     */
    @Transactional(propagation = SUPPORTS)
    String resolveWsdlTarget(String url) throws IOException;

    /**
     * Get a document from a URL.
     * <p/>
     * URL may be http or https with or without client auth, and may contain authentication information
     *
     * @param url                    String URL to download the document from
     * @param maxSizeClusterProperty cluster property which will limit the maximum size of the document at the url. Required
     * @return the contents resolved by this url
     * @throws IOException           thrown on I/O error accessing the WSDL url
     * @throws java.net.MalformedURLException thrown on malformed WSDL url
     */
    @Transactional(propagation = SUPPORTS)
    String resolveUrlTarget(String url, String maxSizeClusterProperty) throws IOException;

    /**
     * Get a document from a URL.
     * <p/>
     * URL may be http or https with or without client auth, and may contain authentication information
     *
     * @param url                    String URL to download the document from
     * @param docType                DownloadDocumentType type of document, which will govern it's max download size
     * @return the contents resolved by this url
     * @throws IOException           thrown on I/O error accessing the WSDL url
     * @throws java.net.MalformedURLException thrown on malformed WSDL url
     */
    @Transactional(propagation = SUPPORTS)
    String resolveUrlTarget(String url, DownloadDocumentType docType) throws IOException;

    /**
     * Retrieve all available service documents for the given published service.
     *
     * @param serviceID The unique identifier of the service
     * @return The collection of ServiceDocuments
     * @throws FindException if there was a problem accessing the requested information.
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.SERVICE, stereotype=GET_PROPERTY_BY_ID)
    @Administrative(licensed=false)
    Collection<ServiceDocument> findServiceDocumentsByServiceID(String serviceID) throws FindException;

    /**
     * Retrieve a chunk of the available {@link PublishedService} headers  This is a version of
     * {@link #findAllPublishedServices()} that allows fetching the result in chunks, perhaps to reduce
     * latency.
     *
     * @return array of {@link ServiceHeader}s for requested subset of {@link PublishedService}s May be empty but never null.
     * @throws FindException   if there was a problem accessing the requested information.
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_HEADERS)
    @Administrative(licensed=false)
    ServiceHeader[] findAllPublishedServicesByOffset(int offset, int windowSize) throws FindException;


    /**
     * Retrieves changes in list of published services.
     *
     * @param oldVersionID  version ID from previous retrieval
     * @return collection changes; never null
     * @throws FindException if there was a problem accessing the requested information
     * @see CollectionUpdate
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_HEADERS)
    @Administrative(licensed=false, background = true)
    CollectionUpdate<ServiceHeader> getPublishedServicesUpdate(final int oldVersionID) throws FindException;

    /**
     * Store the specified new or existing published service. If the specified {@link PublishedService} contains a
     * unique object ID that already exists, this will replace the objects current configuration with the new configuration.
     * Otherwise, a new object will be created.
     * <p/>
     * Unless this is a new service being saved for the first time, the policy XML in the
     * service being saved will be ignored, and will not be updated.
     *
     * @param service the published service to create or update.  Must not be null.
     * @return the unique object ID that was updated or created.
     * @throws SaveException   if the requested information could not be saved
     * @throws UpdateException if the requested information could not be updated
     * @throws VersionException if the service version conflict is detected
     * @throws PolicyAssertionException if the server policy could not be instantiated for this policy
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    long savePublishedService(PublishedService service)
            throws UpdateException, SaveException, VersionException, PolicyAssertionException;

    /**
     * Store the specified new or existing published service. If the specified {@link PublishedService} contains a
     * unique object ID that already exists, this will replace the objects current configuration with the new configuration.
     * Otherwise, a new object will be created.
     * <p/>
     * Unless this is a new service being saved for the first time, the policy XML in the 
     * service being saved will be ignored, and will not be updated.
     *
     * @param service the published service to create or update.  Must not be null.
     * @param serviceDocuments the serviceDocuments to save. Null means no documents.
     * @return the unique object ID that was updated or created.
     * @throws SaveException   if the requested information could not be saved
     * @throws UpdateException if the requested information could not be updated
     * @throws VersionException if the service version conflict is detected
     * @throws PolicyAssertionException if the server policy could not be instantiated for this policy
     */
    @Secured(stereotype=SAVE_OR_UPDATE, relevantArg=0)
    long savePublishedServiceWithDocuments(PublishedService service, Collection<ServiceDocument> serviceDocuments)
            throws UpdateException, SaveException, VersionException, PolicyAssertionException;

    /**
     * Enable or disable debug tracing for the specified service.
     * <p/>
     * This currently requires that the caller have UPDATE permission on all PublishedService entities.
     *
     * @param serviceOid the OID of the published service whose tracing flag to turn on or off.  Must not be null.
     * @param tracingEnabled true to enable debug tracing for this published service; false to turn tracing off.
     * @throws UpdateException if the requested information could not be updated.
     */
    @Secured(stereotype=SET_PROPERTY_BY_UNIQUE_ATTRIBUTE)
    void setTracingEnabled(long serviceOid, boolean tracingEnabled) throws UpdateException;

    /**
     * Validate the service policy and return the policy validation result. Only the server side validation rules
     * are invoked here.
     *
     * @param policyXml the policy xml document
     * @param pvc required.  policy validation context containing:<ul>
     *                      <li>the type of policy this is
     *                      <li>the internal tag, for internal policies
     *                      <li> <code>true</code> if this policy is intended for SOAP services; <code>false</code> otherwise.
                            <li>the contents of the WSDL with which this policy is intended to be compatible, for soap policies
     * @return the job identifier of the validation job.  Call {@link #getJobStatus(com.l7tech.gateway.common.AsyncAdminMethods.JobId) getJobStatus} to poll for job completion
     *         and {@link #getJobResult(JobId)} to pick up the result in the form of a PolicyValidatorResult that contains
     *         policy validation warnings and errors
     */
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    JobId<PolicyValidatorResult> validatePolicy(String policyXml, PolicyValidationContext pvc);

    /**
     * Validate the service policy and return the policy validation result. Only the server side validation rules
     * are invoked here.
     *
     * @param policyXml the policy xml document
     * @param pvc required.  policy validation context containing:<ul>
     *                      <li>the type of policy this is
     *                      <li>the internal tag, for internal policies
     *                      <li> <code>true</code> if this policy is intended for SOAP services; <code>false</code> otherwise.
                            <li>the contents of the WSDL with which this policy is intended to be compatible, for soap policies
     * @param fragments the policy fragments that were included with this policy when it was imported
     * @return the job identifier of the validation job.  Call {@link #getJobStatus(com.l7tech.gateway.common.AsyncAdminMethods.JobId) getJobStatus} to poll for job completion
     *         and {@link #getJobResult(JobId)} to pick up the result in the form of a PolicyValidatorResult that contains
     *         policy validation warnings and errors
     */
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    JobId<PolicyValidatorResult> validatePolicy(String policyXml, PolicyValidationContext pvc, HashMap<String, Policy> fragments);

    /**
     * Asynchronously find all URLs of the WSDLs from UDDI Registry given the service name pattern.
     *
     * @param registryOid   long oid of the UDDIRegistry to search
     * @param namePattern   The string of the service name (wildcard % is supported)
     * @param caseSensitive True if case sensitive, false otherwise.
     * @param getWsdlURL    boolean if true then the WSDL URL is inculded in each returned WsdlPortInfo. Setting to false
     *                      will dramatically increase search performance
     * @return the job identifier of the uddi search job.
     *         Call {@link #getJobStatus(com.l7tech.gateway.common.AsyncAdminMethods.JobId) getJobStatus} to poll for job completion
     *         and {@link #getJobResult(JobId)} to pick up the result in the form of a WsdlPortInfo[] that contains
     *         the search results for all services whose name matches the namePattern
     */
    @Transactional(readOnly = true)
    @Secured(types = EntityType.UDDI_REGISTRY, stereotype = GET_PROPERTY_BY_ID, relevantArg = 0)
    JobId<WsdlPortInfo[]>
    findWsdlInfosFromUDDIRegistry(long registryOid, String namePattern, boolean caseSensitive, boolean getWsdlURL);

    /**
     * Asynchronously get all bindingTemplates for a BusinessService as an array of WsdlPortInfo's
     *
     * Only valid bindingTemplates from the BusinessService will be included in the results. For each valid
     * bindingTemplate there will be one WsdlPortInfo in the returned array.
     *
     * Valid bindingTemplates is defined according to the technical note on using a WSDL in UDDI
     *
     * Only soap bindings are retrieved
     *
     * @param registryOid long oid of the UDDIRegistry to search
     * @param serviceKey String serviceKey of the BusinessService to retrieve wsdl:port infomration for
     * @param getFirstOnly if true, the first value result will be returned. Useful when only a WSDL URL is required.
     * @return WsdlPortInfo array.
     * @throws FindException if there was a problem accessing the requested information.
     */
    @Transactional(readOnly = true)
    @Secured(types=EntityType.UDDI_REGISTRY, stereotype=GET_PROPERTY_BY_ID, relevantArg = 0)
    JobId<WsdlPortInfo[]>
    findWsdlInfosForSingleBusinessService(long registryOid, String serviceKey, boolean getFirstOnly) throws FindException;

    /**
     * Asynchronously find all Businesses from the UDDI Registry given the service name pattern.
     *
     * @param registryOid   long oid of the UDDIRegistry to search
     * @param namePattern   The string of the business name (wildcard % is supported)
     * @param caseSensitive True if case sensitive, false otherwise.
     * @return the job identifier of the uddi search job.
     *         Call {@link #getJobStatus(com.l7tech.gateway.common.AsyncAdminMethods.JobId) getJobStatus} to poll for job completion
     *         and {@link #getJobResult(JobId)} to pick up the result in the form of a UDDINamedEntity[] that contains
     *         the search results, for all business whose name matches the namePattern
     */
    @Transactional(readOnly = true)
    @Secured(types=EntityType.UDDI_REGISTRY, stereotype=GET_PROPERTY_BY_ID, relevantArg = 0)
    JobId<UDDINamedEntity[]> findBusinessesFromUDDIRegistry(long registryOid, String namePattern, boolean caseSensitive);

    /**
     * Find all WS-Policy attachments from the UDDI registry that match the given name pattern.
     *
     * @param registryOid Identifier of the UDDIRegistry to search
     * @param namePattern The string of the policy tModel name
     * @return A list of UDDINamedEntities of policies whose name matches the namePattern.
     * @throws FindException if there was a problem accessing the requested information.
     */
    @Transactional(readOnly = true)
    @Secured(types=EntityType.UDDI_REGISTRY, stereotype=GET_PROPERTY_BY_ID, relevantArg = 0)
    UDDINamedEntity[] findPoliciesFromUDDIRegistry(long registryOid, String namePattern) throws FindException;

    /**
     * Finds the {@link SampleMessage} instance with the specified GOID, or null if it does not exist.
     * @return the {@link SampleMessage} instance with the specified GOID.  May be null if not present.
     */
    @Secured(types=EntityType.SAMPLE_MESSAGE, stereotype=FIND_ENTITY)
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    SampleMessage findSampleMessageById(Goid goid) throws FindException;

    /**
     * Finds any {@link EntityHeader}s belonging to the {@link PublishedService}
     * with the specified OID and (optional) operation name.
     * @param serviceOid the OID of the {@link PublishedService} to which the SampleMessage belongs. Pass -1 for all services.
     * @param operationName the name of the operation for which the SampleMessage was saved. Pass null for all operations, or "" for messages that are not categorized by operation name.
     * @return an array of {@link EntityHeader}s. May be empty, but never null.
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.SAMPLE_MESSAGE, stereotype=FIND_HEADERS)
    @Administrative(licensed=false)
    EntityHeader[] findSampleMessageHeaders(long serviceOid, String operationName) throws FindException;

    @Secured(types=EntityType.SAMPLE_MESSAGE, stereotype=SAVE_OR_UPDATE)
    Goid saveSampleMessage(SampleMessage sm) throws SaveException;

    @Secured(types=EntityType.SAMPLE_MESSAGE, stereotype=DELETE_ENTITY)
    void deleteSampleMessage(SampleMessage message) throws DeleteException;

    /**
     * Delete a {@link com.l7tech.gateway.common.service.PublishedService} by its unique identifier.

     * @param oid the unique identifier of the {@link com.l7tech.gateway.common.service.PublishedService} to delete.
     * @throws com.l7tech.objectmodel.DeleteException if the requested information could not be deleted
     */
    @Secured(stereotype= DELETE_BY_ID)
    void deletePublishedService(String oid) throws DeleteException;

    /**
     * @param serviceoid id of the service to publish on the systinet (or other UDDI) registry
     * @param fullPolicyURL boolean if true, the String returned is configured to get the full Layer7 policy (Requires IP of who ever uses this information to be white listed for it to work)
     * @return String the external URL from where the policy for the service can be downloaded
     * @throws com.l7tech.objectmodel.FindException if the hostname cannot be determined
     */
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    String getPolicyURL(String serviceoid, boolean fullPolicyURL) throws FindException;

    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    String getConsumptionURL(String serviceoid ) throws FindException;

    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    Collection<UDDIRegistryInfo> getUDDIRegistryInfo();

    @Transactional(readOnly=true)
    @Secured(types=EntityType.SERVICE_TEMPLATE, stereotype= MethodStereotype.FIND_ENTITIES)
    Set<ServiceTemplate> findAllTemplates();

    /**
     * According to the given WS-Trust namespace, create a corresponding service template of Security Token Service.
     *
     * @param wsTrustNamespace: the namespace of a chosen WS-Trust version.
     * @return a service template associated with the corresponding WS-Trust version.
     */
    @Secured(types=EntityType.SERVICE_TEMPLATE, stereotype= MethodStereotype.FIND_ENTITY)
    ServiceTemplate createSecurityTokenServiceTemplate(String wsTrustNamespace);

    @Transactional(readOnly=true)
    @Secured(stereotype=MethodStereotype.GET_PROPERTY_OF_ENTITY,relevantArg=0)
    ResolutionReport generateResolutionReport( PublishedService service, Collection<ServiceDocument> serviceDocuments ) throws FindException;

    @Transactional(readOnly=true)
    @Secured(stereotype=MethodStereotype.SAVE_OR_UPDATE, relevantArg=0)
    ResolutionReport generateResolutionReportForNewService( PublishedService service, Collection<ServiceDocument> serviceDocuments ) throws FindException;

    @Transactional(readOnly = true)
    @Secured(stereotype = FIND_ENTITY, types = EntityType.SERVICE)
    PublishedService findByAlias(final long aliasOid) throws FindException;

    class ResolutionReport implements Serializable {
        private final boolean resolvesByPath;
        private final ConflictInfo[] conflicts;

        public ResolutionReport( final boolean resolvesByPath,
                                 final ConflictInfo[] conflicts ) {
            this.resolvesByPath = resolvesByPath;
            this.conflicts = conflicts;
        }

        public ConflictInfo[] getConflicts() {
            return conflicts;
        }

        public boolean isResolvesByPath() {
            return resolvesByPath;
        }

        public boolean isSuccess() {
            return resolvesByPath && (conflicts==null || conflicts.length==0);
        }

        public String toString() {
            final StringBuilder builder = new StringBuilder();

            builder.append( "Resolves using FTP/HTTP: ");
            builder.append( resolvesByPath ? "Yes" : "No");
            if ( conflicts!=null && conflicts.length > 0 ) {
                builder.append( "\n\n" );
                builder.append( "Conflicting services:" );

                List<ConflictInfo> conflictList = Arrays.asList( conflicts );
                Collections.sort( conflictList );
                long lastServiceId = -1;
                for ( final ConflictInfo conflict : conflicts ) {
                    if ( conflict.getServiceOid() != lastServiceId ) {
                        lastServiceId = conflict.getServiceOid();
                        builder.append( "\n  Service: " );
                        builder.append( conflict.getServiceDisplayName() );
                    }

                    builder.append( "\n    NS: " ).append( conflict.getSoapPayloadNamespace() );
                    builder.append( ", Action: " ).append( conflict.getSoapAction() );
                }
            }

            return builder.toString();
        }
    }

    class ConflictInfo implements Serializable, Comparable<ConflictInfo> {
        private final long serviceOid;
        private final String serviceName;
        private final String serviceDisplayName;
        private final String soapAction;
        private final String soapPayloadNamespace;
        private final String path;

        public ConflictInfo( final String path,
                             final String serviceDisplayName,
                             final String serviceName,
                             final long serviceOid,
                             final String soapAction,
                             final String soapPayloadNamespace ) {
            this.path = path;
            this.serviceDisplayName = serviceDisplayName;
            this.serviceName = serviceName;
            this.serviceOid = serviceOid;
            this.soapAction = soapAction;
            this.soapPayloadNamespace = soapPayloadNamespace;
        }

        public String getPath() {
            return path == null ?  "" : path;
        }

        public String getServiceDisplayName() {
            return serviceDisplayName;
        }

        public String getServiceName() {
            return serviceName;
        }

        public long getServiceOid() {
            return serviceOid;
        }

        public String getSoapAction() {
            return soapAction == null ? "" : soapAction;
        }

        public String getSoapPayloadNamespace() {
            return soapPayloadNamespace == null ? "" : soapPayloadNamespace;
        }

        private int compareMaybeNull( final String text1,
                                      final String text2 ) {
            if ( text1 == null || text2 == null ) {
                if ( text1 != null ) {
                    return 1;
                } else if ( text2 != null ) {
                    return -1;                    
                } else {
                    return 0;
                }
            } else {
                return text1.compareTo( text2 );
            }
        }

        @Override
        public int compareTo( final ConflictInfo other ) {
            int value = Long.valueOf( serviceOid ).compareTo( other.getServiceOid() );

            if ( value == 0 ) {
                value = compareMaybeNull( path, other.getPath() );
            }
            if ( value == 0 ) {
                value = compareMaybeNull( soapPayloadNamespace, other.getSoapPayloadNamespace() );
            }
            if ( value == 0 ) {
                value = compareMaybeNull( soapAction, other.getSoapAction() );
            }

            return value;
        }

        @SuppressWarnings({ "RedundantIfStatement" })
        @Override
        public boolean equals( final Object o ) {
            if ( this == o ) return true;
            if ( o == null || getClass() != o.getClass() ) return false;

            final ConflictInfo that = (ConflictInfo) o;

            if ( serviceOid != that.serviceOid ) return false;
            if ( path != null ? !path.equals( that.path ) : that.path != null ) return false;
            if ( serviceDisplayName != null ? !serviceDisplayName.equals( that.serviceDisplayName ) : that.serviceDisplayName != null )
                return false;
            if ( serviceName != null ? !serviceName.equals( that.serviceName ) : that.serviceName != null )
                return false;
            if ( soapAction != null ? !soapAction.equals( that.soapAction ) : that.soapAction != null ) return false;
            if ( soapPayloadNamespace != null ? !soapPayloadNamespace.equals( that.soapPayloadNamespace ) : that.soapPayloadNamespace != null )
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (serviceOid ^ (serviceOid >>> 32));
            result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
            result = 31 * result + (serviceDisplayName != null ? serviceDisplayName.hashCode() : 0);
            result = 31 * result + (soapAction != null ? soapAction.hashCode() : 0);
            result = 31 * result + (soapPayloadNamespace != null ? soapPayloadNamespace.hashCode() : 0);
            result = 31 * result + (path != null ? path.hashCode() : 0);
            return result;
        }
    }
}