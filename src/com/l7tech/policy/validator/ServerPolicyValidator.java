package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.xmlsec.SamlSecurity;

import java.util.HashMap;
import java.util.Map;

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
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 1, 2004<br/>
 * $Id$<br/>
 */
public class ServerPolicyValidator extends PolicyValidator {
    public void validatePath(AssertionPath ap, PolicyValidatorResult r) {
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
                r.addError(new PolicyValidatorResult.Error(a, ap, "The corresponding identity no longer exists.", null));
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
        }
    }

    /**
     * @return 0: the corresponding id does not exist
     *         1: the corresponding id exists and does not belong to saml only fip
     *         2: the corresponding id exists but belongs to saml only fip
     */
    private int getIdStatus(IdentityAssertion identityAssertion) {
        Integer output = (Integer)idAssertionStatus.get(identityAssertion);
        if (output == null) {
            // todo
        }
        return output.intValue();
    }

    // A new validator is instantiated once per policy validation
    // so it's ok to cache these here. This cache will expire at
    // end of each policy validation.
    /**
     * key is IdentityAssertion object
     * value Integer
     *      0: the corresponding id does not exist
     *      1: the corresponding id exists and does not belong to saml only fip
     *      2: the corresponding id exists but belongs to saml only fip
     */
    private Map idAssertionStatus = new HashMap();
    /**
     * key is Long with id provider config id
     * value is Integer
     *      0: the corresponding provider does not exist
     *      1: the corresponding provider exists and has nothing special
     *      2: the corresponding provider exists and is of type that only allows saml
     */
    private Map idProviderStatus = new HashMap();

    class PathContext {
        boolean seenCredCredAssertionOtherThanSaml = false;
    }
}
