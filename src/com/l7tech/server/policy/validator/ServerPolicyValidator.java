package com.l7tech.server.policy.validator;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.security.kerberos.KerberosClient;
import com.l7tech.common.security.kerberos.KerberosConfigException;
import com.l7tech.common.security.kerberos.KerberosException;
import com.l7tech.common.security.keystore.SsgKeyEntry;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.policy.PolicyType;
import com.l7tech.common.policy.Policy;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.User;
import com.l7tech.identity.InvalidIdProviderCfgException;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.fed.VirtualGroup;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xmlsec.RequestWssKerberos;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.communityschemas.SchemaEntryManager;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.transport.jms.JmsEndpointManager;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.nio.charset.Charset;
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
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 1, 2004<br/>
 * $Id$<br/>
 */
public class ServerPolicyValidator extends PolicyValidator implements InitializingBean {

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
    private SchemaEntryManager schemaEntryManager;
    private ClientCertManager clientCertManager;
    private EntityFinder entityFinder;
    private SsgKeyStoreManager ssgKeyStoreManager;

    public ServerPolicyValidator(ReadOnlyEntityManager<Policy, EntityHeader> policyFinder, PolicyPathBuilderFactory pathBuilderFactory) {
        super(policyFinder, pathBuilderFactory);
    }

    public void validatePath(AssertionPath ap, PolicyType policyType, Wsdl wsdl, boolean soap, AssertionLicense assertionLicense, PolicyValidatorResult r) {
        Assertion[] ass = ap.getPath();
        PathContext pathContext = new PathContext();
        for (Assertion as : ass) {
            validateAssertion(as, pathContext, r, ap);
        }
    }

