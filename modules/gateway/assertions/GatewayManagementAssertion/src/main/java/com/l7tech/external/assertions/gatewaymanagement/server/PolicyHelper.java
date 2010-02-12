package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyDetail;
import com.l7tech.gateway.api.PolicyExportResult;
import com.l7tech.gateway.api.PolicyImportResult;
import com.l7tech.gateway.api.PolicyValidationContext;
import com.l7tech.gateway.api.PolicyValidationResult;
import com.l7tech.gateway.api.Resource;
import com.l7tech.gateway.api.ResourceSet;
import com.l7tech.gateway.api.impl.PolicyImportContext;
import com.l7tech.policy.AssertionLicense;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.PolicyReference;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.wsdl.Wsdl;

import javax.wsdl.WSDLException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Helper class for implementing policy import/export and validation.
 *
 * TODO [steve] implement policy import/export and validation (remove mocks)
 */
public class PolicyHelper {

    //- PUBLIC

    public PolicyHelper( final AssertionLicense licenseManager,
                         final PolicyValidator policyValidator,
                         final WspReader wspReader ) {
        this.licenseManager = licenseManager;
        this.policyValidator = policyValidator;
        this.wspReader = wspReader;
    }

    /**
     * Export the given policy.
     *
     * @param policy The policy to export.
     * @return The policy export result.
     */
    public PolicyExportResult exportPolicy( final Policy policy ) {
        if (true) {
            throw new ResourceFactory.ResourceAccessException("Policy export error");
        }
        PolicyExportResult per = ManagedObjectFactory.createPolicyExportResult();
        Resource resource = ManagedObjectFactory.createResource();
        resource.setType( "policyexport" );
        //resource.setSourceUrl( "urn:layer7tech.com:gateway:service:" + selectorMap.get("id"));
        resource.setContent(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:Export Version=\"3.0\"\n" +
                "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <exp:References>\n" +
                "        <IDProviderReference RefType=\"com.l7tech.console.policy.exporter.IdProviderReference\">\n" +
                "            <OID>-2</OID>\n" +
                "            <Name>Internal Identity Provider</Name>\n" +
                "            <Props>PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4gCjxqYXZhIHZlcnNpb249IjEu\n" +
                "Ni4wXzEyIiBjbGFzcz0iamF2YS5iZWFucy5YTUxEZWNvZGVyIj4gCiA8b2JqZWN0IGNsYXNzPSJq\n" +
                "YXZhLnV0aWwuSGFzaE1hcCI+IAogIDx2b2lkIG1ldGhvZD0icHV0Ij4gCiAgIDxzdHJpbmc+YWRt\n" +
                "aW5FbmFibGVkPC9zdHJpbmc+IAogICA8Ym9vbGVhbj50cnVlPC9ib29sZWFuPiAKICA8L3ZvaWQ+\n" +
                "IAogPC9vYmplY3Q+IAo8L2phdmE+IAo=</Props>\n" +
                "            <TypeVal>1</TypeVal>\n" +
                "        </IDProviderReference>\n" +
                "    </exp:References>\n" +
                "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "        <wsp:All wsp:Usage=\"Required\">\n" +
                "            <L7p:AuditAssertion>\n" +
                "                <L7p:SaveRequest booleanValue=\"true\"/>\n" +
                "                <L7p:SaveResponse booleanValue=\"true\"/>\n" +
                "            </L7p:AuditAssertion>\n" +
                "            <L7p:FaultLevel>\n" +
                "                <L7p:LevelInfo soapFaultLevel=\"included\">\n" +
                "                    <L7p:Level intValue=\"4\"/>\n" +
                "                </L7p:LevelInfo>\n" +
                "            </L7p:FaultLevel>\n" +
                "            <L7p:EncryptedUsernameToken/>\n" +
                "            <L7p:Authentication>\n" +
                "                <L7p:IdentityProviderOid longValue=\"-2\"/>\n" +
                "            </L7p:Authentication>\n" +
                "            <L7p:RequireWssTimestamp>\n" +
                "                <L7p:MaxExpiryMilliseconds intValue=\"3600000\"/>\n" +
                "            </L7p:RequireWssTimestamp>\n" +
                "            <L7p:RequireWssSignedElement/>\n" +
                "            <L7p:HardcodedResponse>\n" +
                "                <L7p:Base64ResponseBody stringValue=\"PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPHNvYXBlbnY6RW52ZWxvcGUKICAgIHhtbG5zOnNvYXBlbnY9Imh0dHA6Ly9zY2hlbWFzLnhtbHNvYXAub3JnL3NvYXAvZW52ZWxvcGUvIgogICAgeG1sbnM6eHNkPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYSIgeG1sbnM6eHNpPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYS1pbnN0YW5jZSI+CiAgICA8c29hcGVudjpIZWFkZXIvPgogICAgPHNvYXBlbnY6Qm9keT4KICAgICAgICA8dG5zOmxpc3RQcm9kdWN0cyB4bWxuczp0bnM9Imh0dHA6Ly93YXJlaG91c2UuYWNtZS5jb20vd3MiPgogICAgICAgICAgICA8dG5zOmRlbGF5PjA8L3RuczpkZWxheT4KICAgICAgICA8L3RuczpsaXN0UHJvZHVjdHM+CiAgICA8L3NvYXBlbnY6Qm9keT4KPC9zb2FwZW52OkVudmVsb3BlPgo=\"/>\n" +
                "            </L7p:HardcodedResponse>\n" +
                "        </wsp:All>\n" +
                "    </wsp:Policy>\n" +
                "</exp:Export>" );

        per.setResource( resource );

        return per;
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
        if (true) {
            throw new ResourceFactory.ResourceAccessException("Policy import error");
        }
        PolicyImportResult pir = ManagedObjectFactory.createPolicyImportResult();

        List<String> warnings = new ArrayList<String>();
        warnings.add( "Uknown User reference in policy id '123', login 'abc'" );
        warnings.add( "Uknown Group reference in policy id '123', name 'abc'" );

        List<PolicyImportResult.ImportedPolicyReference> references = new ArrayList<PolicyImportResult.ImportedPolicyReference>();
        references.add( ManagedObjectFactory.createImportedPolicyReference() );
        references.get( 0 ).setType( PolicyImportResult.ImportedPolicyReferenceType.MAPPED );
        references.get( 0 ).setReferenceType( "com.l7tech.console.policy.exporter.IdProviderReference" );
        references.get( 0 ).setReferenceId( "-2" );
        references.get( 0 ).setId( "123" );
        references.add( ManagedObjectFactory.createImportedPolicyReference() );
        references.get( 1 ).setType( PolicyImportResult.ImportedPolicyReferenceType.CREATED );
        references.get( 1 ).setReferenceType( "com.l7tech.console.policy.exporter.IncludedPolicyReference" );
        references.get( 1 ).setReferenceId( "1232" );
        references.get( 1 ).setId( "1234" );
        references.get( 1 ).setGuid( "abcedefasdfsfsdf" );

        pir.setWarnings( warnings );
        pir.setImportedPolicyReferences(references  );

        return pir;
        
    }

