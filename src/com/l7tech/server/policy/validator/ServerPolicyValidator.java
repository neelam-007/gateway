package com.l7tech.server.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.xmlsec.SamlSecurity;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.transport.jms.JmsEndpointManager;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.objectmodel.FindException;
import com.l7tech.common.util.Locator;
import com.l7tech.common.transport.jms.JmsEndpoint;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Performs server side policy validation.
 *
 * Rules checked:
 *
 *   1.     for each id assertion, check that the corresponding id exists
 *
 *   2.     for each id assertion that is saml only, make sure that no
 *          credential assertion (other than saml) preceeds the assertion
 *
 *   3.     for each JMS routing assertion, make sure referenced endpoint exists
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 1, 2004<br/>
 * $Id$<br/>
 */
public class ServerPolicyValidator extends PolicyValidator {
    public void validatePath(AssertionPath ap, PolicyValidatorResult r, boolean isSoap) {
        Assertion[] ass = ap.getPath();
        PathContext pathContext = new PathContext();
        for (int i = 0; i < ass.length; i++) {
            validateAssertion(ass[i], pathContext, r, ap);
        }
    }

    private void validateAssertion(Assertion a, PathContext pathContext, PolicyValidatorResult r, AssertionPath ap) {
        if (a instanceof IdentityAssertion) {
            int idStatus = getIdStatus((IdentityAssertion)a);
            if (idStatus == 0) {
                r.addError(new PolicyValidatorResult.Error(a, ap, "The corresponding identity cannot be found. " +
                                                                  "Please remove the assertion from the policy.",
                                                                  null));
            } else if (idStatus == 2) {
                if (pathContext.seenCredCredAssertionOtherThanSaml) {
                    r.addError(new PolicyValidatorResult.Error(a, ap, "This identity can only authenticate with a SAML " +
                                                                      "token but another type of credential source is " +
                                                                      "specified.", null));
                }
            }
        } else if (a instanceof CredentialSourceAssertion) {
            if (!(a instanceof SamlSecurity)) {
                pathContext.seenCredCredAssertionOtherThanSaml = true;
            }
        } else if (a instanceof JmsRoutingAssertion) {
            JmsRoutingAssertion jmsass = (JmsRoutingAssertion)a;
            if (jmsass.getEndpointOid() != null) {
                long endpointid = jmsass.getEndpointOid().longValue();
                JmsEndpointManager mgr = (JmsEndpointManager)Locator.getDefault().lookup(JmsEndpointManager.class);
                boolean jmsEndpointDefinedOk = false;
                try {
                    JmsEndpoint routedRequestEndpoint = mgr.findByPrimaryKey(endpointid);
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
     *
     * @return see ID_NOT_EXIST, ID_EXIST, or ID_EXIST_BUT_SAMLONLY
     */
    private synchronized int getIdStatus(IdentityAssertion identityAssertion) {
        // look in cache first
        Integer output = (Integer)idAssertionStatus.get(identityAssertion);
        if (output == null) {
            try {
                // get provider
                IdentityProvider prov = IdentityProviderFactory.getProvider(identityAssertion.getIdentityProviderOid());
                // check if it's a fip
                boolean samlonly = false;
                if (prov.getConfig().getTypeVal() == IdentityProviderType.FEDERATED.toVal()) {
                    FederatedIdentityProviderConfig cfg = (FederatedIdentityProviderConfig)prov.getConfig();
                    if (cfg.isSamlSupported() && !cfg.isX509Supported()) {
                        samlonly = true;
                    }
                }
                boolean idexists = false;
                // check if user or group exists
                if (identityAssertion instanceof SpecificUser) {
                    SpecificUser su = (SpecificUser)identityAssertion;
                    if (prov.getUserManager().findByPrimaryKey(su.getUserUid()) != null) {
                        idexists = true;
                    }
                } else if (identityAssertion instanceof MemberOfGroup) {
                    MemberOfGroup mog = (MemberOfGroup)identityAssertion;
                    prov.getGroupManager().findByPrimaryKey(mog.getGroupId());
                } else {
                    throw new RuntimeException("Type not supported " + identityAssertion.getClass().getName());
                }
                int val = -1;
                if (!idexists) val = ID_NOT_EXIST;
                else if (samlonly) val = ID_EXIST_BUT_SAMLONLY;
                else val = ID_EXIST;
                output = new Integer(val);
                idAssertionStatus.put(identityAssertion, output);
            } catch (FindException e) {
                logger.log(Level.WARNING, "problem retrieving identity", e);
                output = new Integer(ID_NOT_EXIST);
                idAssertionStatus.put(identityAssertion, output);
            }
        }
        return output.intValue();
    }

    private static final int ID_NOT_EXIST = 0; // the corresponding id does not exist
    private static final int ID_EXIST = 1; // the corresponding id exists and does not belong to saml only fip
    private static final int ID_EXIST_BUT_SAMLONLY = 2; // the corresponding id exists but belongs to saml only fip

    // A new validator is instantiated once per policy validation
    // so it's ok to cache these here. This cache will expire at
    // end of each policy validation.
    /**
     * key is IdentityAssertion object
     * value Integer (ID_NOT_EXIST, ID_EXIST, or ID_EXIST_BUT_SAMLONLY)
     */
    private Map idAssertionStatus = new HashMap();

    private final Logger logger = Logger.getLogger(ServerPolicyValidator.class.getName());

    class PathContext {
        boolean seenCredCredAssertionOtherThanSaml = false;
    }
}