    @SuppressWarnings(value = "fallthrough")
    private void validateAssertion(Assertion a, PathContext pathContext, PolicyValidatorResult r, AssertionPath ap) {
        if (a instanceof IdentityAssertion) {
            final IdentityAssertion identityAssertion = (IdentityAssertion)a;
            int idStatus = getIdentityStatus(identityAssertion);
            switch (idStatus) {
                case PROVIDER_NOT_EXIST:
                    r.addError(new PolicyValidatorResult.Error(a,
                      ap,
                      "The corresponding identity provider does not exist any more. " +
                      "Please remove the assertion from the policy.",
                      null));
                    break;
                case ID_NOT_EXIST:
                    r.addError(new PolicyValidatorResult.Error(a,
                      ap,
                      "The corresponding identity cannot be found. " +
                      "Please remove the assertion from the policy.",
                      null));
                    break;
                case ID_FIP_NOCERT:
                    r.addWarning(new PolicyValidatorResult.Warning(a, ap, WARNING_NOCERT, null));
                    // fall through to the rest of the samlonly handling
                case ID_FIP:
                    { // scope
                        boolean foundUsableCredSource = false;
                        for (Assertion credSrc : pathContext.credentialSources) {
                            if (credSrc instanceof RequestWssSaml ||
                                credSrc instanceof RequestWssX509Cert ||
                                credSrc instanceof SecureConversation ||
                                credSrc instanceof SslAssertion)
                            {
                                foundUsableCredSource = true;
                                break;
                            }
                        }
                        if(!foundUsableCredSource) {
                            r.addError(new PolicyValidatorResult.Error(a,
                              ap,
                              "This identity cannot authenticate with the " +
                              "type of credential " +
                              "source specified.",
                              null));
                        }
                    }
                    break;
                case ID_SAMLONLY_NOCERT:
                    r.addWarning(new PolicyValidatorResult.Warning(a, ap, WARNING_NOCERT, null));
                    // fall through to the rest of the samlonly handling
                case ID_SAMLONLY:
                    { // scope
                        boolean foundUsableCredSource = false;
                        for (Assertion credSrc : pathContext.credentialSources) {
                            if (credSrc instanceof RequestWssSaml || credSrc instanceof SslAssertion) {
                                foundUsableCredSource = true;
                                break;
                            }
                        }
                        if(!foundUsableCredSource) {
                            r.addError(new PolicyValidatorResult.Error(a,
                              ap,
                              "This identity can only authenticate with " +
                              "a SAML token or SSL Client Certificate" +
                              "but another type of credential " +
                              "source is specified.",
                              null));
                        }
                    }
                    break;
                case ID_X509ONLY_NOCERT:
                    r.addWarning(new PolicyValidatorResult.Warning(a, ap, WARNING_NOCERT, null));
                    // fall through to the rest of the x509only handling
                case ID_X509ONLY:
                    { // scope
                        boolean foundUsableCredSource = false;
                        for (Assertion credSrc : pathContext.credentialSources) {
                            if (credSrc instanceof RequestWssX509Cert ||
                                    credSrc instanceof SecureConversation ||
                                    credSrc instanceof SslAssertion) {
                                foundUsableCredSource = true;
                                break;
                            }
                        }
                        if(!foundUsableCredSource) {
                            r.addError(new PolicyValidatorResult.Error(a,
                              ap,
                              "This identity can only authenticate using " +
                              "its client cert. " +
                              "The specified type of credential " +
                              "source is not supported by that user.",
                              null));
                        }
                    }
                    break;
                case ID_LDAP:
                    if (identityAssertion instanceof SpecificUser) {
                        if (pathContext.contains(HttpDigest.class)) {
                            r.addWarning(new PolicyValidatorResult.Warning(a,
                              ap,
                              "This identity may not be able to authenticate with the " +
                              "type of credential " +
                              "source specified.",
                              null));
                        }
                    }
                    break;
                case ID_ERROR:
                    r.addWarning(new PolicyValidatorResult.Warning(a,
                      ap,
                      "This identity cannot be validated at this time (identity provider error)",
                      null));
                    break;
            }
        } else if (a.isCredentialSource()) {
            pathContext.credentialSources.add(a);
        } else if (a instanceof JmsRoutingAssertion) {
            JmsRoutingAssertion jmsass = (JmsRoutingAssertion)a;
            if (jmsass.getEndpointOid() != null) {
                long endpointid = jmsass.getEndpointOid();
                boolean jmsEndpointDefinedOk = false;
                try {
                    JmsEndpoint routedRequestEndpoint = jmsEndpointManager.findByPrimaryKey(endpointid);
                    if (routedRequestEndpoint != null) jmsEndpointDefinedOk = true;
                } catch (FindException e) {
                    logger.log(Level.FINE, "Error fetching endpoint " + endpointid, e);
                }
                if (!jmsEndpointDefinedOk) {
                    r.addError(new PolicyValidatorResult.Error(a,
                      ap,
                      "This routing assertion refers to a JMS " +
                      "endpoint that cannot be found on this system.",
                      null));
                }
            }
        } else if (a instanceof SchemaValidation) {

            SchemaValidation svass = (SchemaValidation)a;
            AssertionResourceInfo ri = svass.getResourceInfo();
            if (ri instanceof StaticResourceInfo) { // check for unresolved imports
                validateSchemaValidation(a, ap, (StaticResourceInfo)ri, r);
            } else if (ri instanceof GlobalResourceInfo) { // check for broken ref
                boolean res = checkGlobalSchemaExists((GlobalResourceInfo)ri);
                if (!res) {
                    r.addError(new PolicyValidatorResult.Error(a,
                                        ap, "This assertion refers to a global schema that no longer exists.", null));
                }
            }
        } else if (a instanceof UnknownAssertion) {
            UnknownAssertion ua = (UnknownAssertion) a;

            String message = "Unknown assertion {0}; all requests to this service will fail.";
            String detail = "";
            Throwable cause = ua.cause();
            if(cause instanceof ClassNotFoundException) {
                String className = cause.getMessage();
                detail = " [" + (className.substring(className.lastIndexOf('.')+1)) + "]";
            }
            r.addError(new PolicyValidatorResult.Error(a, ap, MessageFormat.format(message, detail), null));
        } else if (a instanceof HttpFormPost) {
            HttpFormPost httpFormPost = (HttpFormPost) a;

            StringBuffer contentTypeBuffer = new StringBuffer();
            HttpFormPost.FieldInfo[] fieldInfos = httpFormPost.getFieldInfos();
            for (HttpFormPost.FieldInfo fieldInfo : fieldInfos) {
                String contentType = fieldInfo.getContentType();
                try {
                    ContentTypeHeader cth = ContentTypeHeader.parseValue(contentType);
                    String encoding = cth.getEncoding();
                    if (!Charset.isSupported(encoding)) {
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
                r.addError(new PolicyValidatorResult.Error(a, ap, msg, null));
            }
        }

        // not else-if since this is also a credential source
        if (a instanceof RequestWssKerberos) {
            try {
                KerberosClient.getKerberosAcceptPrincipal();
            }
            catch(KerberosConfigException kce) {
                r.addError(new PolicyValidatorResult.Error(a,
                  ap,
                  "Kerberos is not configured on the Gateway.",
                  null));
            }
            catch(KerberosException ke) {
                r.addError(new PolicyValidatorResult.Error(a,
                  ap,
                  "Gateway Kerberos configuration is invalid.",
                  null));
            }
        }

        if (a instanceof UsesEntities && !(a instanceof IdentityAssertion || a instanceof JmsRoutingAssertion)) {
            UsesEntities uea = (UsesEntities)a;
            for (EntityHeader header : uea.getEntitiesUsed()) {
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
                if (entity == null)
                    r.addError(new PolicyValidatorResult.Error(a, ap,
                            "Assertion refers to a " + header.getType().getName() +
                                    " that cannot be located on this system", thrown));
            }

        }

        if (a instanceof PrivateKeyable) {
            checkPrivateKey((PrivateKeyable)a, ap, r);
        }
    }

    private boolean checkGlobalSchemaExists(GlobalResourceInfo globalResourceInfo) {
        // look for the presence of the schema
        String sId = globalResourceInfo.getId();
        Collection res = null;
        try {
            res = schemaEntryManager.findByName(sId);
        } catch (FindException e) {
            logger.log(Level.INFO, "error looking for schema", e);
        }
        return !(res == null || res.isEmpty());
    }

    private void validateSchemaValidation(Assertion a, AssertionPath ap, StaticResourceInfo sri, PolicyValidatorResult r) {
        Document schemaDoc;
        try {
            schemaDoc = XmlUtil.stringToDocument(sri.getDocument());
            Element schemael = schemaDoc.getDocumentElement();
            List listofimports = XmlUtil.findChildElementsByName(schemael, schemael.getNamespaceURI(), "import");
            if (!listofimports.isEmpty()) {
                ArrayList<String> unresolvedImportsList = new ArrayList<String>();

                //noinspection ForLoopReplaceableByForEach
                for (Iterator iterator = listofimports.iterator(); iterator.hasNext();) {
                    Element importEl = (Element)iterator.next();
                    String importns = importEl.getAttribute("namespace");
                    String importloc = importEl.getAttribute("schemaLocation");
                    try {
                        if (importloc == null || schemaEntryManager.findByName(importloc).isEmpty()) {
                            if (importns == null || schemaEntryManager.findByTNS(importns).isEmpty()) {
                                if (importloc != null) {
                                    unresolvedImportsList.add(importloc);
                                } else {
                                    unresolvedImportsList.add(importns);
                                }
                            }
                        }
                    } catch (ObjectModelException e) {
                        logger.log(Level.SEVERE, "cannot get schema", e);
                    }
                }
                if (!unresolvedImportsList.isEmpty()) {
                    StringBuffer msg = new StringBuffer("The schema validation assertion contains unresolved imported " +
                            "schemas: ");
                    for (Iterator iterator = unresolvedImportsList.iterator(); iterator.hasNext();) {
                        msg.append(iterator.next());
                        if (iterator.hasNext()) msg.append(", ");
                    }
                    msg.append(".");
                    r.addError(new PolicyValidatorResult.Error(a, ap, msg.toString(), null));
                }
            }
        } catch (SAXException e) {
            logger.log(Level.INFO, "cannot parse xml from schema validation assertion", e);
            r.addError(new PolicyValidatorResult.Error(a,
                                                       ap,
                                                       "This schema validation assertion does not appear " +
                                                               "to contain a well-formed xml schema.",
                                                       null));
        }
    }

    protected void checkPrivateKey(PrivateKeyable a, AssertionPath ap, PolicyValidatorResult r) {
        if (!a.isUsesDefaultKeyStore()) {
            boolean found = false;
            try {
                long keystoreId = a.getNonDefaultKeystoreId();
                String keyAlias = a.getKeyAlias();
                SsgKeyEntry foundKey = ssgKeyStoreManager.lookupKeyByKeyAlias(keyAlias, keystoreId);
                if (foundKey != null) {
                    found = true;
                    long foundKeystoreId = foundKey.getKeystoreId();
                    if (foundKey.getKeystoreId() != keystoreId) {
                        SsgKeyFinder ks = ssgKeyStoreManager.findByPrimaryKey(foundKeystoreId);
                        String name = ks != null ? ks.getName() : ("id:" + foundKeystoreId);
                        r.addWarning(new PolicyValidatorResult.Warning((Assertion)a, ap,
                                                                       "This assertion refers to a Private Key with alias '" + keyAlias + "' in a keystore with ID '" + keystoreId + "'.  " +
                                                                       "No keystore with that ID is present on this SecureSpan Gateway, but the system has found " +
                                                                       "a Private Key with a matching alias in the keystore '" + name + "' and will use that.",
                                                                       null));
                    }
                }

            } catch (Exception e) {
                logger.log(Level.WARNING, "error looking for private key", e);
            }
            if (!found) {
                r.addError(new PolicyValidatorResult.Error((Assertion)a, ap,
                                                           "This assertion refers to a Private Key which cannot " +
                                                           "be found on this SecureSpan Gateway",
                                                           null));
            }
        }
    }

    /**
     * This is synchronized just in case, i dont expect this to be invoked from multiple simultaneous threads.
     * Add more return values as needed.
     * todo: ocnsider refactoring into some sort of status class 'IdentityStatus' instead of returning codes. - em
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
                        prov.test(true);
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
                        if (cfg.getTrustedCertOids().length==0 && idexists && !idhascert) {
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
                logger.log(Level.WARNING, "problem retrieving identity", e);
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

    public void setSchemaEntryManager(SchemaEntryManager schemaManager) {
        this.schemaEntryManager = schemaManager;
    }

    public void setClientCertManager(ClientCertManager clientCertManager) {
        this.clientCertManager = clientCertManager;
    }

    public void setEntityFinder(EntityFinder entityFinder) {
        this.entityFinder = entityFinder;
    }

    public void afterPropertiesSet() throws Exception {
        if (jmsEndpointManager == null) {
            throw new IllegalArgumentException("JMS Endpoint manager is required");
        }

        if (identityProviderFactory == null) {
            throw new IllegalArgumentException("Identity Provider Factory is required");
        }

        if (clientCertManager == null) {
            throw new IllegalArgumentException("Client Cert Manger is required");
        }

        if (entityFinder == null) {
            throw new IllegalArgumentException("EntityFinder is required");
        }
    }

    class PathContext {
        Collection<Assertion> credentialSources = new ArrayList<Assertion>();

        boolean contains(Class<? extends Assertion> clz) {
            for (Assertion ass : credentialSources) {
                if (ass.getClass().equals(clz)) {
                    return true;
                }
            }
            return false;
        }
    }
}