    /**
     * Validate the given / referenced policy and it's dependencies.
     *
     * TODO [steve] create / use a composite policy validator that does both server and default validation.
     *
     * @param validationContext The validation request context
     * @param resolver Resolver to locate referenced policy if not part of the request
     * @return The result of the policy validation
     * @throws ResourceFactory.InvalidResourceException If the given policy or data is invalid or the referenced policy is invalid
     * @throws ResourceFactory.ResourceNotFoundException If the referenced policy does not exists
     */
    public PolicyValidationResult validatePolicy( final PolicyValidationContext validationContext,
                                                  final PolicyResolver resolver ) throws ResourceFactory.InvalidResourceException, ResourceFactory.ResourceNotFoundException {
        final PolicyValidationContext policyValidationContext = validationContext != null ? validationContext : ManagedObjectFactory.createPolicyValidationContext();

        // Get request values
        PolicyType policyType = getPolicyType( policyValidationContext.getPolicyType() );
        boolean soap = isSoap( policyValidationContext.getProperties() );
        final Map<String, ResourceSet> resourceSetMap = resourceHelper.getResourceSetMap( policyValidationContext.getResourceSets() );
        Wsdl wsdl = getWsdl( resourceHelper.getResources( resourceSetMap, ResourceHelper.WSDL_TAG, false, null ) );
        Assertion assertion = getAssertion( resourceHelper.getResources( resourceSetMap, ResourceHelper.POLICY_TAG, false, null ));

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
            soap = policy.isSoap();
            wsdl = resolver.resolveWsdl();
        }

