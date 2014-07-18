package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.PolicyImportContext;
import com.l7tech.gateway.api.impl.PolicyValidationContext;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.PolicyReference;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.ext.entity.CustomEntitySerializer;
import com.l7tech.policy.assertion.ext.store.KeyValueStore;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreException;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreListener;
import com.l7tech.policy.exporter.*;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.globalresources.HttpConfigurationManager;
import com.l7tech.server.globalresources.ResourceEntryManager;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.policy.CustomKeyValueStoreManager;
import com.l7tech.server.policy.PolicyAssertionRbacChecker;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.export.PolicyExporterImporterManager;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.siteminder.SiteMinderConfigurationManager;
import com.l7tech.server.transport.SsgActiveConnectorManager;
import com.l7tech.server.transport.jms.JmsConnectionManager;
import com.l7tech.server.transport.jms.JmsEndpointManager;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.*;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.xml.soap.SoapVersion;
import com.sun.istack.Nullable;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

import javax.wsdl.WSDLException;
import java.io.IOException;
import java.security.KeyStoreException;
import java.util.*;
import java.util.logging.Logger;

import static com.l7tech.util.Option.optional;

/**
 * Helper class for implementing policy import/export and validation.
 */
public class PolicyHelper {

    //- PUBLIC

    public PolicyHelper( final AssertionLicense licenseManager,
                         final PolicyValidator policyValidator,
                         final WspReader wspReader,
                         final GatewayExternalReferenceFinder referenceFinder,
                         final EntityResolver entityResolver,
                         final PolicyAssertionRbacChecker policyAssertionRbacChecker) {
        this.licenseManager = licenseManager;
        this.policyValidator = policyValidator;
        this.wspReader = wspReader;
        this.referenceFinder = referenceFinder;
        this.entityResolver = entityResolver;
        this.policyAssertionRbacChecker = policyAssertionRbacChecker;
    }

    /**
     * Export the given policy.
     *
     * @param policy The policy to export.
     * @return The policy export result.
     */
    public PolicyExportResult exportPolicy( final Policy policy ) {
        try {
            final PolicyExporter exporter = new PolicyExporter( referenceFinder, entityResolver );
            final Document exportDoc = exporter.exportToDocument( policy.getAssertion(), referenceFinder.findAllExternalReferenceFactories() );
            final PolicyExportResult policyExportResult = ManagedObjectFactory.createPolicyExportResult();
            final Resource resource = ManagedObjectFactory.createResource();
            resource.setType( ResourceHelper.POLICY_EXPORT_TYPE );
            resource.setContent( XmlUtil.nodeToFormattedString(exportDoc) );
            policyExportResult.setResource( resource );

            return policyExportResult;
        } catch ( SAXException e ) {
            throw new ResourceFactory.ResourceAccessException("Error creating policy export '"+ExceptionUtils.getMessage( e )+"'.", e);
        } catch ( IOException e ) {
            throw new ResourceFactory.ResourceAccessException("Error creating policy export '"+ExceptionUtils.getMessage( e )+"'.", e);
        } catch (FindException e) {
            throw new ResourceFactory.ResourceAccessException("Error creating policy export '"+ExceptionUtils.getMessage( e )+"'.", e);
        }
    }

