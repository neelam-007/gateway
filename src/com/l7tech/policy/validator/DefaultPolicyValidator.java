package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.AssertionLicense;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CommentAssertion;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.service.PublishedService;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.xml.WsdlUtil;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.xml.sax.SAXException;

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

    public PolicyValidatorResult validate(Assertion assertion, PublishedService service, AssertionLicense assertionLicense) throws InterruptedException {
        PolicyValidatorResult r = super.validate(assertion, service, assertionLicense);

        try {
            if (service.isSoap() &&
                Assertion.contains(assertion, XpathBasedAssertion.class) &&
                WsdlUtil.isRPCWithNoSchema(service.getWsdlXml())) {

                Assertion lastAssertion = assertion;
                if (assertion instanceof CompositeAssertion) {
                    List children = ((CompositeAssertion) assertion).getChildren();
                    if (children != null && !children.isEmpty()) {
                        lastAssertion = (Assertion) children.get(children.size()-1);
                    }
                }

                r.addWarning(new PolicyValidatorResult.Warning(lastAssertion.getOrdinal(), 0, "Assertions that use XPaths may not work as expected with RPC services.", null));
            }
        } catch(SAXException se) {
            log.warning("Could not parse wsdl '" + ExceptionUtils.getMessage(se) + "', skipping WSDL checks.");
        }

        return r;
    }

    public void validatePath(AssertionPath ap, PolicyValidatorResult r, PublishedService service, AssertionLicense assertionLicense) throws InterruptedException {
        Assertion[] ass = ap.getPath();

        // paths that have the pattern "OR, Comment" should be ignored completly (bugzilla #2449)
        for (int i = 0; i < ass.length; i++) {
            if (ass[i] instanceof CommentAssertion) {
                if (ass[i].getParent() instanceof OneOrMoreAssertion) {
                    // if the parent OR has something else than comments as children, the this path should be ignored
                    OneOrMoreAssertion parent = (OneOrMoreAssertion)ass[i].getParent();
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

        PathValidator pv = new PathValidator(ap, r, service, assertionLicense);
        for (int i = 0; i < ass.length; i++) {
            if (ass[i] instanceof CommentAssertion) continue;
            pv.validate(ass[i]);
        }

        // deferred validations
        Iterator dIt = pv.getDeferredValidators().iterator();
        while (dIt.hasNext()) {
            if (Thread.interrupted())
                throw new InterruptedException();
            DeferredValidate dv = (DeferredValidate)dIt.next();
            dv.validate(pv, ass);
        }
        // last assertion should be last non-comment assertion
        Assertion lastAssertion = ap.lastAssertion();
        for (int i = ass.length-1; i >= 0; i--) {
            if (!(ass[i] instanceof CommentAssertion)) {
                lastAssertion = ass[i];
                break;
            }
        }
        if (!pv.seenRouting) { // no routing report that
            r.addWarning(new PolicyValidatorResult.
              Warning(lastAssertion, ap, "No route assertion.", null));
        }
        if (!pv.seenCredentials(XmlSecurityRecipientContext.LOCALRECIPIENT_ACTOR_VALUE) && pv.seenRouting) {
            r.addWarning(new PolicyValidatorResult.Warning(lastAssertion, ap,
              "No credential assertion is present in the policy. The" +
              " service may be exposed to public access", null));
        }
        if (pv.seenCredentials(XmlSecurityRecipientContext.LOCALRECIPIENT_ACTOR_VALUE) && !pv.seenAccessControl && pv.seenRouting) {
            r.addWarning(new PolicyValidatorResult.Warning(lastAssertion, ap, "Credentials are collected but not authenticated." +
              " This service may be exposed to public access.", null));
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
