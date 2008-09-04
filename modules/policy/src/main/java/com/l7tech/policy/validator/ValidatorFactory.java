package com.l7tech.policy.validator;

import com.l7tech.util.ConstructorInvocation;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.xmlsec.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;


/**
 * The class <code>ValidatorFactory</code> creates <code>AssertionValidator</code>
 * instances based on <code>Assertion</code> instances.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 * @noinspection UnusedDeclaration
 */
class ValidatorFactory {
    private static final Map<Class<? extends Assertion>, Class<? extends AssertionValidator>> assertionMap =
            new HashMap<Class<? extends Assertion>, Class<? extends AssertionValidator>>();

    private static final Map<Class<? extends Assertion>, Object> useNullValidator =
            Collections.synchronizedMap(new WeakHashMap<Class<? extends Assertion>, Object>());

    // maping assertions to validators
    static {
        assertionMap.put(RequestXpathAssertion.class, XpathBasedAssertionValidator.class);
        assertionMap.put(ResponseXpathAssertion.class, XpathBasedAssertionValidator.class);
        assertionMap.put(RequestWssIntegrity.class, XpathBasedAssertionValidator.class);
        assertionMap.put(RequestWssConfidentiality.class, RequestWssConfidentialityValidator.class);
        assertionMap.put(ResponseWssIntegrity.class, ResponseWssIntegrityValidator.class);
        assertionMap.put(ResponseWssConfidentiality.class, ResponseWssConfidentialityValidator.class);
        assertionMap.put(RequestSwAAssertion.class, SwaRequestAssertionValidator.class);
        assertionMap.put(RequestWssSaml.class, SamlStatementValidator.class);
        assertionMap.put(RequestWssSaml2.class, SamlStatementValidator.class);
        assertionMap.put(WsiBspAssertion.class, WsiBspAssertionValidator.class);
        assertionMap.put(WsspAssertion.class, WsspAssertionValidator.class);
        assertionMap.put(BridgeRoutingAssertion.class, BridgeRoutingAssertionValidator.class);
        // add mapping
    }

    private static Map<Class<? extends Assertion>, Constructor<AssertionValidator>> ctorCache =
            Collections.synchronizedMap(new WeakHashMap<Class<? extends Assertion>, Constructor<AssertionValidator>>());

    /**
     * private constructor, this class cannot be instantiated
     */
    private ValidatorFactory() {
    }



    /**
     * Returns the corresponding <code>AssertionTreeNode</code> instance
     * for an <code>Assertion</code><br>
     * In case there is no corresponding <code>AssertionTreeNode</code>
     * the <code>UnknownAssertionTreeNode</code> is returned
     *
     * @param assertion  the assertion that is to be validated.  required
     * @return the AssertionTreeNode for a given assertion
     */
    static AssertionValidator getValidator(Assertion assertion) {
        if (assertion == null) {
            throw new IllegalArgumentException();
        }

        final Class<? extends Assertion> assclass = assertion.getClass();

        Constructor<AssertionValidator> ctor = ctorCache.get(assclass);
        if (ctor != null) {
            try {
                // In some cases where an assertion has its own validator and also needs to validate context variables,
                // so it is safe to call addValidationAspects(...) to create a sequence validator.  It won't hurt if the
                // assertion doesn't need to validate context variables.
                return addValidationAspects(assertion, ctor.newInstance(assertion));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // assertion lookup, find the  assertion tree node
        Class classNode = assertionMap.get(assclass);
        if (null == classNode) {
            if (useNullValidator.containsKey(assclass))
                return addValidationAspects(assertion, new NullValidator(assertion));
            String classname = (String)assertion.meta().get(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME);
            try {
                classNode = assclass.getClassLoader().loadClass(classname);
            } catch (ClassNotFoundException e) {
                // Fallthrough and treat as null validator
            }
        }
        if (null == classNode) {
            useNullValidator.put(assclass, Boolean.TRUE);
            return addValidationAspects(assertion, new NullValidator(assertion));
        }
        try {
            return addValidationAspects(assertion, makeValidator(classNode, assertion));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create the validator node <code>classNode</code> using reflection.
     * The target class is searched for the constructor that accepts the assertion
     * as a parameter.
     * That is, the <code>AssertionValidator</code> implementation is searched for
     * the constructor accepting the <code>Assertion</code> parameter.
     *
     * @param classNode the class that is a subclass of AssertionTreeNode
     * @param assertion the assertion constructor parameter
     * @return the corresponding validator
     * @throws InstantiationException    thrown on error instantiating the validator
     * @throws InvocationTargetException thrown on error invoking the constructor
     * @throws IllegalAccessException    thrown if there is no access to the desired
     *                                   constructor
     */
    private static AssertionValidator makeValidator(Class classNode, Assertion assertion)
      throws InstantiationException, InvocationTargetException, IllegalAccessException {

        //noinspection unchecked
        Constructor<AssertionValidator> ctor = ConstructorInvocation.findMatchingConstructor(classNode, new Class[]{assertion.getClass()});
        if (ctor != null) {
            ctorCache.put(assertion.getClass(), ctor);
            return ctor.newInstance(assertion);
        }
        throw new RuntimeException("Cannot locate expected he constructor in " + classNode);
    }

    /**
     * Decorate the given AssertionValidator with any additional AssertionValidators that are
     * appropriate for the given assertion.
     *
     * @param assertion The assertions whose validation aspects are to be considered
     * @param assertionValidator The underlying assertion validator
     * @return The decorated AssertionValidator which may be the same as the given AssertionValidator
     */
    private static AssertionValidator addValidationAspects( final Assertion assertion, final AssertionValidator assertionValidator ) {
        AssertionValidator decoratedValidator = assertionValidator;

        if ( assertion instanceof UsesVariables ) {
            decoratedValidator = new SequenceValidator( assertionValidator, new VariableUseValidator(assertion) );
        }

        return decoratedValidator;
    }

    /**
     * special 'nullvalidator'
     */
    static class NullValidator implements AssertionValidator {
        public NullValidator(Assertion a) {
        }

        public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {}
    }
}
