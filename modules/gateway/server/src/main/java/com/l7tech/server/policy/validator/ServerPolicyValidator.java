package com.l7tech.server.policy.validator;

import com.l7tech.common.io.ResourceReference;
import com.l7tech.common.io.SchemaUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.gateway.common.custom.ClassNameToEntitySerializer;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.entity.EntitiesResolver;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gateway.common.workqueue.WorkQueue;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.fed.VirtualGroup;
import com.l7tech.kerberos.KerberosClient;
import com.l7tech.kerberos.KerberosConfigException;
import com.l7tech.kerberos.KerberosException;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.credential.http.HttpCredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.ext.entity.CustomEntitySerializer;
import com.l7tech.policy.assertion.ext.store.KeyValueStore;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xmlsec.RequestWssKerberos;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.policy.validator.AbstractPolicyValidator;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.cassandra.CassandraConnectionEntityManager;
import com.l7tech.server.globalresources.ResourceEntryManager;
import com.l7tech.server.globalresources.ResourceEntrySchemaSourceResolver;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.policy.CustomKeyValueStoreManager;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.store.CustomKeyValueStoreImpl;
import com.l7tech.server.transport.jms.JmsEndpointManager;
import com.l7tech.server.util.EntityUseUtils;
import com.l7tech.server.workqueue.WorkQueueEntityManager;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.KeyStoreException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Performs server side policy validation.
 * <p/>
 * Rules checked:
 * <p/>
 * 1.     for each id assertion, check that the corresponding id exists
 * <p/>
 * 2.     for each id assertion that is from a fip. make sure the appropritate
 * credential source type is in same path
 * <p/>
 * 3.     for each JMS routing assertion, make sure referenced endpoint exists
 * <p/>
 * 4.     Various other rules, which have been added over time. See {@link ServerPolicyValidator#validateAssertion(Assertion, ServerPolicyValidator.PathContext, PolicyValidationContext, PolicyValidatorResult, AssertionPath)}
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 1, 2004<br/>
 */
public class ServerPolicyValidator extends AbstractPolicyValidator implements InitializingBean {

    private static final String WARNING_NOCERT = "This identity requires a certificate for authentication (authentication will always fail).";

    private static final int ID_NOT_EXIST = 0; // the corresponding id does not exist
    private static final int ID_EXIST = 1; // the corresponding id exists and is not fip
    private static final int ID_FIP = 2; // the corresponding id exists but in a fip provider (saml and wss authen only)
    private static final int ID_SAMLONLY = 3; // the corresponding id exists but belongs to saml only fip
    private static final int ID_X509ONLY = 4; // the corresponding id exists but belongs to X509 only fip
    private static final int ID_LDAP = 5; // the corresponding id exists in a ldap provider
    private static final int PROVIDER_NOT_EXIST = 6; // the corresponding provider does not exist any more
    private static final int ID_FIP_NOCERT = 7; // 2 & there is no certificate available to authenticate with
    private static final int ID_SAMLONLY_NOCERT = 8; // 3 & there is no certificate available to authenticate with
    private static final int ID_X509ONLY_NOCERT = 9; // 4 & there is no certificate available to authenticate with
    private static final int ID_ERROR = 10; // Error on ID provider

    private static final Logger logger = Logger.getLogger(ServerPolicyValidator.class.getName());

    /**
    * value is {@link #ID_NOT_EXIST}, {@link #ID_EXIST}, {@link #ID_FIP}, {@link #ID_SAMLONLY}, or {@link #ID_X509ONLY}
    */
    private final Map<IdentityAssertion, Integer> idAssertionStatusCache = new WeakHashMap<IdentityAssertion, Integer>();
    private final Map<EntityHeader, Entity> entityCache = new WeakHashMap<EntityHeader, Entity>();

    private JmsEndpointManager jmsEndpointManager;
    private IdentityProviderFactory identityProviderFactory;
    private ResourceEntryManager resourceEntryManager;
    private ClientCertManager clientCertManager;
    private EntityFinder entityFinder;
    private SsgKeyStoreManager ssgKeyStoreManager;
    private JdbcConnectionManager jdbcConnectionManager;
    private Config config;
    private KeyValueStore keyValueStore;
    private CustomAssertionsRegistrar customAssertionsRegistrar;
    private CassandraConnectionEntityManager cassandraEntityManager;
    private WorkQueueEntityManager workQueueEntityManager;

    public ServerPolicyValidator( final GuidBasedEntityManager<Policy> policyFinder,
                                  final PolicyPathBuilderFactory pathBuilderFactory ) {
        super(policyFinder, pathBuilderFactory);
    }

    @Override
    protected void doValidation( final Assertion assertion,
                                 final PolicyValidationContext pvc,
                                 final AssertionLicense assertionLicense,
                                 final PolicyValidatorResult result ) throws InterruptedException {
        prepareAssertionBean(assertion, result);
        if ( pvc.getPolicyType().isServicePolicy() ) {
            // Full path validation
            super.doValidation( assertion, pvc, assertionLicense, result );
        } else {
            // Assertion validation only
            try {
                final Iterator<Assertion> assertions = assertion.preorderIterator( getAssertionTranslator() );
                while ( assertions.hasNext() ) {
                    final Assertion as = assertions.next();
                    if (as.isEnabled()) validateAssertion( as, null, pvc, result, null );
                }
            } catch ( PolicyAssertionException e) {
                result.addError(new PolicyValidatorResult.Error(e.getAssertion(), e.getMessage(), e));
            }
        }
    }

    private void prepareAssertionBean(Assertion assertion, PolicyValidatorResult result) {
        if (assertion instanceof UsesEntitiesAtDesignTime) {
            UsesEntitiesAtDesignTime entityUser = (UsesEntitiesAtDesignTime) assertion;
            EntityHeader[] headers = entityUser.getEntitiesUsedAtDesignTime();
            if (headers != null) {
                for (EntityHeader header : headers) {
                    if (entityUser.needsProvideEntity(header)) {
                        try {
                            Entity entity = entityFinder.find(header);
                            if (entity == null) {
                                result.addError(new PolicyValidatorResult.Error(assertion, "No " + header.getType() + " found with ID " + header.getStrId(), null));
                            } else {
                                entityUser.provideEntity(header, entity);
                            }
                        } catch (FindException e) {
                            result.addError(new PolicyValidatorResult.Error(assertion, "Unable to find " + header.getType() + " found with ID " + header.getStrId(), e));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void validatePath( final AssertionPath path,
                              final PolicyValidationContext pvc,
                              final AssertionLicense assertionLicense,
                              final PolicyValidatorResult result ) {
        Assertion[] ass = path.getPath();
        PathContext pathContext = new PathContext();
        for (Assertion as : ass) {
            if (as.isEnabled()) validateAssertion( as, pathContext, pvc, result, path );
        }
    }

    /**
     * Validate the specific assertion.
     * Precondition: the assertion "a" must have been pre-checked to be enabled.
     * @see {@link com.l7tech.server.policy.validator.ServerPolicyValidator#validatePath(com.l7tech.policy.AssertionPath, com.l7tech.policy.validator.PolicyValidationContext, com.l7tech.policy.AssertionLicense, com.l7tech.policy.PolicyValidatorResult)}
     * @param assertion: the assertion to be validated.
     * @param pathContext: the assertion path context (May be null)
     * @param result: storing the validation result.
     * @param path: the assertion path containing the given assertion (May be null)
     */
    @SuppressWarnings(value = "fallthrough")
    private void validateAssertion( final Assertion assertion,
                                    final PathContext pathContext,
                                    final PolicyValidationContext pvc,
                                    final PolicyValidatorResult result,
                                    final AssertionPath path ) {
        final String targetName = AssertionUtils.getTargetName(assertion);
        if ( assertion instanceof IdentityAssertion) {
            final IdentityAssertion identityAssertion = (IdentityAssertion) assertion;
            int idStatus = getIdentityStatus(identityAssertion);
            switch (idStatus) {
                case PROVIDER_NOT_EXIST:
                    result.addError(new PolicyValidatorResult.Error( assertion,
                            "The corresponding identity provider does not exist any more. " +
                      "Please remove the assertion from the policy.",
                      null));
                    break;
                case ID_NOT_EXIST:
                    result.addError(new PolicyValidatorResult.Error( assertion,
                            "The corresponding identity cannot be found. " +
                      "Please remove the assertion from the policy.",
                      null));
                    break;
                case ID_FIP_NOCERT:
                    result.addWarning(new PolicyValidatorResult.Warning( assertion, WARNING_NOCERT, null));
                    // fall through to the rest of the samlonly handling
                case ID_FIP:
                    if ( pathContext != null ) {
                        boolean foundUsableCredSource = false;
                        for (Assertion credSrc : pathContext.getCredentialSourceAssertions(targetName)) {
                            if (credSrc instanceof RequireWssSaml ||
                                credSrc instanceof RequireWssX509Cert ||
                                credSrc instanceof SecureConversation ||
                                credSrc instanceof SslAssertion)
                            {
                                foundUsableCredSource = true;
                                break;
                            }
                        }
                        if(!foundUsableCredSource) {
                            result.addError(new PolicyValidatorResult.Error( assertion,
                                    "This identity cannot authenticate with the " +
                              "type of credential " +
                              "source specified.",
                              null));
                        }
                    }
                    break;
                case ID_SAMLONLY_NOCERT:
                    result.addWarning(new PolicyValidatorResult.Warning( assertion, WARNING_NOCERT, null));
                    // fall through to the rest of the samlonly handling
                case ID_SAMLONLY:
                    if ( pathContext != null ) {
                        boolean foundUsableCredSource = false;
                        for (Assertion credSrc : pathContext.getCredentialSourceAssertions(targetName)) {
                            if (credSrc instanceof RequireWssSaml || credSrc instanceof SslAssertion) {
                                foundUsableCredSource = true;
                                break;
                            }
                        }
                        if(!foundUsableCredSource) {
                            result.addError(new PolicyValidatorResult.Error( assertion,
                                    "This identity can only authenticate with a SAML token or SSL Client Certificate but another type of credential source is specified.",
                              null));
                        }
                    }
                    break;
                case ID_X509ONLY_NOCERT:
                    result.addWarning(new PolicyValidatorResult.Warning( assertion, WARNING_NOCERT, null));
                    // fall through to the rest of the x509only handling
                case ID_X509ONLY:
                    if ( pathContext != null ) {
                        boolean foundUsableCredSource = false;
                        for (Assertion credSrc : pathContext.getCredentialSourceAssertions(targetName)) {
                            if (isX509CredentialSource(credSrc)) {
                                foundUsableCredSource = true;
                                break;
                            }
                        }
                        if(!foundUsableCredSource) {
                            result.addError(new PolicyValidatorResult.Error( assertion,
                                    "This identity can only authenticate using its client cert. The specified type of credential source is not supported by that user.",
                              null));
                        }
                    }
                    break;
                case ID_LDAP:
                    if ( pathContext != null && identityAssertion instanceof SpecificUser) {
                        final Collection<Assertion> credentaiSourceAssertions = pathContext.getCredentialSourceAssertions(targetName);
                        //were looking for a digest assertion, which we cannot know will work with an external ldap. It might but we can't tell.
                        for (Assertion aCredAssertion : credentaiSourceAssertions) {
                            if(aCredAssertion instanceof HttpCredentialSourceAssertion){
                                HttpCredentialSourceAssertion httpCredAssertion = (HttpCredentialSourceAssertion) aCredAssertion;
                                if(httpCredAssertion.isDigestSource()){
                                    result.addWarning(new PolicyValidatorResult.Warning( assertion,
                                            "This identity may not be able to authenticate with the type of credential source specified.",
                                      null));
                                    break;
                                }
                            }
                        }
                    }
                    break;
                case ID_ERROR:
                    result.addWarning(new PolicyValidatorResult.Warning( assertion,
                            "This identity cannot be validated at this time (identity provider error)",
                      null));
                    break;
            }
        } else if ( assertion.isCredentialSource()) {
            if ( pathContext != null ) {
                pathContext.seenCredentialSource(targetName, assertion );
            }
        } else if ( assertion instanceof JmsRoutingAssertion) {
            JmsRoutingAssertion jmsass = (JmsRoutingAssertion) assertion;
            if (jmsass.getEndpointOid() != null) {
                Goid endpointid = jmsass.getEndpointOid();
                boolean jmsEndpointDefinedOk = false;
                try {
                    JmsEndpoint routedRequestEndpoint = jmsEndpointManager.findByPrimaryKey(endpointid);
                    jmsEndpointDefinedOk = routedRequestEndpoint != null;
                } catch (FindException e) {
                    logger.log(Level.FINE, "Error fetching endpoint " + endpointid + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
                if (!jmsEndpointDefinedOk) {
                    result.addError(new PolicyValidatorResult.Error( assertion,
                            "This routing assertion refers to a JMS " +
                      "endpoint that cannot be found on this system.",
                      null));
                }
            }
        } else if ( assertion instanceof SchemaValidation) {

            SchemaValidation svass = (SchemaValidation) assertion;
            AssertionResourceInfo ri = svass.getResourceInfo();
            if (ri instanceof StaticResourceInfo) { // check for unresolved imports
                validateSchemaValidation( assertion, path, (StaticResourceInfo)ri, result);
            } else if (ri instanceof GlobalResourceInfo) { // check for broken ref
                boolean res = checkGlobalSchemaExists((GlobalResourceInfo)ri);
                if (!res) {
                    result.addError(new PolicyValidatorResult.Error( assertion,
                            "This assertion refers to a global schema that no longer exists.", null));
                }
            }
        } else if ( assertion instanceof UnknownAssertion) {
            UnknownAssertion ua = (UnknownAssertion) assertion;

            String message = "Unknown assertion {0}; all requests to this service will fail.";
            String detail = "";
            Throwable cause = ua.cause();
            if(cause instanceof ClassNotFoundException) {
                String className = cause.getMessage();
                detail = " [" + (className.substring(className.lastIndexOf('.')+1)) + "]";
            }
            result.addError(new PolicyValidatorResult.Error( assertion, MessageFormat.format(message, detail), null));
        } else if ( assertion instanceof HttpFormPost) {
            HttpFormPost httpFormPost = (HttpFormPost) assertion;

            StringBuffer contentTypeBuffer = new StringBuffer();
            HttpFormPost.FieldInfo[] fieldInfos = httpFormPost.getFieldInfos();
            for (HttpFormPost.FieldInfo fieldInfo : fieldInfos) {
                String contentType = fieldInfo.getContentType();
                try {
                    ContentTypeHeader cth = ContentTypeHeader.parseValue(contentType);
                    String encoding = cth.getParam("charset");
                    if (encoding != null && !Charset.isSupported(encoding)) {
                        throw new IllegalStateException();
                    }
                }
                catch (Exception e) {
                    if (contentTypeBuffer.length() > 0) contentTypeBuffer.append(", ");
                    contentTypeBuffer.append("'");
                    contentTypeBuffer.append(contentType);
                    contentTypeBuffer.append("'");
                }
            }

            if (contentTypeBuffer.length() > 0) {
                String msg = "Invalid MIME Content-Type(s): " + contentTypeBuffer.toString();
                result.addError(new PolicyValidatorResult.Error( assertion, msg, null));
            }
        }

        // not else-if since this is also a credential source
        if ( assertion instanceof RequestWssKerberos) {
            try {
                KerberosClient.validateKerberosPrincipals();
            }
            catch(KerberosConfigException kce) {
                result.addError(new PolicyValidatorResult.Error( assertion,
                        "Kerberos is not configured on the Gateway.",
                  null));
            }
            catch(KerberosException ke) {
                result.addError(new PolicyValidatorResult.Error( assertion,
                        "Gateway Kerberos configuration is invalid.",
                  null));
            }
        }
        
        if ( assertion instanceof HttpRoutingAssertion ) {
            HttpRoutingAssertion httpRoutingAssertion = (HttpRoutingAssertion) assertion;
            if (httpRoutingAssertion.isKrbDelegatedAuthentication() || httpRoutingAssertion.isKrbUseGatewayKeytab()) {
                try {
                    KerberosClient.validateKerberosPrincipals();
                }
                catch(KerberosConfigException kce) {
                    result.addError(new PolicyValidatorResult.Error( assertion,
                            "Kerberos is not configured on the Gateway.",
                            null));
                }
                catch(KerberosException ke) {
                    result.addError(new PolicyValidatorResult.Error( assertion,
                            "Gateway Kerberos configuration is invalid.",
                            null));
                }
            }
        }

        if (!(assertion instanceof IdentityAssertion || assertion instanceof JmsRoutingAssertion)) {
            // getEntitiesUsed works only for UsesEntities and CustomAssertionHolder
            final EntitiesResolver entitiesResolver = EntitiesResolver
                    .builder()
                    .keyValueStore(keyValueStore)
                    .classNameToSerializer(new ClassNameToEntitySerializer() {
                        @Override
                        public CustomEntitySerializer getSerializer(final String className) {
                            return customAssertionsRegistrar.getExternalEntitySerializer(className);
                        }
                    })
                    .build();
            for (EntityHeader header : entitiesResolver.getEntitiesUsed(assertion)) {
                Entity entity = null;
                FindException thrown = null;
                try {
                    synchronized(entityCache) {
                        entity = entityCache.get(header);
                    }

                    if (entity == null) {
                        entity = entityFinder.find(header);
                        if (entity != null) {
                            synchronized(entityCache) {
                                entityCache.put(header, entity);
                            }
                        }
                    }
                } catch (FindException e) {
                    thrown = e;
                }
                if (entity == null) {
                    boolean err = true;

                    if ( assertion instanceof EncapsulatedAssertion ) {
                        EncapsulatedAssertion encass = (EncapsulatedAssertion) assertion;
                        if ( encass.isNoOpIfConfigMissing() && EntityType.ENCAPSULATED_ASSERTION.equals( header.getType() ) ) {
                            // Explicitly declared as not a fatal error for this encass call site to refer to a nonexistent encass config
                            err = false;
                        }
                    }

                    if ( err ) {
                        result.addError( new PolicyValidatorResult.Error( assertion,
                                "Assertion refers to a " +
                                        EntitiesResolver.getEntityTypeName(
                                                assertion,
                                                header.getType(),
                                                new Functions.Binary<String, UsesEntities, EntityType>() {
                                                    @Override
                                                    public String call(
                                                            @NotNull final UsesEntities usesEntities,
                                                            @NotNull EntityType entityType
                                                    ) {
                                                        return EntityUseUtils.getTypeName( usesEntities, entityType );
                                                    }
                                                }
                                        ) +
                                        " that cannot be located on this system", thrown ) );
                    }
                }
            }
        }

        if ( assertion instanceof PrivateKeyable) {
            checkPrivateKey((PrivateKeyable) assertion, path, pvc, result);
        }

        if ( assertion instanceof Connectionable ) {
            checkConnection((Connectionable) assertion, path, result);
        }

        if ( assertion instanceof HttpDigest) {
            checkHttpDigestConfiguration((HttpDigest) assertion, path, result);
        }

        if ( assertion instanceof CassandraConnectionable ) {
            checkCassandraConnection((CassandraConnectionable) assertion, path, result);
        }
    }

    private boolean checkGlobalSchemaExists(GlobalResourceInfo globalResourceInfo) {
        // look for the presence of the schema
        String sId = globalResourceInfo.getId();
        ResourceEntryHeader res = null;
        try {
            res = resourceEntryManager.findHeaderByUriAndType(sId, ResourceType.XML_SCHEMA);
        } catch (FindException e) {
            logger.log(Level.INFO, "error looking for schema: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
        return res != null;
    }

    private void validateSchemaValidation(Assertion a, AssertionPath ap, StaticResourceInfo sri, PolicyValidatorResult r) {
        try {
            final InputSource inputSource = new InputSource();
            inputSource.setSystemId( sri.getOriginalUrl()!=null ? sri.getOriginalUrl() : "urn:uuid:" + UUID.randomUUID().toString() );
            inputSource.setCharacterStream( new StringReader(sri.getDocument()) );

            final Collection<ResourceReference> references = SchemaUtil.getDependencies( inputSource, new ResourceEntrySchemaSourceResolver(resourceEntryManager) );

            if (!references.isEmpty()) {
                final ArrayList<String> unresolvedReferencesList = new ArrayList<String>();

                for ( final ResourceReference reference : references ) {
                    try {
                        boolean resolved = false;

                        // By unresolved URI
                        if ( reference.getUri() != null && resourceEntryManager.findHeaderByUriAndType(reference.getUri(), null) != null ) {
                            resolved = true;
                        }

                        // By resolved URI
                        if ( !resolved && reference.getBaseUri() != null && reference.getUri() != null ) {
                            try {
                                final String resolvedUri = new URI(reference.getBaseUri()).resolve(reference.getUri()).toString();
                                if ( resourceEntryManager.findHeaderByUriAndType(resolvedUri, null) != null ) {
                                    resolved = true;
                                }
                            } catch ( URISyntaxException e ) {
                                logger.info( "Unable to resolve schema dependency URI '"+reference.getUri()+"' against '"+reference.getBaseUri()+"': " + ExceptionUtils.getMessage(e) );
                            } catch ( IllegalArgumentException e ) {
                                logger.info( "Unable to resolve schema dependency URI '"+reference.getUri()+"' against '"+reference.getBaseUri()+"': " + ExceptionUtils.getMessage(e) );
                            }
                        }

                        // By target namespace
                        if ( !resolved && reference.hasTargetNamespace() && !resourceEntryManager.findHeadersByTNS(reference.getTargetNamespace()).isEmpty()) {
                            resolved = true;
                        }

                        // By public identifier
                        if ( !resolved && reference.getPublicIdentifier() != null && !resourceEntryManager.findHeadersByPublicIdentifier(reference.getPublicIdentifier()).isEmpty() ) {
                            resolved = true;    
                        }

                        if ( !resolved ) {
                            if (reference.getUri() != null) {
                                unresolvedReferencesList.add( reference.getUri() );
                            } else {
                                unresolvedReferencesList.add( reference.getTargetNamespace()!=null ? reference.getTargetNamespace() : "<no namespace>" );
                            }
                        }
                    } catch (ObjectModelException e) {
                        logger.log(Level.SEVERE, "cannot get schema: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    }
                }

                if (!unresolvedReferencesList.isEmpty()) {
                    StringBuffer msg = new StringBuffer("The schema validation assertion contains unresolved " +
                            "schema dependencies: ");
                    for (Iterator iterator = unresolvedReferencesList.iterator(); iterator.hasNext();) {
                        msg.append(iterator.next());
                        if (iterator.hasNext()) msg.append(", ");
                    }
                    msg.append(".");
                    r.addError(new PolicyValidatorResult.Error(a, msg.toString(), null));
                }
            }
        } catch (SAXException e) {
            logger.log(Level.INFO, "cannot parse xml from schema validation assertion: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            r.addError(new PolicyValidatorResult.Error(a,
                    "This schema validation assertion does not appear " +
                                                               "to contain a well-formed xml schema.",
                                                       null));
        } catch ( IOException e ) {
            logger.log(Level.INFO, "cannot parse xml from schema validation assertion: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            r.addError(new PolicyValidatorResult.Error(a,
                    "This schema validation assertion does not appear " +
                                                               "to contain a well-formed xml schema.",
                                                       null));
        }
    }

    protected void checkPrivateKey(PrivateKeyable a, AssertionPath ap, PolicyValidationContext pvc, PolicyValidatorResult r) {
        if (!a.isUsesDefaultKeyStore()) {
            boolean found = false;
            try {
                Goid keystoreId = a.getNonDefaultKeystoreId();
                String keyAlias = a.getKeyAlias();
                SsgKeyEntry foundKey = ssgKeyStoreManager.lookupKeyByKeyAlias(keyAlias, keystoreId);
                if (foundKey != null) {
                    found = true;
                    Goid foundKeystoreId = foundKey.getKeystoreId();
                    if (!Goid.equals(foundKey.getKeystoreId(), keystoreId)) {
                        SsgKeyFinder ks = ssgKeyStoreManager.findByPrimaryKey(foundKeystoreId);
                        String name = ks != null ? ks.getName() : ("id:" + foundKeystoreId);
                        r.addWarning(new PolicyValidatorResult.Warning((Assertion)a,
                                "This assertion refers to a Private Key with alias '" + keyAlias + "' in a keystore with ID '" + keystoreId + "'.  " +
                                                                       "No keystore with that ID is present on this Gateway, but the system has found " +
                                                                       "a Private Key with a matching alias in the keystore '" + name + "' and will use that.",
                                                                       null));
                    }

                    boolean frag = isPolicyFragement(pvc);
                    boolean avp = isAuditViewerPolicy(pvc);
                    if (!frag && foundKey.isRestrictedAccess() && !avp) {
                        // TODO replace with more generic message as soon as there is a restricted access key other than the audit viewer key
                        r.addError(new PolicyValidatorResult.Error((Assertion)a,
                                "This assertion refers to the audit viewer private key, which can only be used from within the audit viewer internal policy.", null));
                    }
                }
            } catch (ObjectModelException e) {
                logger.log(Level.WARNING, "error looking for private key: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            } catch (KeyStoreException e) {
                logger.log(Level.WARNING, "error looking for private key: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            } catch (Exception e) {
                logger.log(Level.WARNING, "error looking for private key: " + ExceptionUtils.getMessage(e), e);
            }
            if (!found) {
                r.addError(new PolicyValidatorResult.Error((Assertion)a,
                        "This assertion refers to a Private Key which cannot " +
                                                           "be found on this Gateway",
                                                           null));
            }
        }
    }

    private static boolean isPolicyFragement(PolicyValidationContext pvc) {
        PolicyType type = pvc.getPolicyType();
        return type != null && type.isPolicyFragment();
    }

    private static boolean isAuditViewerPolicy(PolicyValidationContext pvc) {
        return PolicyType.TAG_AUDIT_VIEWER.equals(pvc.getPolicyInternalTag());
    }

    protected void checkConnection(final Connectionable connectionable, final AssertionPath ap, final PolicyValidatorResult r) {
        final String name = connectionable.getConnectionName();
        if ( name != null && Syntax.getReferencedNames(name).length == 0) {
            if(connectionable instanceof JdbcConnectionable) {
                checkJdbcConnection((JdbcConnectionable) connectionable, ap, r);
            }
            else if(connectionable instanceof CassandraConnection) {
                checkCassandraConnection((CassandraConnectionable) connectionable, ap, r);
            }
        }
    }

    private void checkJdbcConnection( final JdbcConnectionable jdbcConnectionable,
                                        final AssertionPath ap,
                                        final PolicyValidatorResult r ) {
        final String name = jdbcConnectionable.getConnectionName();
        try {
            JdbcConnection connection = jdbcConnectionManager.getJdbcConnection( name );
            if ( connection == null ) {
                r.addError(new PolicyValidatorResult.Error( (Assertion)jdbcConnectionable,
                        "Assertion refers to the " + EntityType.JDBC_CONNECTION.getName() +" '"+name+"' which cannot be located on this system.", null));

            }
        } catch ( FindException e ) {
            logger.log(Level.WARNING, "Error looking for JDBC connection: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void checkWorkQueue(final WorkQueueable workQueueable,
                                final AssertionPath ap,
                                final PolicyValidatorResult r) {
        final String name = workQueueable.getWorkQueueName();
        try {
            WorkQueue wq = workQueueEntityManager.getWorkQueueEntity(name);
            if (wq == null) {
                r.addError(new PolicyValidatorResult.Error((Assertion) workQueueable,
                        "Assertion refers to the " + EntityType.WORK_QUEUE.getName() + " '" + name + "' which cannot be located on this system.", null));

            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "Error looking for work queue: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void checkHttpDigestConfiguration(final HttpDigest httpDigestAssertion,
                                              final AssertionPath ap,
                                              final PolicyValidatorResult r) {

        final boolean isDigestEnabled = config.getBooleanProperty("httpDigest.enable", false);
        if ( !isDigestEnabled ) {
            r.addWarning(new PolicyValidatorResult.Warning(httpDigestAssertion,
                    "Credentials gathered by this assertion are unlikely to be authenticated by any identity provider because no provider is currently configured to store MD5 password hashes.", null));
        }
    }

    private void checkCassandraConnection( final CassandraConnectionable cassandraConnectionable,
                                           final AssertionPath ap,
                                           final PolicyValidatorResult r ) {
        final String name = cassandraConnectionable.getConnectionName();
        try {
            CassandraConnection connection = cassandraEntityManager.getCassandraConnectionEntity( name );
            if ( connection == null ) {
                r.addError(new PolicyValidatorResult.Error( (Assertion)cassandraConnectionable,
                        "Assertion refers to the " + EntityType.CASSANDRA_CONFIGURATION.getName() +" '"+name+"' which cannot be located on this system.", null));

            }
        } catch ( FindException e ) {
            logger.log(Level.WARNING, "Error looking for Cassandra Connection connection: " + ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * This is synchronized just in case, i don't expect this to be invoked from multiple simultaneous threads.
     * Add more return values as needed.
     * todo: consider refactoring into some sort of status class 'IdentityStatus' instead of returning codes. - em
     *
     * @return see ID_NOT_EXIST, ID_EXIST, or ID_EXIST_BUT_SAMLONLY
     */
    private synchronized int getIdentityStatus(IdentityAssertion identityAssertion) {
        // look in cache first
        Integer output = idAssertionStatusCache.get(identityAssertion);
        if (output == null) {
            try {
// get provider
                IdentityProvider prov = identityProviderFactory.getProvider(identityAssertion.getIdentityProviderOid());
                if (prov == null) {
                    idAssertionStatusCache.put(identityAssertion, PROVIDER_NOT_EXIST);
                    return PROVIDER_NOT_EXIST;
                }
// check if user or group exists
                boolean idexists = false;
                boolean idhascert = false;
                if (identityAssertion instanceof SpecificUser) {
                    SpecificUser su = (SpecificUser)identityAssertion;
                    final String uid = su.getUserUid();
                    User u;
                    if (uid == null) {
                        u = prov.getUserManager().findByLogin(su.getUserLogin());
                    } else {
                        u = prov.getUserManager().findByPrimaryKey(uid);
                    }
                    if (u != null) {
                        idexists = true;
                        idhascert = clientCertManager.getUserCert(u)!=null;
                    }
                } else if (identityAssertion instanceof MemberOfGroup) {
                    MemberOfGroup mog = (MemberOfGroup)identityAssertion;
                    Group g;
                    final String gid = mog.getGroupId();
                    if (gid == null) {
                        g = prov.getGroupManager().findByName(mog.getGroupName());
                    } else {
                        g = prov.getGroupManager().findByPrimaryKey(gid);
                    }
                    if (g != null) {
                        idexists = true;
                        if (!(g instanceof VirtualGroup)) {
                            idhascert = true;
                        }
                    }
                } else if (identityAssertion instanceof AuthenticationAssertion) {
                    idexists = true; // We don't care who you are
                    try {
                        prov.test(true, null, null);
                    } catch (InvalidIdProviderCfgException iipce) {
                        throw new FindException("Error testing provider.", iipce);
                    }
                } else {
                    throw new RuntimeException("Type not supported " + identityAssertion.getClass().getName());
                }
                if (!idexists) {
                    output = ID_NOT_EXIST;
                } else {
// check for special fip values
                    int val = ID_EXIST;
                    if (IdentityProviderType.is(prov, IdentityProviderType.FEDERATED)) {
                        val = ID_FIP;
                        FederatedIdentityProviderConfig cfg = (FederatedIdentityProviderConfig)prov.getConfig();
                        boolean certmissing = false;
                        if (cfg.getTrustedCertGoids().length == 0 && !idhascert) {
                            certmissing = true;
                        }
                        if (cfg.isSamlSupported() && !cfg.isX509Supported()) {
                            val = certmissing ? ID_SAMLONLY_NOCERT : ID_SAMLONLY;
                        } else if (!cfg.isSamlSupported() && cfg.isX509Supported()) {
                            val = certmissing ? ID_X509ONLY_NOCERT : ID_X509ONLY;
                        } else if (certmissing) {
                            val = ID_FIP_NOCERT;
                        }
                    } else if (IdentityProviderType.is(prov, IdentityProviderType.LDAP)) {
                        val = ID_LDAP;
                    }
                    output = val;
                }
                idAssertionStatusCache.put(identityAssertion, output);
            } catch (FindException e) {
                logger.log(Level.WARNING, "problem retrieving identity: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                output = ID_ERROR;
                idAssertionStatusCache.put(identityAssertion, output);
            }
        }
        return output;
    }

    public void setJmsEndpointManager(JmsEndpointManager jmsEndpointManager) {
        this.jmsEndpointManager = jmsEndpointManager;
    }

    public void setIdentityProviderFactory(IdentityProviderFactory identityProviderFactory) {
        this.identityProviderFactory = identityProviderFactory;
    }

    public void setSsgKeyStoreManager(SsgKeyStoreManager ssgKeyStoreManager) {
        this.ssgKeyStoreManager = ssgKeyStoreManager;
    }

    public void setResourceEntryManager(ResourceEntryManager resourceEntryManager) {
        this.resourceEntryManager = resourceEntryManager;
    }

    public void setClientCertManager(ClientCertManager clientCertManager) {
        this.clientCertManager = clientCertManager;
    }

    public void setEntityFinder(EntityFinder entityFinder) {
        this.entityFinder = entityFinder;
    }

    public void setJdbcConnectionManager( final JdbcConnectionManager jdbcConnectionManager ) {
        this.jdbcConnectionManager = jdbcConnectionManager;
    }

    public void setCassandraEntityManager(CassandraConnectionEntityManager cassandraEntityManager) {
        this.cassandraEntityManager = cassandraEntityManager;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public void setKeyValueStoreManager(@NotNull final CustomKeyValueStoreManager keyValueStoreManager) {
        this.keyValueStore = new CustomKeyValueStoreImpl(keyValueStoreManager);
    }

    public void setCustomAssertionsRegistrar(@NotNull final CustomAssertionsRegistrar customAssertionsRegistrar) {
        this.customAssertionsRegistrar = customAssertionsRegistrar;
    }

    public void setWorkQueueEntityManager(WorkQueueEntityManager workQueueEntityManager) {
        this.workQueueEntityManager = workQueueEntityManager;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (jmsEndpointManager == null) {
            throw new IllegalArgumentException("JMS Endpoint manager is required");
        }

        if (identityProviderFactory == null) {
            throw new IllegalArgumentException("Identity Provider Factory is required");
        }

        if (ssgKeyStoreManager == null) {
            throw new IllegalArgumentException("KeyStore Manager is required");
        }

        if (resourceEntryManager == null) {
            throw new IllegalArgumentException("Resource Entry Manager is required");
        }

        if (clientCertManager == null) {
            throw new IllegalArgumentException("Client Cert Manger is required");
        }

        if (entityFinder == null) {
            throw new IllegalArgumentException("EntityFinder is required");
        }

        if (jdbcConnectionManager == null) {
            throw new IllegalArgumentException("JDBC Connection Manager is required");
        }

        if (keyValueStore == null) {
            throw new IllegalArgumentException("Custom key value store is required");
        }

        if (customAssertionsRegistrar == null) {
            throw new IllegalArgumentException("Custom assertions registrar is required");
        }
    }

    private static class PathContext {
        private final Map<String,Collection<Assertion>> credentialSources = new HashMap<String,Collection<Assertion>>();

        void seenCredentialSource( final String targetName, final Assertion assertion ) {
            Collection<Assertion> assertions = credentialSources.get(targetName.toLowerCase());
            if ( assertions == null ) {
                assertions = new ArrayList<Assertion>();
                credentialSources.put(targetName.toLowerCase(), assertions);
            }
            assertions.add( assertion );
        }

        Collection<Assertion> getCredentialSourceAssertions( final String targetName ) {
            Collection<Assertion> credentialAssertions = credentialSources.get(targetName.toLowerCase());
            if ( credentialAssertions == null ) {
                credentialAssertions = Collections.emptyList();
            }
            return credentialAssertions;
        }

        boolean contains( final String targetName, final Class<? extends Assertion> clz ) {
            Collection<Assertion> assertions = credentialSources.get(targetName.toLowerCase());
            if ( assertions != null ) {
                for (Assertion ass : assertions) {
                    if (ass.getClass().equals(clz)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
