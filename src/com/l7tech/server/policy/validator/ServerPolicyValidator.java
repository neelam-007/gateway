package com.l7tech.server.policy.validator;

import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.User;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.SamlSecurity;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.transport.jms.JmsEndpointManager;
import com.l7tech.service.PublishedService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

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
public class ServerPolicyValidator extends PolicyValidator
    implements ApplicationContextAware, InitializingBean {
    private ApplicationContext applicationContext;
    private JmsEndpointManager jmsEndpointManager;

    public void validatePath(AssertionPath ap, PolicyValidatorResult r, PublishedService service) {
        Assertion[] ass = ap.getPath();
        PathContext pathContext = new PathContext();
        for (int i = 0; i < ass.length; i++) {
            validateAssertion(ass[i], pathContext, r, ap);
        }
    }

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
                case ID_FIP:
                    for (Iterator iterator = pathContext.credentialSources.iterator(); iterator.hasNext();) {
                        CredentialSourceAssertion credSrc = (CredentialSourceAssertion)iterator.next();
                        if (credSrc instanceof SamlSecurity || credSrc instanceof RequestWssX509Cert ||
                          credSrc instanceof SecureConversation || credSrc instanceof HttpClientCert)
                            ;
                        else {
                            r.addError(new PolicyValidatorResult.Error(a,
                              ap,
                              "This identity cannot authenticate with the " +
                              "type of credential " +
                              "source specified.",
                              null));
                            break;
                        }
                    }
                    break;
                case ID_SAMLONLY:
                    for (Iterator iterator = pathContext.credentialSources.iterator(); iterator.hasNext();) {
                        CredentialSourceAssertion credSrc = (CredentialSourceAssertion)iterator.next();
                        if (!(credSrc instanceof SamlSecurity)) {
                            r.addError(new PolicyValidatorResult.Error(a,
                              ap,
                              "This identity can only authenticate with " +
                              "a SAML token " +
                              "but another type of credential " +
                              "source is specified.",
                              null));
                            break;
                        }
                    }
                    break;
                case ID_X509ONLY:
                    for (Iterator iterator = pathContext.credentialSources.iterator(); iterator.hasNext();) {
                        CredentialSourceAssertion credSrc = (CredentialSourceAssertion)iterator.next();
                        if (!(credSrc instanceof RequestWssX509Cert || credSrc instanceof SecureConversation ||
                          credSrc instanceof HttpClientCert)) {
                            r.addError(new PolicyValidatorResult.Error(a,
                              ap,
                              "This identity can only authenticate using " +
                              "its client cert. " +
                              "The specified type of credential " +
                              "source is not supported by that user.",
                              null));
                            break;
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
            }
        } else if (a instanceof CredentialSourceAssertion) {
            pathContext.credentialSources.add(a);
        } else if (a instanceof JmsRoutingAssertion) {
            JmsRoutingAssertion jmsass = (JmsRoutingAssertion)a;
            if (jmsass.getEndpointOid() != null) {
                long endpointid = jmsass.getEndpointOid().longValue();
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
        Integer output = (Integer)idAssertionStatusCache.get(identityAssertion);
        if (output == null) {
            try {
// get provider
                IdentityProviderFactory ipf = (IdentityProviderFactory)applicationContext.getBean("identityProviderFactory");
                IdentityProvider prov = ipf.getProvider(identityAssertion.getIdentityProviderOid());
                if (prov == null) {
                    idAssertionStatusCache.put(identityAssertion, new Integer(PROVIDER_NOT_EXIST));
                    return PROVIDER_NOT_EXIST;
                }
// check if user or group exists
                boolean idexists = false;
                if (identityAssertion instanceof SpecificUser) {
                    SpecificUser su = (SpecificUser)identityAssertion;
                    final String uid = su.getUserUid();
                    User u = null;
                    if (uid == null) {
                        u = prov.getUserManager().findByLogin(su.getUserLogin());
                    } else {
                        u = prov.getUserManager().findByPrimaryKey(uid);
                    }
                    if (u != null) {
                        idexists = true;
                    }
                } else if (identityAssertion instanceof MemberOfGroup) {
                    MemberOfGroup mog = (MemberOfGroup)identityAssertion;
                    Group g = null;
                    final String gid = mog.getGroupId();
                    if (gid == null) {
                        g = prov.getGroupManager().findByName(mog.getGroupName());
                    } else {
                        g = prov.getGroupManager().findByPrimaryKey(gid);
                    }
                    if (g != null) {
                        idexists = true;
                    }
                } else {
                    throw new RuntimeException("Type not supported " + identityAssertion.getClass().getName());
                }
                if (!idexists) {
                    output = new Integer(ID_NOT_EXIST);
                } else {
// check for special fip values
                    int val = ID_EXIST;
                    if (IdentityProviderType.is(prov, IdentityProviderType.FEDERATED)) {
                        val = ID_FIP;
                        FederatedIdentityProviderConfig cfg = (FederatedIdentityProviderConfig)prov.getConfig();
                        if (cfg.isSamlSupported() && !cfg.isX509Supported()) {
                            val = ID_SAMLONLY;
                        } else if (!cfg.isSamlSupported() && cfg.isX509Supported()) {
                            val = ID_X509ONLY;
                        }
                    } else if (IdentityProviderType.is(prov, IdentityProviderType.LDAP)) {
                        val = ID_LDAP;
                    }
                    output = new Integer(val);
                }
                idAssertionStatusCache.put(identityAssertion, output);
            } catch (FindException e) {
                logger.log(Level.WARNING, "problem retrieving identity", e);
                output = new Integer(ID_NOT_EXIST);
                idAssertionStatusCache.put(identityAssertion, output);
            }
        }
        return output.intValue();
    }

    private static final int ID_NOT_EXIST = 0; // the corresponding id does not exist
    private static final int ID_EXIST = 1; // the corresponding id exists and is not fip
    private static final int ID_FIP = 2; // the corresponding id exists but in a fip provider (saml and wss authen only)
    private static final int ID_SAMLONLY = 3; // the corresponding id exists but belongs to saml only fip
    private static final int ID_X509ONLY = 4; // the corresponding id exists but belongs to X509 only fip
    private static final int ID_LDAP = 5; // the corresponding id exists in a ldap provider
    private static final int PROVIDER_NOT_EXIST = 6; // the corresponding provider does not exist any more

// A new validator is instantiated once per policy validation
// so it's ok to cache these here. This cache will expire at
// end of each policy validation.
    /**
     * key is IdentityAssertion object
     * value Integer (ID_NOT_EXIST, ID_EXIST, ID_FIP, ID_SAMLONLY, or ID_X509ONLY)
     */
    private Map idAssertionStatusCache = new HashMap();

    private final Logger logger = Logger.getLogger(ServerPolicyValidator.class.getName());

    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        applicationContext = ctx;
    }

    public void setJmsEndpointManager(JmsEndpointManager jmsEndpointManager) {
        this.jmsEndpointManager = jmsEndpointManager;
    }

    public void afterPropertiesSet() throws Exception {
        if (jmsEndpointManager == null) {
            throw new IllegalArgumentException("JMS Endpoint manager is required");
        }
    }

    class PathContext {
        Collection credentialSources = new ArrayList();

        boolean contains(Class clz) {
            for (Iterator iterator = credentialSources.iterator(); iterator.hasNext();) {
                Object o = iterator.next();
                if (o.getClass().equals(clz)) {
                    return true;
                }
            }
            return false;
        }
    }
}
