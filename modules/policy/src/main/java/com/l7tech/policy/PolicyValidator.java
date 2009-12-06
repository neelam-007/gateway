package com.l7tech.policy;

import com.l7tech.wsdl.Wsdl;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.validator.ValidatorFlag;
import com.l7tech.objectmodel.GuidBasedEntityManager;
import com.l7tech.util.Functions;

import java.io.IOException;
import java.util.*;

/**
 * A class for validating policies.
 *
 * To create a <code>PolicyValidator</code>, call one of the static factory
 * methods.
 *
 * Once a PolicyValidator object has been created, it can be used to validate
 * policy/assertion trees by calling validate method
 * and passing it the <code>Asserion</code> to be validated.
 *
 * the result is returned in an object of <code>PolicyValidatorResult</code>
 * type.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public abstract class PolicyValidator {
    protected final GuidBasedEntityManager<Policy> policyFinder;
    private final PolicyPathBuilderFactory pathBuilderFactory;

    /**
     * Protected constructor, the <code>PolicyValidator</code> instances
     * are obtained using Spring
     */
    protected PolicyValidator(GuidBasedEntityManager<Policy> policyFinder, PolicyPathBuilderFactory pathBuilderFactory) {
        this.policyFinder = policyFinder;
        this.pathBuilderFactory = pathBuilderFactory;
    }

    /**
     * Validates the specified assertion tree.
     */
    public PolicyValidatorResult validate(Assertion assertion, PolicyType policyType, Wsdl wsdl, boolean soap, AssertionLicense assertionLicense) throws InterruptedException {
        assertion.treeChanged();

        // where to collect the result
        PolicyValidatorResult result = new PolicyValidatorResult();
        PolicyPathResult path;
        try {
            //we'll preprocess for any include fragments errors, if there are errors then we'll return the list of errros
            //back to the GUI
            result = preProcessIncludeFragments(assertion);

            //if contains at least one error, report it to GUI
            if ( !result.getErrors().isEmpty() ) {
                return result;
            }

            path = pathBuilderFactory.makePathBuilder().generate(assertion);
        } catch (PolicyAssertionException e) {
            result.addError(new PolicyValidatorResult.Error(e.getAssertion(), null, e.getMessage(), e));
            return result;
        }

        for (AssertionPath assertionPath : path.paths()) {
            validatePath(assertionPath, policyType, wsdl, soap, assertionLicense, result);
        }
        return result;
    }

    /**
     * Validate the the asserion path and collect the result into the validator result
     *
     * @param ap the assertion path to validate
     * @param policyType The type of the policy to validate
     * @param wsdl The WSDL that relates to the policy
     * @param soap true if the policy is only for SOAP services
     * @param r The result collect parameter
     * @throws InterruptedException if the thread is interrupted while validating
     */
    abstract public void validatePath(AssertionPath ap, PolicyType policyType, Wsdl wsdl, boolean soap, AssertionLicense assertionLicense, PolicyValidatorResult r) throws InterruptedException;

    /**
     * Scans the provided assertion tree looking for circular includes.
     *
     * @param policyId The identifier for the policy that the provided assertion is the root of
     * @param policyName The name of the policy that the provided assertion is the root of
     * @param rootAssertion The root assertion to start scanning
     * @param r The results of the validation check
     */
    public void checkForCircularIncludes(String policyId, String policyName, Assertion rootAssertion, PolicyValidatorResult r) {
        Map<String,String> visitedPolicyIdentifiers = new HashMap<String,String>();
        visitedPolicyIdentifiers.put(policyId, policyName);

        checkAssertionForCircularIncludes(rootAssertion, visitedPolicyIdentifiers, r);
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private void checkAssertionForCircularIncludes(Assertion rootAssertion, Map<String,String> visitedPolicies, PolicyValidatorResult r) {
        if(rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion compositeAssertion = (CompositeAssertion)rootAssertion;
            for(Iterator it = compositeAssertion.children();it.hasNext();) {
                Assertion child = (Assertion)it.next();
                checkAssertionForCircularIncludes(child, visitedPolicies, r);
            }
        } else if(rootAssertion instanceof Include) {
            Include includeAssertion = (Include)rootAssertion;
            String policyIdentifier = includeAssertion.getPolicyGuid();
            if ( policyIdentifier == null ) {
                policyIdentifier = "policyoid:" + includeAssertion.getPolicyOid();    
            }
            if(visitedPolicies.keySet().contains(policyIdentifier)) {
                PolicyAssertionException pae = new PolicyAssertionException(includeAssertion, "Circular policy include for Policy " + visitedPolicies.get(policyIdentifier));
                r.addError(new PolicyValidatorResult.Error(includeAssertion, new AssertionPath(includeAssertion.getPath()), pae.getMessage(), pae));
            } else {
                Policy includedPolicy = includeAssertion.retrieveFragmentPolicy();

                if(includedPolicy == null) {
                    try {
                        includedPolicy = policyFinder.findByGuid(includeAssertion.getPolicyGuid());
                        if(includedPolicy == null) {
                            return;
                        }
                    } catch(Exception e) {
                        return;
                    }
                }

                visitedPolicies.put(includedPolicy.getGuid(), includedPolicy.getName());
                visitedPolicies.put("policyoid:" + includedPolicy.getOid(), includedPolicy.getName());
                try {
                    checkAssertionForCircularIncludes(includedPolicy.getAssertion(), visitedPolicies, r);
                } catch(IOException e) {
                    // ignore
                } finally {
                    visitedPolicies.remove(includedPolicy.getGuid());
                    visitedPolicies.remove("policyoid:" + includedPolicy.getOid());
                }
            }
        }
    }

    /**
     * This will take in an assertion and scan through the assertion for any policy assertion problems that involves with
     * Include fragments.  If there are problems, it will accumulate these problems into the PolicyValidatorResult object
     * which contains all the PolicyAssertionException.
     *
     * @param assertion The assertion to be used for the preprocessing.
     * @return  Returns a PolicyValidatorResult containing errors, if any.  Will not return NULL.
     */
    private PolicyValidatorResult preProcessIncludeFragments(Assertion assertion) {
        PolicyValidatorResult policyValidatorResult = new PolicyValidatorResult();

        //resuse the build factory to help us scan the include fragments
        List<PolicyAssertionException> listOfExceptions = pathBuilderFactory.makePathBuilder().preProcessIncludeFragments(assertion);

        //if there are errors, the we want to store these errors into the PolicyValidatorResult object
        if ( !listOfExceptions.isEmpty() ) {
            for (PolicyAssertionException pae : listOfExceptions ) {
                policyValidatorResult.addError(new PolicyValidatorResult.Error(pae.getAssertion(), null, pae.getMessage(), pae));
            }
        }
        return policyValidatorResult;
    }

    /**
     * Check if the specified assertion is a credential source assertion configured to gather X.509 credentials.
     *
     * @param credSrc the assertion to examine.  Required.
     * @return  true if the specfied assertion appears to be a credential source assertion configured to gather X.509 credentials.
     */
    public static boolean isX509CredentialSource(Assertion credSrc) {
        if (!credSrc.isCredentialSource())
            return false;

        if (credSrc instanceof RequireWssX509Cert ||
                credSrc instanceof SecureConversation ||
                credSrc instanceof SslAssertion) {
            return true;
        }

        Functions.Unary<Set<ValidatorFlag>, Assertion> flagfac = credSrc.meta().get(AssertionMetadata.POLICY_VALIDATOR_FLAGS_FACTORY);
        if (flagfac == null)
            return false;

        Set<ValidatorFlag> flags = flagfac.call(credSrc);
        return flags != null && flags.contains(ValidatorFlag.GATHERS_X509_CREDENTIALS);
    }
}
