package com.l7tech.policy.validator;

import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RequestXpathAssertion;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;


/**
 * The class <code>ValidatorFactory</code> creates <code>AssertionValidator</code>
 * instances based on <code>Assertion</code> instances.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
class ValidatorFactory {
    private static Map assertionMap = new HashMap();

    // maping assertions to validators
    static {
        assertionMap.put(RequestXpathAssertion.class, RequestXpathAssertionValidator.class);
        // add mapping
    }

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
     * @return the AssertionTreeNode for a given assertion
     */
    static AssertionValidator getValidator(Assertion assertion) {
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
        // assertion lookup, find the  assertion tree node
        Class classNode = (Class)assertionMap.get(assertion.getClass());
        if (null == classNode) {
            return new NullValidator(assertion);
        }
        try {
            return makeValidator(classNode, assertion);
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

        Constructor ctor = findMatchingConstructor(classNode, new Class[]{assertion.getClass()});
        if (ctor != null)
            return (AssertionValidator)ctor.newInstance(new Object[]{assertion});
        throw new RuntimeException("Cannot locate expected he constructor in " + classNode);
    }

    /**
     * Find the matching public constructor in the class cls for the class
     * array that descripbes the parameters.
     * <p/>
     * 
     * @param cls    the class that is searched for the constructor
     * @param params the constructor parameters
     * @return the matching <code>Constructor</code> or <b>null</b> if no
     *         constructor can be found that matches parameters
     */
    static Constructor findMatchingConstructor(Class cls, Class[] params) {
        Constructor[] constructors = cls.getConstructors();
        for (int i = 0; i < constructors.length; i++) {
            if (isAssignable(constructors[i].getParameterTypes(), params)) {
                return constructors[i];
            }
        }
        return null;
    }

    /**
     * Determine whether the assignTo array accepts assignFrom classes in
     * the given order.
     * <p/>
     * {@link Class#isAssignableFrom(Class) is used to determine if the
     * assignTo accepts the parameter from the assignFrom.
     * 
     * @param assignTo   the array receiving
     * @param assignFrom the class array to check
     * @return true if assignable, false otherwise
     */
    static boolean isAssignable(Class[] assignTo, Class[] assignFrom) {
        if (assignTo == null) {
            return assignFrom == null || assignFrom.length == 0;
        }

        if (assignFrom == null) {
            return assignTo.length == 0;
        }

        if (assignTo.length != assignFrom.length) {
            return false;
        }

        for (int i = 0; i < assignTo.length; i++) {
            if (!(assignTo[i].isAssignableFrom(assignFrom[i]))) {
                return false;
            }
        }
        return true;
    }


    /**
     * special 'nullvalidator'
     */
    static class NullValidator implements AssertionValidator {
        private Assertion assertion;

        public NullValidator(Assertion a) {
            assertion = a;
        }

        public void validate(AssertionPath path, PolicyValidatorResult result) {}
    }
}