        // Run the validator
        final PolicyValidatorResult result;
        try {
            result = policyValidator.validate( assertion, policyType, wsdl, soap, licenseManager );
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new ResourceFactory.ResourceAccessException(e);
        }

        // Process the results, ensure duplicates are removed
        final Set<PolicyValidationResult.PolicyValidationMessage> messages = new LinkedHashSet<PolicyValidationResult.PolicyValidationMessage>();
        for ( final PolicyValidatorResult.Message message : result.getMessages() ) {
            final PolicyValidationResult.PolicyValidationMessage pvm = ManagedObjectFactory.createPolicyValidationMessage();
            pvm.setAssertionOrdinal( message.getAssertionOrdinal() );
            pvm.setLevel( message instanceof PolicyValidatorResult.Error ? "Error" : "Warning" );
            pvm.setMessage( message.getMessage() );

            final List<PolicyValidationResult.AssertionDetail> details = new ArrayList<PolicyValidationResult.AssertionDetail>();
            Assertion current = assertion;
            for ( final Integer position : message.getAssertionIndexPath() ) {
                final PolicyValidationResult.AssertionDetail detail = ManagedObjectFactory.createAssertionDetail();
                detail.setPosition( position );

                current = ((CompositeAssertion)current).getChildren().get( position );
                detail.setDescription( current.meta().<String>get( AssertionMetadata.WSP_EXTERNAL_NAME ) + " (" + current.meta().<String>get( AssertionMetadata.PALETTE_NODE_NAME ) + ")" );

                details.add( detail );
            }
            pvm.setAssertionDetails( details );

            messages.add( pvm );
        }

        final PolicyValidationResult pvr = ManagedObjectFactory.createPolicyValidationResult();
        pvr.setPolicyValidationMessages( new ArrayList<PolicyValidationResult.PolicyValidationMessage>(messages) );

        return pvr;
    }

    private boolean isSoap( final Map<String,Object> properties ) {
        boolean soap = false;

        if ( properties != null && properties.get( "soap" ) instanceof Boolean ) {
            soap = (Boolean) properties.get( "soap" );
        }

        return soap;
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
                default:
                    throw new ResourceFactory.ResourceAccessException("Unknown policy type '" + policyType + "'");    
            }
        } else {
            requestPolicyType = PolicyType.PRIVATE_SERVICE;
        }

        return requestPolicyType;
    }

    //- PUBLIC

    interface PolicyResolver {
        Policy resolve() throws ResourceFactory.ResourceNotFoundException;
        Wsdl resolveWsdl() throws ResourceFactory.ResourceNotFoundException;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( PolicyHelper.class.getName() );

    private final AssertionLicense licenseManager;
    private final PolicyValidator policyValidator;
    private final WspReader wspReader;
    private final ResourceHelper resourceHelper = new ResourceHelper();

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
                        "WSDL root resource type incorrect." );
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

    private void addPoliciesToPolicyReferenceAssertions( final Assertion rootAssertion,
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

}