    /**
     * Import the given policy data to the supplied policy.
     *
     * @param policy The target policy
     * @param policyImportContext The policy import request context
     * @return The result of the policy import
     * @throws ResourceFactory.InvalidResourceException If the import request is not valid.
     */
    public PolicyImportResult importPolicy( final Policy policy,
                                            final PolicyImportContext policyImportContext ) throws ResourceFactory.InvalidResourceException {
        if ( policyImportContext == null )
            throw new ResourceFactory.InvalidResourceException( ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES, "missing import context" );
        if ( policyImportContext.getResource() == null )
            throw new ResourceFactory.InvalidResourceException( ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES, "missing policy export resource" );
        final Resource resource = policyImportContext.getResource();
        if ( !(ResourceHelper.POLICY_EXPORT_TYPE.equals( resource.getType() ) || ResourceHelper.ENCASS_EXPORT_TYPE.equals( resource.getType() ))) {
            throw new ResourceFactory.InvalidResourceException( ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "unexpected resource type" );
        }
        final String exportXml = resource.getContent();
        if ( exportXml == null )
            throw new ResourceFactory.InvalidResourceException( ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES, "missing policy export" );

        try {
            final Document exportDoc = XmlUtil.parse( exportXml );
            final List<PolicyImportResult.ImportedPolicyReference> references = new ArrayList<PolicyImportResult.ImportedPolicyReference>();
            final List<String> warnings = new ArrayList<String>();
            final List<Triple<String,String,String>> conflictingPolicies = new ArrayList<Triple<String,String,String>>();
            final List<ExternalReference> unresolvedReferences = new ArrayList<ExternalReference>();

            final PolicyImporter.PolicyImporterResult result =
                    PolicyImporter.importPolicy(
                            policy,
                            exportDoc,
                            referenceFinder.findAllExternalReferenceFactories(),
                            wspReader,
                            referenceFinder,
                            entityResolver,
                            buildErrorListener( warnings ),
                            buildPolicyImportAdvisor( policyImportContext, references, conflictingPolicies, unresolvedReferences ) );

            final PolicyImportResult pir = ManagedObjectFactory.createPolicyImportResult();

            if ( !conflictingPolicies.isEmpty() || !unresolvedReferences.isEmpty() ) {
                final String errorDetail = buildErrorDetail( conflictingPolicies, unresolvedReferences );
                throw new ResourceFactory.InvalidResourceException( ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, errorDetail );
            } else {
                policy.setXml( WspWriter.getPolicyXml(result.assertion) );

                for ( final Policy includedPolicy : result.policyFragments.values() ) {
                    if ( referenceFinder.policyManager.findByGuid( includedPolicy.getGuid() ) == null ) {
                        if ( includedPolicy.getType() != PolicyType.INCLUDE_FRAGMENT  &&
                             includedPolicy.getType() != PolicyType.INTERNAL) {
                            throw new ResourceFactory.ResourceAccessException("Policy include fragment is not of expected type.");
                        }
                        includedPolicy.setGoid( Policy.DEFAULT_GOID );
                        includedPolicy.setFolder( null );

                        if ( referenceFinder.getUser()==null ||
                             !referenceFinder.rbacServices.isPermittedForEntity(referenceFinder.getUser(), includedPolicy, OperationType.CREATE, null ) )
                            throw new PermissionDeniedException( OperationType.CREATE, EntityType.POLICY );
                        Goid goid = referenceFinder.policyManager.save( includedPolicy );

                        PolicyImportResult.ImportedPolicyReference reference = ManagedObjectFactory.createImportedPolicyReference();
                        reference.setType( PolicyImportResult.ImportedPolicyReferenceType.CREATED );
                        reference.setReferenceType( "com.l7tech.console.policy.exporter.IncludedPolicyReference" );
                        reference.setReferenceId( includedPolicy.getGuid() );
                        reference.setId( Goid.toString( goid ) );
                        reference.setGuid( includedPolicy.getGuid() );
                        references.add( reference );
                    }
                }

                if ( !warnings.isEmpty() ) {
                    pir.setWarnings( warnings );
                }

                if ( !references.isEmpty() ) {
                    pir.setImportedPolicyReferences(references  );
                }
            }

            return pir;
        } catch ( PolicyImportCancelledException e ) {
            throw new ResourceFactory.ResourceAccessException(e); // Should not occur
        } catch ( ObjectModelException e ) {
            throw new ResourceFactory.ResourceAccessException(e);
        } catch ( SAXException e ) {
            throw new ResourceFactory.InvalidResourceException( ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid policy export '"+ExceptionUtils.getMessage( e )+"'" );
        } catch ( InvalidPolicyStreamException e ) {
            throw new ResourceFactory.InvalidResourceException( ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid policy export '"+ExceptionUtils.getMessage( e )+"'" );
        }
    }

    /**
     * Validate the given / referenced policy and it's dependencies.
     *
     * @param validationContext The validation request context
     * @param resolver Resolver to locate referenced policy if not part of the request
     * @return The result of the policy validation
     * @throws ResourceFactory.InvalidResourceException If the given policy or data is invalid or the referenced policy is invalid
     * @throws ResourceFactory.ResourceNotFoundException If the referenced policy does not exists
     */
    public PolicyValidationResult validatePolicy( final PolicyValidationContext validationContext,
                                                  final PolicyResolver resolver ) throws ResourceFactory.InvalidResourceException, ResourceFactory.ResourceNotFoundException {
        final PolicyValidationContext policyValidationContext = optional( validationContext ).orSome( new PolicyValidationContext() );

        // Get request values
        PolicyType policyType = getPolicyType( policyValidationContext.getPolicyType() );
        String policyInternalTag = null;
        boolean soap = isSoap( policyValidationContext.getProperties() );
        SoapVersion soapVersion = soap ? getSoapVersion( policyValidationContext.getProperties() ) : null;
        final Map<String, ResourceSet> resourceSetMap = resourceHelper.getResourceSetMap( policyValidationContext.getResourceSets() );
        Wsdl wsdl = getWsdl( resourceHelper.getResources( resourceSetMap, ResourceHelper.WSDL_TAG, false, null ) );
        @Nullable Assertion assertion = getAssertion( resourceHelper.getResources( resourceSetMap, ResourceHelper.POLICY_TAG, false, null ));

        // If the request does not specify a policy see if an existing policy can be resolved
        if ( assertion == null ) {
            // Policy not passed in request so validate existing policy with current configuration.
            Policy policy = resolver.resolve();
            try {
                assertion = policy.getAssertion();
            } catch ( IOException e ) {
                throw new ResourceFactory.ResourceAccessException("Policy is invalid.");
            }
            policyType = policy.getType();
            policyInternalTag = policy.getInternalTag();
            soap = policy.isSoap();
            wsdl = resolver.resolveWsdl();
            soapVersion = resolver.resolveSoapVersion();
        }

        // Run the validator
        final PolicyValidatorResult result;
        try {
            result = policyValidator.validate( assertion, new com.l7tech.policy.validator.PolicyValidationContext(policyType, policyInternalTag, wsdl, soap, soapVersion), licenseManager );
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new ResourceFactory.ResourceAccessException(e);
        }

        // Process the results, ensure duplicates are removed
        final Set<PolicyValidationResult.PolicyValidationMessage> messages = new LinkedHashSet<PolicyValidationResult.PolicyValidationMessage>();
        PolicyValidationResult.ValidationStatus status = PolicyValidationResult.ValidationStatus.OK;
        for ( final PolicyValidatorResult.Message message : result.getMessages() ) {
            final PolicyValidationResult.PolicyValidationMessage pvm = ManagedObjectFactory.createPolicyValidationMessage();
            pvm.setAssertionOrdinal( message.getAssertionOrdinal() );
            if ( message instanceof PolicyValidatorResult.Error ) {
                status = PolicyValidationResult.ValidationStatus.ERROR;
                pvm.setLevel( "Error" );
            } else {
                if ( status == PolicyValidationResult.ValidationStatus.OK ) {
                    status = PolicyValidationResult.ValidationStatus.WARNING;
                }
                pvm.setLevel( "Warning" );
            }
            pvm.setMessage( message.getMessage() );

            final List<PolicyValidationResult.AssertionDetail> details = new ArrayList<PolicyValidationResult.AssertionDetail>();
            Assertion current = assertion;
            for ( final Integer position : message.getAssertionIndexPath() ) {
                final PolicyValidationResult.AssertionDetail detail = ManagedObjectFactory.createAssertionDetail();
                detail.setPosition( position );

                Assertion child = null;
                if ( current instanceof CompositeAssertion ) {
                    child = ((CompositeAssertion)current).getChildren().get( position );
                } else if ( current instanceof Include ) {
                    final Include include = (Include) current;
                    Policy policy = getIncludePolicy( include );
                    child = getPolicyAssertionChild( policy, position );
                }
                current = child;

                if ( current != null ) {
                    detail.setDescription( current.meta().<String>get( AssertionMetadata.WSP_EXTERNAL_NAME ) + " (" + current.meta().<String>get( AssertionMetadata.PALETTE_NODE_NAME ) + ")" );
                } else {
                    detail.setDescription( "" );
                }

                details.add( detail );
            }
            pvm.setAssertionDetails( details );

            messages.add( pvm );
        }

        final PolicyValidationResult pvr = ManagedObjectFactory.createPolicyValidationResult();
        pvr.setStatus( status );
        if ( !messages.isEmpty() ) {
            pvr.setPolicyValidationMessages( new ArrayList<PolicyValidationResult.PolicyValidationMessage>(messages) );
        }
        return pvr;
    }

    /**
     * Validate that the given policy has a valid syntax and an AllAssertion at the root.
     *
     * @param policyXml The policy to check
     * @return The policy XML if it is valid
     * @throws ResourceFactory.InvalidResourceException If the policy is invalid
     */
    public String validatePolicySyntax( final String policyXml ) throws ResourceFactory.InvalidResourceException {
        try {
            final Assertion assertion = wspReader.parsePermissively( policyXml, WspReader.INCLUDE_DISABLED );
            if ( !(assertion instanceof AllAssertion) ) {
                throw new ResourceFactory.InvalidResourceException( ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid policy");
            }
        } catch ( IOException e ) {
            throw new ResourceFactory.InvalidResourceException( ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid policy");
        }

        return policyXml;
    }

    /**
     * Check that the current admin user (if any) has appropriate AssertionAccess RBAC permission for all assertions
     * used within the specified policy.
     *
     * @param policy the policy to examine.  If null, this method takes no action.
     * @throws PermissionDeniedException if a contextual admin user is present and the policy contains at least one assertion
     *                                   for which the current admin user does not have permission to save a policy that uses that assertion.
     * @throws ResourceFactory.InvalidResourceException if the policy contains invalid policy XML.
     */
    public void checkPolicyAssertionAccess( final Policy policy ) throws PermissionDeniedException, ResourceFactory.InvalidResourceException {
        try {
            if ( policyAssertionRbacChecker != null )
                policyAssertionRbacChecker.checkPolicy( policy );
        } catch (IOException e) {
            throw new ResourceFactory.InvalidResourceException( ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid policy");
        } catch (FindException e) {
            throw (PermissionDeniedException) new PermissionDeniedException( OperationType.CREATE, EntityType.ASSERTION_ACCESS, "Error in permission check.").initCause(e);
        }
    }

    public static class GatewayExternalReferenceFinder implements ExternalReferenceFinder {
        private final RbacServices rbacServices;
        private final SecurityFilter securityFilter;
        private final CustomAssertionsRegistrar customAssertionsRegistrar;
        private final IdentityProviderConfigManager identityProviderConfigManager;
        private final IdentityProviderFactory identityProviderFactory;
        private final JdbcConnectionManager jdbcConnectionManager;
        private final JmsConnectionManager jmsConnectionManager;
        private final JmsEndpointManager jmsEndpointManager;
        private final PolicyManager policyManager;
        private final ResourceEntryManager resourceEntryManager;
        private final SsgKeyStoreManager ssgKeyStoreManager;
        private final TrustedCertManager trustedCertManager;
        private final SsgActiveConnectorManager ssgActiveConnectorManager;
        private final PolicyExporterImporterManager policyExporterImporterManager;
        private final SiteMinderConfigurationManager siteMinderConfigurationManager;
        private final GenericEntityManager genericEntityManager;
        private final HttpConfigurationManager httpConfigurationManager;
        private final RoleManager roleManager;
        private final SecurePasswordManager securePasswordManager;
        private final CustomKeyValueStoreManager customKeyValueStoreManager;

        public GatewayExternalReferenceFinder( final RbacServices rbacServices,
                                               final SecurityFilter securityFilter,
                                               final CustomAssertionsRegistrar customAssertionsRegistrar,
                                               final IdentityProviderConfigManager identityProviderConfigManager,
                                               final IdentityProviderFactory identityProviderFactory,
                                               final JdbcConnectionManager jdbcConnectionManager,
                                               final JmsConnectionManager jmsConnectionManager,
                                               final JmsEndpointManager jmsEndpointManager,
                                               final PolicyManager policyManager,
                                               final ResourceEntryManager resourceEntryManager,
                                               final SsgKeyStoreManager ssgKeyStoreManager,
                                               final TrustedCertManager trustedCertManager,
                                               final SsgActiveConnectorManager ssgActiveConnectorManager,
                                               final PolicyExporterImporterManager policyExporterImporterManager,
                                               final SiteMinderConfigurationManager siteMinderConfigurationManager,
                                               final GenericEntityManager genericEntityManager,
                                               final HttpConfigurationManager httpConfigurationManager,
                                               final RoleManager roleManager,
                                               final SecurePasswordManager securePasswordManager,
                                               final CustomKeyValueStoreManager customKeyValueStoreManager) {
            this.rbacServices = rbacServices;
            this.securityFilter = securityFilter;
            this.customAssertionsRegistrar = customAssertionsRegistrar;
            this.identityProviderConfigManager = identityProviderConfigManager;
            this.identityProviderFactory = identityProviderFactory;
            this.jdbcConnectionManager = jdbcConnectionManager;
            this.jmsConnectionManager = jmsConnectionManager;
            this.jmsEndpointManager = jmsEndpointManager;
            this.policyManager = policyManager;
            this.resourceEntryManager = resourceEntryManager;
            this.ssgKeyStoreManager = ssgKeyStoreManager;
            this.trustedCertManager = trustedCertManager;
            this.ssgActiveConnectorManager = ssgActiveConnectorManager;
            this.policyExporterImporterManager = policyExporterImporterManager;
            this.siteMinderConfigurationManager = siteMinderConfigurationManager;
            this.genericEntityManager = genericEntityManager;
            this.httpConfigurationManager = httpConfigurationManager;
            this.roleManager = roleManager;
            this.securePasswordManager = securePasswordManager;
            this.customKeyValueStoreManager = customKeyValueStoreManager;
        }

        private User getUser() {
            return JaasUtils.getCurrentUser();
        }

        private <E extends Entity> E filter( final E entity ) throws FindException {
            E filtered = null;

            if ( entity != null && getUser() != null && rbacServices.isPermittedForEntity( getUser(), entity, OperationType.READ, null ) ) {
                filtered = entity;
            }

            return filtered;
        }

        private <EH extends EntityHeader> EH filter( final EH entityHeader ) throws FindException {
            EH filtered = null;

            if ( entityHeader != null && getUser() != null ) {
                Collection<EH> filteredCollection = securityFilter.filter( Collections.singleton( entityHeader ), getUser(), OperationType.READ, null );
                if ( !filteredCollection.isEmpty() ) {
                    filtered = entityHeader;
                }
            }

            return filtered;
        }

        @SuppressWarnings({ "unchecked" })
        private <E> Collection<E> filter( final Collection<E> entitiesOrEntityHeaders ) throws FindException {
            Collection<E> filtered;

            EntityType entityType = null;
            if ( entitiesOrEntityHeaders.size() > 0 ) {
                Object item = entitiesOrEntityHeaders.iterator().next();
                if ( item instanceof EntityHeader ) {
                    entityType = ((EntityHeader)item).getType();
                } else if ( item instanceof Entity ) {
                    entityType = EntityType.findTypeByEntity( (Class<? extends Entity>)item.getClass() );
                }
            }

            final User user = getUser();
            if ( user == null ) {
                filtered = Collections.emptyList();
            } else if ( entityType != null && rbacServices.isPermittedForAnyEntityOfType(user, OperationType.READ, entityType) ) {
                filtered = entitiesOrEntityHeaders;
            } else {
                filtered = securityFilter.filter( entitiesOrEntityHeaders, user, OperationType.READ, null );
            }

            return filtered;
        }

        @Override
        public TrustedCert findCertByPrimaryKey( final Goid certOid ) throws FindException {
            return filter( trustedCertManager.findByPrimaryKey( certOid ) );
        }

        @Override
        public Collection<TrustedCert> findAllCerts() throws FindException {
            return filter( trustedCertManager.findAll() );
        }

        @Override
        public SsgKeyEntry findKeyEntry( final String alias, final Goid keystoreGoid ) throws FindException, KeyStoreException {
            try {
                return filter(ssgKeyStoreManager.lookupKeyByKeyAlias( alias, keystoreGoid ) );
            } catch ( ObjectNotFoundException e ) {
                return null;
            }
        }

        @Override
        public Collection getAssertions() {
            return customAssertionsRegistrar.getAssertions();
        }

        @Override
        public Policy findPolicyByGuid( final String guid ) throws FindException {
            return filter( policyManager.findByGuid( guid ) );
        }

        @Override
        public Policy findPolicyByUniqueName( final String name ) throws FindException {
            return filter( policyManager.findByUniqueName( name ) );
        }

        @Override
        public ResourceEntryHeader findResourceEntryByUriAndType( final String uri, final ResourceType type ) throws FindException {
            return filter( resourceEntryManager.findHeaderByUriAndType( uri, type ) );
        }

        @Override
        public Collection<ResourceEntryHeader> findResourceEntryByKeyAndType( final String key, final ResourceType type ) throws FindException {
            return filter( resourceEntryManager.findHeadersByKeyAndType( key, type ) );
        }

        public JdbcConnection getJdbcConnectionById( final String id ) throws FindException {
            try {
                return filter( jdbcConnectionManager.findByPrimaryKey( GoidUpgradeMapper.mapId(EntityType.JDBC_CONNECTION, id) ) );
            } catch ( IllegalArgumentException nfe ) {
                return null;
            }
        }

        @Override
        public JdbcConnection getJdbcConnection( final String name ) throws FindException {
            return filter( jdbcConnectionManager.findByUniqueName( name ) );
        }

        @Override
        public JmsEndpoint findEndpointByPrimaryKey( final Goid goid ) throws FindException {
            return filter( jmsEndpointManager.findByPrimaryKey( goid ) );
        }

        @Override
        public JmsConnection findConnectionByPrimaryKey( final Goid goid ) throws FindException {
            return filter( jmsConnectionManager.findByPrimaryKey( goid ) );
        }

        @Override
        public SsgActiveConnector findConnectorByPrimaryKey(Goid goid) throws FindException {
            return filter(ssgActiveConnectorManager.findByPrimaryKey(goid));
        }

        @Override
        public Collection<SsgActiveConnector> findSsgActiveConnectorsByType(String type) throws FindException {
            return filter(ssgActiveConnectorManager.findSsgActiveConnectorsByType(type));
        }

        @Override
        public Set<ExternalReferenceFactory> findAllExternalReferenceFactories() throws FindException {
            return policyExporterImporterManager.findAllExternalReferenceFactories();
        }

        @Override
        public List<Pair<JmsEndpoint, JmsConnection>> loadJmsQueues() throws FindException {
            final List<Pair<JmsEndpoint, JmsConnection>> result = new ArrayList<Pair<JmsEndpoint, JmsConnection>>();
            final Collection<JmsConnection> connections = filter( jmsConnectionManager.findAll() );
            for ( final JmsConnection connection : connections ) {
                final JmsEndpoint[] endpoints = jmsEndpointManager.findEndpointsForConnection(connection.getGoid());
                for ( final JmsEndpoint endpoint : endpoints) {
                    JmsEndpoint filteredEndpoint = filter( endpoint );
                    if ( filteredEndpoint != null ) {
                        result.add(new Pair<JmsEndpoint, JmsConnection>(filteredEndpoint, connection));
                    }
                }
            }

            return result;
        }

        @Override
        public EntityHeader[] findAllIdentityProviderConfig() throws FindException {
            Collection<EntityHeader> headers = filter( identityProviderConfigManager.findAllHeaders() );
            return headers.toArray( new EntityHeader[headers.size()] );
        }

        @Override
        public IdentityProviderConfig findIdentityProviderConfigByID( final Goid providerOid ) throws FindException {
            return filter( identityProviderConfigManager.findByPrimaryKey( providerOid ) );
        }

        @Override
        public EntityHeaderSet<IdentityHeader> findAllGroups( final Goid providerOid ) throws FindException {
            return (EntityHeaderSet<IdentityHeader>)filter( getIdentityProvider( providerOid ).getGroupManager().findAllHeaders() );
        }

        @Override
        public Group findGroupByID( final Goid providerOid, final String groupId ) throws FindException {
            return filter( getIdentityProvider( providerOid ).getGroupManager().findByPrimaryKey( groupId ) );
        }

        @Override
        public Group findGroupByName( final Goid providerOid, final String name ) throws FindException {
            return filter( getIdentityProvider( providerOid ).getGroupManager().findByName( name ) );
        }

        @Override
        public Collection<IdentityHeader> getUserHeaders( final Goid providerOid, final String groupId ) throws FindException {
            return filter( getIdentityProvider( providerOid ).getGroupManager().getUserHeaders( groupId ) );
        }

        @Override
        public Collection<IdentityHeader> findAllUsers( final Goid providerOid ) throws FindException {
            return filter( getIdentityProvider( providerOid ).getUserManager().findAllHeaders() );
        }

        @Override
        public User findUserByID( final Goid providerOid, final String userId ) throws FindException {
            return filter( getIdentityProvider( providerOid ).getUserManager().findByPrimaryKey( userId ) );
        }

        @Override
        public User findUserByLogin( final Goid providerOid, final String login ) throws FindException {
            return filter( getIdentityProvider( providerOid ).getUserManager().findByLogin( login ) );
        }

        @Override
        public SiteMinderConfiguration findSiteMinderConfigurationByName(final String name) throws FindException {
            return filter(siteMinderConfigurationManager.findByUniqueName(name));
        }

        @Override
        public SiteMinderConfiguration findSiteMinderConfigurationByID(final Goid id) throws FindException {
            return filter(siteMinderConfigurationManager.findByPrimaryKey(id));
        }


        @Override
        public <ET extends GenericEntity> EntityManager<ET, GenericEntityHeader> getGenericEntityManager(@NotNull Class<ET> entityClass) throws FindException {
            return genericEntityManager.getEntityManager(entityClass);
        }

        @Override
        public SecurePassword findSecurePasswordById(Goid id) throws FindException {
            return filter( securePasswordManager.findByPrimaryKey( id ) );
        }

        @Override
        public SecurePassword findSecurePasswordByName(String name) throws FindException {
            return filter( securePasswordManager.findByUniqueName( name ) );
        }

        @Override
        public KeyValueStore getCustomKeyValueStore() {
            return new FilteringKeyValueStore(customKeyValueStoreManager);
        }

        @Override
        public CustomEntitySerializer getCustomKeyValueEntitySerializer(final String entitySerializerClassName) {
            return customAssertionsRegistrar.getExternalEntitySerializer(entitySerializerClassName);
        }

        private IdentityProvider<?,?,?,?> getIdentityProvider( final Goid providerOid ) throws FindException {
            final IdentityProvider provider = identityProviderFactory.getProvider( providerOid );
            if ( provider == null )
                throw new FindException("IdentityProvider could not be found");
            return provider;
        }

        /**
         * Utility class for filtering CustomKeyValueStore entities based on user roles.
         * <p/>
         * Taking in count that the class can be used from ESM, we cannot use
         * {@link com.l7tech.console.api.CustomKeyValueStoreImpl console version of KeyValueStore} since it uses
         * {@link com.l7tech.console.util.Registry Registry} to call admin methods. <br/>
         * Instead we have to use a server version of the {@code KeyValueStore} and implement filtering manually
         * based on user roles.
         * <p/>
         * For convenience {@link GatewayExternalReferenceFinder#filter(com.l7tech.objectmodel.Entity)} method, and its
         * variants, are used, therefore this class is designed as a inner class of {@link GatewayExternalReferenceFinder}
         */
        private class FilteringKeyValueStore implements KeyValueStore {
            /**
             * This is the server version of KeyValueStore manager.
             */
            @NotNull
            private final CustomKeyValueStoreManager customKeyValueStoreManager;

            public FilteringKeyValueStore(@NotNull final CustomKeyValueStoreManager customKeyValueStoreManager) {
                this.customKeyValueStoreManager = customKeyValueStoreManager;
            }

            @Override
            public Map<String, byte[]> findAllWithKeyPrefix(final String keyPrefix) throws KeyValueStoreException {
                try {
                    final Collection<CustomKeyValueStore> customKeyValues = filter(customKeyValueStoreManager.findByKeyPrefix(keyPrefix));
                    final Map<String, byte[]> result = new HashMap<>(customKeyValues.size());
                    for (CustomKeyValueStore customKeyValue : customKeyValues) {
                        result.put(customKeyValue.getName(), customKeyValue.getValue());
                    }
                    return result;
                } catch (FindException e) {
                    throw new KeyValueStoreException("Unable to find all: " + ExceptionUtils.getMessage(e), e);
                }
            }

            @Override
            public byte[] get(final String key) throws KeyValueStoreException {
                try {
                    final CustomKeyValueStore customKeyValue = filter(customKeyValueStoreManager.findByUniqueName(key));
                    return (customKeyValue != null) ? customKeyValue.getValue() : null;
                } catch (FindException e) {
                    throw new KeyValueStoreException("Unable to get: " + ExceptionUtils.getMessage(e), e);
                }
            }

            @Override
            public boolean contains(final String key) throws KeyValueStoreException {
                try {
                    return filter(customKeyValueStoreManager.findByUniqueName(key)) != null;
                } catch (FindException e) {
                    throw new KeyValueStoreException("Unable to check if key exists: " + ExceptionUtils.getMessage(e), e);
                }
            }

            @Override
            public void save(final String key, final byte[] value) throws KeyValueStoreException {
                try {
                    final User user = getUser();
                    if (user != null) {
                        // no need to check whether the key already exists or not, since it may be misleading in case
                        // when the key already exists in the system but the user doesn't have read access to it.
                        // This means that for the user the key doesn't exists but it does system wise.
                        // We'll let CustomKeyValueStoreManager fail with proper SaveException.
                        final EntityType entityType = EntityType.findTypeByEntity(CustomKeyValueStore.class);
                        if (entityType != null && rbacServices.isPermittedForAnyEntityOfType(user, OperationType.CREATE, entityType)) {
                            final CustomKeyValueStore customKeyValue = new CustomKeyValueStore();
                            customKeyValue.setName(key);
                            customKeyValue.setValue(value);
                            customKeyValueStoreManager.save(customKeyValue);
                        }
                    }
                } catch (FindException | SaveException e) {
                    throw new KeyValueStoreException("Unable to save: " + ExceptionUtils.getMessage(e), e);
                }
            }

            @Override
            public void update(final String key, final byte[] value) throws KeyValueStoreException {
                try {
                    final User user = getUser();
                    if (user != null) {
                        final CustomKeyValueStore customKeyValue = customKeyValueStoreManager.findByUniqueName(key);
                        if (customKeyValue != null &&
                            rbacServices.isPermittedForEntity(user, customKeyValue, OperationType.READ, null) &&
                            rbacServices.isPermittedForEntity(user, customKeyValue, OperationType.UPDATE, null))
                        {
                            customKeyValue.setValue(value);
                            customKeyValueStoreManager.update(customKeyValue);
                        }
                    }
                } catch (FindException | UpdateException e) {
                    throw new KeyValueStoreException("Unable to update: " + ExceptionUtils.getMessage(e), e);
                }
            }

            @Override
            public void saveOrUpdate(final String key, final byte[] value) throws KeyValueStoreException {
                if (!this.contains(key)) {
                    this.save(key, value);
                } else {
                    this.update(key, value);
                }
            }

            @Override
            public void delete(final String key) throws KeyValueStoreException {
                try {
                    final User user = getUser();
                    if (user != null) {
                        final EntityType entityType = EntityType.findTypeByEntity(CustomKeyValueStore.class);
                        if (entityType != null && rbacServices.isPermittedForAnyEntityOfType(user, OperationType.DELETE, entityType)) {
                            customKeyValueStoreManager.deleteByKey(key);
                        }
                    }
                } catch (FindException | DeleteException e) {
                    throw new KeyValueStoreException("Unable to delete: " + ExceptionUtils.getMessage(e), e);
                }
            }

            @Override
            public <L extends KeyValueStoreListener> L getListener(Class<L> lClass) {
                // Key value store listener not supported.
                //
                return null;
            }
        }
    }

    //- PACKAGE

    interface PolicyResolver {
        Policy resolve() throws ResourceFactory.ResourceNotFoundException;
        Wsdl resolveWsdl() throws ResourceFactory.ResourceNotFoundException;
        SoapVersion resolveSoapVersion() throws ResourceFactory.ResourceNotFoundException;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( PolicyHelper.class.getName() );

    private static final String PROP_ACCEPT_POLICY_CONFLICT = "acceptPolicyConflict";
    private static final String PROP_FORCE = "force";

    private final AssertionLicense licenseManager;
    private final PolicyValidator policyValidator;
    private final WspReader wspReader;
    private final GatewayExternalReferenceFinder referenceFinder;
    private final EntityResolver entityResolver;
    private final PolicyAssertionRbacChecker policyAssertionRbacChecker;
    private final ResourceHelper resourceHelper = new ResourceHelper();

    private boolean isSoap( final Map<String,Object> properties ) {
        boolean soap = false;

        if ( properties != null && properties.get( "soap" ) instanceof Boolean ) {
            soap = (Boolean) properties.get( "soap" );
        }

        return soap;
    }

    private SoapVersion getSoapVersion( final Map<String,Object> properties ) {
        SoapVersion soapVersion = null;

        if ( properties != null && properties.get( "soapVersion" ) != null ) {
            final String soapVersionText = properties.get( "soapVersion" ).toString().trim();
            if ( soapVersionText != null ) {
                soapVersion = SoapVersion.versionNumberToSoapVersion( soapVersionText );
            }
        }

        return soapVersion;
    }

    private PolicyType getPolicyType( final PolicyDetail.PolicyType policyType ) {
        PolicyType requestPolicyType;

        if ( policyType != null ) {
            switch ( policyType ) {
                case INCLUDE:
                    requestPolicyType = PolicyType.INCLUDE_FRAGMENT;
                    break;
                case INTERNAL:
                    requestPolicyType = PolicyType.INTERNAL;
                    break;
                case GLOBAL:
                    requestPolicyType = PolicyType.GLOBAL_FRAGMENT;
                    break;
                default:
                    throw new ResourceFactory.ResourceAccessException( "Unknown policy type '" + policyType + "'" );
            }
        } else {
            requestPolicyType = PolicyType.PRIVATE_SERVICE;
        }

        return requestPolicyType;
    }

    private Map<String,String> toMap( final Collection<Resource> resources,
                                      final boolean byUrl,
                                      final String type ) throws ResourceFactory.InvalidResourceException {
        final Map<String,String> resourceMap = new HashMap<String,String>();

        for ( Resource resource : resources ) {
            String id = byUrl ? resource.getSourceUrl() : resource.getId();
            if ( id != null && resource.getContent() != null && (type==null || type.equals(resource.getType())) ) {
                if ( resourceMap.put( id, resource.getContent() ) != null ) {
                    throw new ResourceFactory.InvalidResourceException(
                            ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,
                            "duplicate resource '"+id+"'" );
                }
            }
        }

        return resourceMap;
    }

    private Wsdl getWsdl( final Collection<Resource> resources ) throws ResourceFactory.InvalidResourceException {
        Wsdl wsdl = null;

        if ( !resources.isEmpty() ) {
            final Resource base = resources.iterator().next();
            if ( !ResourceHelper.WSDL_TYPE.equals(base.getType() )) {
                throw new ResourceFactory.InvalidResourceException(
                        ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,
                        "WSDL root resource type incorrect." );
            }
            try {
                wsdl = Wsdl.newInstance( Wsdl.getWSDLLocator( base.getSourceUrl(), toMap(resources, true, null), logger ));
            } catch ( WSDLException e ) {
                throw new ResourceFactory.InvalidResourceException(
                        ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,
                        "invalid WSDL '"+ExceptionUtils.getMessage(e)+"'" );
            }
        }

        return wsdl;
    }

    private Assertion getAssertion( final Collection<Resource> resources ) throws ResourceFactory.InvalidResourceException {
        Assertion assertion = null;

        if ( !resources.isEmpty() ) {
            Resource resource = resources.iterator().next();
            if ( !ResourceHelper.POLICY_TYPE.equals(resource.getType() )) {
                throw new ResourceFactory.InvalidResourceException(
                        ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,
                        "Policy root resource type incorrect." );
            }
            try {
                assertion = wspReader.parsePermissively(resource.getContent(), WspReader.INCLUDE_DISABLED);
                addPoliciesToPolicyReferenceAssertions(assertion, toMap(resources, false, ResourceHelper.POLICY_TYPE));
            } catch (IOException e) {
                throw new ResourceFactory.InvalidResourceException(
                        ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,
                        "Error parsing policy '"+ExceptionUtils.getMessage(e)+"'" );
            }
        }

        return assertion;
    }

    private Policy getIncludePolicy( final Include include ) {
        Policy policy = include.retrieveFragmentPolicy();

        if ( policy == null ) {
            try {
                policy = referenceFinder.findPolicyByGuid( include.getPolicyGuid() );
            } catch ( FindException e ) {
                throw new ResourceFactory.ResourceAccessException(e);
            }
            include.replaceFragmentPolicy( policy );
        }

        return policy;
    }

    private Assertion getPolicyAssertionChild( final Policy policy,
                                               final int position ) {
        Assertion child = null;

        if ( policy != null ) {
            try {
                final Assertion assertion = policy.getAssertion();
                final CompositeAssertion compositeAssertion = assertion instanceof CompositeAssertion ?
                        (CompositeAssertion) assertion :
                        null;
                if ( compositeAssertion != null && compositeAssertion.getChildren().size() > position ) {
                    child = compositeAssertion.getChildren().get( position );
                }
            } catch ( IOException e ) {
                // continue but don't include details
            }
        }

        return child;
    }

    private void addPoliciesToPolicyReferenceAssertions( final @Nullable Assertion rootAssertion,
                                                         final Map<String, String> fragments ) throws IOException {
        if( rootAssertion instanceof CompositeAssertion ) {
            final CompositeAssertion compAssertion = (CompositeAssertion)rootAssertion;
            for( Iterator it = compAssertion.children();it.hasNext();) {
                final Assertion child = (Assertion)it.next();
                addPoliciesToPolicyReferenceAssertions(child, fragments);
            }
        } else if( rootAssertion instanceof PolicyReference ) {
            final PolicyReference policyReference = (PolicyReference) rootAssertion;
            final String guid = policyReference.retrievePolicyGuid();
            if( fragments.containsKey( guid ) ) {
                final Policy fragmentPolicy = new Policy(PolicyType.INCLUDE_FRAGMENT, guid, fragments.get( guid ), false);
                policyReference.replaceFragmentPolicy( fragmentPolicy );
                addPoliciesToPolicyReferenceAssertions(policyReference.retrieveFragmentPolicy().getAssertion(), fragments);
            }
        }
    }

    // Todo: the logic in the method below doesn't make much sense. Why are we getting the real internal id of an
    // entity when we only want the referenced id?
    private String getId( final ExternalReference reference ) {
        String refId = reference.getRefId();

        if ( refId == null ) {
            try {
                // Some references do not include an ID so we need to resolve them
                if ( reference instanceof JdbcConnectionReference ) {
                    final String name = ((JdbcConnectionReference) reference).getConnectionName();
                    if ( name != null ) {
                        JdbcConnection connection = referenceFinder.getJdbcConnection( name );
                        if ( connection != null ) {
                            refId = connection.getId();
                        }
                    }
                } else if ( reference instanceof GlobalResourceReference ) {
                    final GlobalResourceReference globalResourceReference = (GlobalResourceReference) reference;
                    final ResourceType type = globalResourceReference.getType();
                    final String uri = globalResourceReference.getSystemIdentifier();
                    final String key = globalResourceReference.getResourceKey1();
                    if ( uri != null ) {
                        final ResourceEntryHeader resourceEntryHeader = referenceFinder.findResourceEntryByUriAndType( uri, type );
                        if ( resourceEntryHeader != null ) {
                            refId = resourceEntryHeader.getStrId();
                        }
                    }
                    if ( refId == null && key != null ) {
                        Collection<ResourceEntryHeader> resourceEntryHeaders = referenceFinder.findResourceEntryByKeyAndType( key, type );
                        if ( resourceEntryHeaders.size() == 1 ) {
                            refId = resourceEntryHeaders.iterator().next().getStrId();
                        }
                    }
                } else if ( reference instanceof IncludedPolicyReference){
                    refId = ((IncludedPolicyReference) reference).getGuid();
                }

                // If there is no natural identifier use a synthetic identifier
                if ( refId == null ) {
                    refId = reference.getSyntheticRefId();
                }
            } catch ( FindException fe ) {
                throw new ResourceFactory.ResourceAccessException(fe);
            }
        }

        return refId;
    }

    private String buildErrorDetail( final List<Triple<String, String, String>> conflictingPolicies,
                                     final List<ExternalReference> unresolvedReferences ) {
        StringBuilder builder = new StringBuilder();

        if ( !unresolvedReferences.isEmpty() ) {
            builder.append( "The following dependencies could not be automatically resolved (" );
            boolean first = true;
            for ( ExternalReference reference : unresolvedReferences ) {
                if ( first ) first = false;
                else builder.append(", ");
                builder.append( reference.getRefType() );
                builder.append( ":" );
                builder.append( getId(reference) );
            }
            builder.append(")");
        }

        if ( !conflictingPolicies.isEmpty() ) {
            if ( !unresolvedReferences.isEmpty() ) builder.append(". ");
            builder.append( "The following policy include fragments already exist but do not match the policy include fragments in the policy export (" );
            boolean first = true;
            for ( Triple<String,String,String> conflictingPolicy : conflictingPolicies ) {
                if ( first ) first = false;
                else builder.append(", ");
                builder.append( conflictingPolicy.left );
                if ( !conflictingPolicy.left.equalsIgnoreCase(conflictingPolicy.middle) ) {
                    // If the import name does not match the existing policy name then show both names
                    builder.append( '/' );
                    builder.append( conflictingPolicy.middle );
                }
                builder.append( "[guid:" );
                builder.append( conflictingPolicy.right );
                builder.append( ']' );
            }
            builder.append(")");
        }
        return builder.toString();
    }

    private PolicyImporter.PolicyImporterAdvisor buildPolicyImportAdvisor( final PolicyImportContext policyImportContext,
                                                                           final List<PolicyImportResult.ImportedPolicyReference> references,
                                                                           final List<Triple<String, String, String>> conflictingPolicies,
                                                                           final List<ExternalReference> unresolvedReferences ) {
        return new PolicyImporter.PolicyImporterAdvisor(){
            @Override
            public boolean mapReference( final String referenceType, final String referenceId, final String targetId ) {
                PolicyImportResult.ImportedPolicyReference reference = ManagedObjectFactory.createImportedPolicyReference();
                reference.setType( PolicyImportResult.ImportedPolicyReferenceType.MAPPED );
                reference.setReferenceType( referenceType );
                reference.setReferenceId( referenceId );
                reference.setId( targetId );
                references.add( reference );
                return true;
            }

            @Override
            public boolean resolveReferences( final ExternalReference[] unresolvedRefsArray ) {
                final List<PolicyReferenceInstruction> instructions = policyImportContext.getPolicyReferenceInstructions();
                for ( final ExternalReference reference : unresolvedRefsArray ) {
                    final String type = reference.getRefType();
                    final String id = getId(reference);

                    if ( id == null || instructions == null ) {
                        unresolvedReferences.add( reference );
                        continue;
                    }

                    boolean handled = false;
                    for ( final PolicyReferenceInstruction instruction : instructions ) {
                        if ( type.equals( instruction.getReferenceType() ) && id.equals(handleReferenceId(instruction)) ) {
                            switch ( instruction.getPolicyReferenceInstructionType() ) {
                                case DELETE:
                                    handled = reference.setLocalizeDelete();
                                    break;
                                case IGNORE:
                                    reference.setLocalizeIgnore();
                                    handled = true;
                                    break;
                                case MAP:
                                    final String mapId = instruction.getMappedReferenceId();
                                    if ( mapId != null && !mapId.trim().isEmpty() ) {
                                        handled = reference.setLocalizeReplace( mapId.trim() );
                                    }
                                    if ( !handled && reference instanceof JdbcConnectionReference ) {
                                        JdbcConnectionReference jdbcReference = (JdbcConnectionReference) reference;
                                        try {
                                            JdbcConnection connection = referenceFinder.getJdbcConnectionById( mapId );
                                            if ( connection != null ) {
                                                jdbcReference.setLocalizeReplaceByName( connection.getName() );
                                                handled = true;
                                            }
                                        } catch ( FindException fe ) {
                                            throw new ResourceFactory.ResourceAccessException(fe);
                                        }
                                    }
                                    break;
                                case RENAME:
                                    final String mapName = instruction.getMappedName();
                                    if ( mapName != null && !mapName.trim().isEmpty() ) {
                                        handled = reference.setLocalizeRename( mapName.trim() );
                                    }
                                    break;
                                default:
                                    break;
                            }
                            break;
                        }
                    }

                    if ( !handled ) {
                        unresolvedReferences.add( reference );
                    }
                }
                return unresolvedReferences.isEmpty();
            }

            private String handleReferenceId(PolicyReferenceInstruction instruction) {
                // map reference id if it smells like an old id
                if(!ValidationUtils.isValidGoid(instruction.getReferenceId(),false) && ValidationUtils.isValidLong(instruction.getReferenceId(),false,Long.MIN_VALUE,Long.MAX_VALUE))
                {
                    if(instruction.getReferenceType().equals("com.l7tech.console.policy.exporter.IdProviderReference")){
                        return GoidUpgradeMapper.mapId(EntityType.ID_PROVIDER_CONFIG,instruction.getReferenceId()).toString();
                    }else if(instruction.getReferenceType().equals("com.l7tech.console.policy.exporter.FederatedIdProviderReference")){
                        return GoidUpgradeMapper.mapId(EntityType.ID_PROVIDER_CONFIG,instruction.getReferenceId()).toString();
                    }else if(instruction.getReferenceType().equals("com.l7tech.console.policy.exporter.IncludedPolicyReference")){
                        return GoidUpgradeMapper.mapId(EntityType.POLICY,instruction.getReferenceId()).toString();
                    }else if(instruction.getReferenceType().equals("com.l7tech.console.policy.exporter.JMSEndpointReference")){
                        return GoidUpgradeMapper.mapId(EntityType.JMS_ENDPOINT,instruction.getReferenceId()).toString();
                    }else if(instruction.getReferenceType().equals("com.l7tech.console.policy.exporter.TrustedCertReference")){
                        return GoidUpgradeMapper.mapId(EntityType.TRUSTED_CERT,instruction.getReferenceId()).toString();
                    }else if(instruction.getReferenceType().equals("com.l7tech.external.assertions.mqnative.MqNativeExternalReference")){
                        return GoidUpgradeMapper.mapId(EntityType.SSG_ACTIVE_CONNECTOR,instruction.getReferenceId()).toString();
                    }
                }
                return instruction.getReferenceId();
            }

            @Override
            public boolean acceptPolicyConflict( final String policyName, final String existingPolicyName, final String guid ) {
                Map<String,Object> properties = policyImportContext.getProperties();
                if ( !isFlagSet(properties, PROP_ACCEPT_POLICY_CONFLICT) && !isFlagSet(properties, PROP_FORCE) ) {
                    conflictingPolicies.add( new Triple<String,String,String>( policyName, existingPolicyName, guid) );
                }
                return true; // always continue to ensure we find all conflicting policies.
            }

            private boolean isFlagSet( final Map<String,Object> properties,
                                       final String propName ) {
                return properties != null &&
                        (properties.get( propName ) instanceof Boolean) &&
                        ((Boolean)properties.get( propName ));
            }

            @Override
            public boolean applyRenameToResolvedReferences(Collection<ExternalReference> references) throws PolicyImportCancelledException {
                final List<PolicyReferenceInstruction> instructions = policyImportContext.getPolicyReferenceInstructions();
                boolean success = true;
                if(instructions == null){
                    return success;
                }
                for (final PolicyReferenceInstruction instruction : instructions) {
                    if ( instruction.getPolicyReferenceInstructionType().equals(PolicyReferenceInstruction.PolicyReferenceInstructionType.RENAME) ) {
                        for ( final ExternalReference reference : references ) {
                            final String type = reference.getRefType();
                            final String id = getId(reference);
                            if (type.equals(instruction.getReferenceType()) && id.equals(handleReferenceId(instruction))) {
                                final String mapName = instruction.getMappedName();
                                if ( mapName != null && !mapName.trim().isEmpty() ) {
                                    success = reference.setLocalizeRename( mapName.trim() );
                                }
                            }
                        }
                    }
                }
                return success ;
            }
        };
    }

    private ExternalReferenceErrorListener buildErrorListener( final List<String> warnings ) {
        return new ExternalReferenceErrorListener(){
            @Override
            public void warning( final String title, final String message ) {
                warnings.add( message );
            }
        };
    }
}