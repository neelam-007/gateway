package com.l7tech.policy.validator;

import com.l7tech.common.policy.Policy;
import com.l7tech.common.policy.PolicyType;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.xml.WsdlUtil;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ReadOnlyEntityManager;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CommentAssertion;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * The policy validator that analyzes the policy assertion tree
 * and collects the errors.
 * <p/>
 * Errors are collected in the PolicyValidatorResult instance.
 * <p/>
 * The expected order is:
 * <ul>
 * <li><i>Pre conditions</i> such as ssl, and ip address range (optional)
 * <li><i>Credential location</i> such as ssl, and ip address range (optional)
 * <li><i>Access control, identity, group membership</i> (optional), if present
 * expects the credential finder precondition
 * <li><i>Routing</i> (optional), if present expects the credential finder
 * precondition
 * </ul>
 * <p/>
 * The class methods are not synchronized.
 * <p/>
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class DefaultPolicyValidator extends PolicyValidator {
    static Logger log = Logger.getLogger(DefaultPolicyValidator.class.getName());

    public DefaultPolicyValidator(ReadOnlyEntityManager<Policy, EntityHeader> policyFinder, PolicyPathBuilderFactory pathBuilderFactory) {
        super(policyFinder, pathBuilderFactory);
    }

    public PolicyValidatorResult validate(Assertion assertion, PolicyType policyType, Wsdl wsdl, boolean soap, AssertionLicense assertionLicense) throws InterruptedException {
        PolicyValidatorResult r = super.validate(assertion, policyType, wsdl, soap, assertionLicense);

        if (soap && Assertion.contains(assertion, XpathBasedAssertion.class) && WsdlUtil.isRPCWithNoSchema(wsdl)) {
            Assertion lastAssertion = assertion;
            if (assertion instanceof CompositeAssertion) {
                List children = ((CompositeAssertion) assertion).getChildren();
                if (children != null && !children.isEmpty()) {
                    lastAssertion = (Assertion) children.get(children.size()-1);
                }
            }

            r.addWarning(new PolicyValidatorResult.Warning(lastAssertion.getOwnerPolicyOid(), lastAssertion.getOrdinal(), 0, "Assertions that use XPaths may not work as expected with RPC services.", null));
        }

        return r;
    }

    public void validatePath(final AssertionPath ap,
                             final PolicyType policyType,
                             final Wsdl wsdl,
                             final boolean soap,
                             final AssertionLicense assertionLicense,
                             final PolicyValidatorResult r)
            throws InterruptedException
    {
        Assertion[] path = ap.getPath();

        // paths that have the pattern "OR, Comment" should be ignored completly (bugzilla #2449)
        for (Assertion assertion: path) {
//            assertion = dereferenceInclude(assertion, r);
            if (assertion instanceof CommentAssertion) {
                if (assertion.getParent() instanceof OneOrMoreAssertion) {
                    // if the parent OR has something else than comments as children, the this path should be ignored
                    OneOrMoreAssertion parent = (OneOrMoreAssertion) assertion.getParent();
                    boolean onlyCommentsInOR = true;
                    if (Thread.interrupted())
                        throw new InterruptedException();
                    for (Iterator parit = parent.children(); parit.hasNext();) {
                        if (!(parit.next() instanceof CommentAssertion)) {
                            onlyCommentsInOR = false;
                            break;
                        }
                    }
                    if (!onlyCommentsInOR) {
                        log.fine("Path " + ap + " ignored for validation purposes");
                        return;
                    }
                }
            }
        }

        PathValidator pv = new PathValidator(ap, r, wsdl, soap, assertionLicense);
        for (Assertion assertion : path) {
//            assertion = dereferenceInclude(assertion, r);
            if (assertion instanceof CommentAssertion) continue;
            pv.validate(assertion);
        }

        // deferred validations
        for (DeferredValidate dv : pv.getDeferredValidators()) {
            if (Thread.interrupted()) throw new InterruptedException();
            dv.validate(pv, path);
        }
        
        if (!policyType.isServicePolicy()) {
            // All subsequent rules pertain only to Service policies (i.e. not fragments)
            return;
        }

        // last assertion should be last non-comment assertion
        Assertion lastAssertion = ap.lastAssertion();
        for (int i = path.length-1; i >= 0; i--) {
//            Assertion ass = dereferenceInclude(path[i], r);
            Assertion ass = path[i];
            if (!(ass instanceof CommentAssertion)) {
                lastAssertion = ass;
                break;
            }
        }

        if (!pv.seenResponse) { // no routing report that
            r.addWarning(new PolicyValidatorResult.
              Warning(lastAssertion, ap, "No route assertion.", null));
        }
        if (!pv.seenParsing) {
            if (!soap) {
                r.addWarning(new PolicyValidatorResult.
                  Warning(lastAssertion, ap, "This path potentially allows non-xml content through.", null));
            }
        }
        if (!pv.seenCredentials(XmlSecurityRecipientContext.LOCALRECIPIENT_ACTOR_VALUE) && pv.seenResponse) {
            r.addWarning(new PolicyValidatorResult.Warning(lastAssertion, ap,
              "No credential assertion is present in the policy. The" +
              " service may be exposed to public access", null));
        }
        if (pv.seenCredentials(XmlSecurityRecipientContext.LOCALRECIPIENT_ACTOR_VALUE) && !pv.seenAccessControl && pv.seenResponse) {
            r.addWarning(new PolicyValidatorResult.Warning(lastAssertion, ap, "Credentials are collected but not authenticated." +
              " This service may be exposed to public access.", null));
        }
    }

    private Assertion dereferenceInclude(final Assertion assertion, final PolicyValidatorResult r) {
        if (!(assertion instanceof Include)) return assertion;

        Include include = (Include) assertion;
        try {
            Policy policy = policyFinder.findByPrimaryKey(include.getPolicyOid());
            if (policy == null) {
                r.addError(new PolicyValidatorResult.Error(assertion.getOwnerPolicyOid(), assertion.getOrdinal(), -1, "Include Assertion refers to Policy #" + include.getOwnerPolicyOid() + " (" + include.getPolicyName() + "), which no longer exists", null));
                return include;
            } else {
                try {
                    return policy.getAssertion();
                } catch (IOException e) {
                    r.addError(new PolicyValidatorResult.Error(assertion.getOwnerPolicyOid(), assertion.getOrdinal(), -1, "Include Assertion refers to Policy #" + include.getOwnerPolicyOid() + " (" + include.getPolicyName() + "), which cannot be parsed", e));
                    return include;
                }
            }
        } catch (FindException e) {
            throw new RuntimeException("Unable to load included policy #" + include.getPolicyOid() + " (" + include.getPolicyName() + ")");
        }
    }


    /**
     * The implementations are invoked after the regular (sequential)
     * validate of the assertion path. This is useful for unordered validations,
     * that is, where some asseriton must be present but not necessarily
     * before the assertion currently examined.
     */
    static interface DeferredValidate {
        void validate(PathValidator pv, Assertion[] path);
    }
}
