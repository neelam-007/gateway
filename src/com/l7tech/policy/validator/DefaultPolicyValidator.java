package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.service.PublishedService;

import java.util.Iterator;
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


    public void validatePath(AssertionPath ap, PolicyValidatorResult r, PublishedService service) {
        Assertion[] ass = ap.getPath();
        PathValidator pv = new PathValidator(ap, r, service);
        for (int i = 0; i < ass.length; i++) {
            pv.validate(ass[i]);
        }

        // deferred validations
        Iterator dIt = pv.getDeferredValidators().iterator();
        while (dIt.hasNext()) {
            DeferredValidate dv = (DeferredValidate)dIt.next();
            dv.validate(pv, ass);
        }
        Assertion lastAssertion = ap.lastAssertion();
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
