package com.l7tech.console.tree.policy;

import java.lang.reflect.Constructor;

/**
 * Supporting class with assertion package private utilities.
 * <p>
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a> 
 */
public class Assertions {

    /**
     * this class cannot be instantiated
     */
    private Assertions() {
    }

    /**
     * Find the matching public constructor in the class cls for the class
     * array that descripbes the parameters.
     * <p>
     * @param cls the class that is searched for the constructor
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
     *
     * {@link Class#isAssignableFrom(Class) is used to determine if the
     * assignTo accepts the parameter from the assignFrom.
     *
     * @param assignTo the array receiving
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


}
