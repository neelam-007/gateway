/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * Represents a constructor invocation on a class. The constructor requires an target Class,
 * and an array of arguments. e.g. <code>new ConstructorInvocation(String.class, new Object[]{"Hello"});</code>
 * <p>
 * The constructor can be invoked at any time by calling either
 * {@link ConstructorInvocation#invoke()} or {@link ConstructorInvocation#invoke(java.lang.Object[])}
 * and it will always result in creation of the new instance.
 *
 * @author <a href="mailto:emarceta@layer7tech.com">Emil Marceta</a>
 * @version Feb 26, 2004
 */
public final class ConstructorInvocation {
    private Class targetClass;
    private Class[] arguments = new Class[]{};

    /**
     * Creates a constructor invocation to be executed on target class with no arguments.
     *
     * @param targetClass the target class that will be instantiated
     */
    public ConstructorInvocation(Class targetClass) {
        this(targetClass, new Class[]{});
    }

    /**
     * Creates a constructor invocation to be executed on target class with the provided
     * arguments.
     *
     * @param targetClass the target class that will be instantiated
     * @param arguments   the constructor arguments. If <b>null</b> assumes no arguments
     */
    public ConstructorInvocation(Class targetClass, Class[] arguments) {
        if (targetClass == null) {
            throw new IllegalArgumentException();
        }
        if (arguments != null) {
            this.arguments = arguments;
        }
        this.targetClass = targetClass;
    }

    /**
     * Invokes the constructor on the saved class with the arguments specified in the constructor
     *
     * @return the new object instance
     * @throws IllegalAccessException see {@link Constructor#newInstance(java.lang.Object[])}
     * @throws InvocationTargetException see {@link Constructor#newInstance(java.lang.Object[])}
     * @throws InstantiationException see {@link Constructor#newInstance(java.lang.Object[])}
     */
    public Object invoke() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        return invoke(arguments);
    }


    /**
     * Invokes the constructor on the saved class with the arguments
     *
     * @param arguments the constructor arguments
     * @return the new instance constructed with the arguments
     * @throws IllegalAccessException see {@link Constructor#newInstance(java.lang.Object[])}
     * @throws InvocationTargetException see {@link Constructor#newInstance(java.lang.Object[])}
     * @throws InstantiationException see {@link Constructor#newInstance(java.lang.Object[])}
     */
    public Object invoke(Object[] arguments)
      throws IllegalAccessException, InvocationTargetException, InstantiationException {
        Class[] argumentsClasses = new Class[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            Object argument = arguments[i];
            argumentsClasses[i] = argument.getClass();
        }
        Constructor ctor = findMatchingConstructor(targetClass, argumentsClasses);
        if (ctor == null) {
            String msg = "No constructor in class '" + targetClass + "' with arguments " + Arrays.asList(argumentsClasses);
            InvocationTargetException ie = new InvocationTargetException(null, msg);
            throw ie;
        }
        return ctor.newInstance(arguments);
    }

    /**
     * Find the matching public constructor in the class cls for the class
     * array that describes the parameters.
     * <p/>
     *
     * @param cls    the class that is searched for the constructor
     * @param params the constructor parameters
     * @return the matching <code>Constructor</code> or <b>null</b> if no
     *         constructor can be found that matches parameters
     */
    public static Constructor findMatchingConstructor(Class cls, Class[] params) {
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
    public static boolean isAssignable(Class[] assignTo, Class[] assignFrom) {
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