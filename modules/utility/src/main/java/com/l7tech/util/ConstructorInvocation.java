/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
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
    private Object[] arguments = new Object[]{};

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
    public ConstructorInvocation(Class targetClass, Object[] arguments) {
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
            throw new InvocationTargetException(null, msg);
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
        for (Constructor constructor : constructors) {
            if (isAssignable(constructor.getParameterTypes(), params)) {
                return constructor;
            }
        }
        return null;
    }


    /**
     * Determine whether the assignTo array accepts assignFrom classes in
     * the given order.
     * <p/>
     * {@link Class#isAssignableFrom(Class)} is used to determine if the
     * assignTo accepts the parameter from the assignFrom.
     *
     * @param assignTo     the array receiving
     * @param assignFrom   the class array to check
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

    /**
     * Loads the specified prospect class using the specified class loader, verifies that it is assignable to requiredSuperclass,
     * and checks it for a public constructor that takes formal parameters compatible with inClasses.
     *
     * @param loader              the ClassLoader to use when loading prospectClassname, or null to use the this methods own classloader.
     * @param prospectClassname   the name of the class to load.  If null, this method immediately returns null without taking further action.
     * @param requiredSuperclass  a superclass or interface that prospectClassname is expected to extend or implement.  Must not be null.
     * @param inClasses           formal parameter types for which a matching constructor must be compatible.
     *                            For example, if inClasses was {String.class, JDialog.class},
     *                            a constructor that takes (String, Window) would be considered a match.
     *                            Must not be null, but may be empty, in which case only a nullary constructor will match.
     * @return the matching constructor that was found.  Never null.
     * @throws ClassNotFoundException     if prospectClassname can't be found by the specified class loader
     * @throws WrongSuperclassException   if prospectClassname was found but is not assignable to requiredSuperclass
     * @throws AbstractClassException     if prospectClassname was found but turned out to be abstract
     * @throws NoMatchingPublicConstructorException  if prospectClassname was found but did not have a public constructor compatible with inClasses
     */
    public static <OUT> Constructor<OUT>
    findMatchingConstructor(ClassLoader loader,
                            String prospectClassname,
                            Class<OUT> requiredSuperclass,
                            Class[] inClasses)
            throws ClassNotFoundException, WrongSuperclassException,
                   AbstractClassException, NoMatchingPublicConstructorException
    {
        if (prospectClassname == null)
            throw new NullPointerException("prospectClassname is required");
        //noinspection unchecked
        Class<OUT> prospectClass = (Class<OUT>)Class.forName(prospectClassname, true, loader);
        if (!requiredSuperclass.isAssignableFrom(prospectClass))
            throw new WrongSuperclassException();
        if (Modifier.isAbstract(prospectClass.getModifiers()))
            throw new AbstractClassException();
        //noinspection unchecked
        final Constructor<OUT> ctor = findMatchingConstructor(prospectClass, inClasses);
        if (ctor == null || !Modifier.isPublic(ctor.getModifiers()))
            throw new NoMatchingPublicConstructorException();
        return ctor;
    }

    /**
     * Loads the specified prospect class using the specified class loader, verifies that it is assignable to requiredSuperclass,
     * and checks it for a public constructor that takes a single argument of type inClass.
     * If it finds everything it needs, it returns a unary factory that can be used to create an instance of OUT
     * given an instance of IN.
     *
     * @param loader              the ClassLoader to use when loading prospectClassname, or null to use the this methods own classloader.
     * @param prospectClassname   the name of the class to load.  If null, this method immediately returns null without taking further action.
     * @param requiredSuperclass  a superclass or interface that prospectClassname is expected to extend or implement.  Must not be null.
     * @param inClass             the type of the single argument that the constructor and unary factory method will take.  Must not be null.
     * @return a Unary factory that can be used to create instances of prospectClassname, or null if prospectClassname is null.
     * @throws ClassNotFoundException     if prospectClassname can't be found by the specified class loader
     * @throws WrongSuperclassException   if prospectClassname was found but is not assignable to requiredSuperclass
     * @throws AbstractClassException     if prospectClassname was found but turned out to be abstract
     * @throws NoMatchingPublicConstructorException  if prospectClassname was found but did not have a public unary constructor-from-inClass
     */
    public static <IN, OUT> Functions.Unary<OUT, IN>
    createFactoryOutOfUnaryConstructor(ClassLoader loader,
                                       String prospectClassname,
                                       Class<OUT> requiredSuperclass,
                                       Class<IN> inClass)
            throws WrongSuperclassException, AbstractClassException, ClassNotFoundException, NoMatchingPublicConstructorException
    {
        if (prospectClassname == null)
            return null;
        final Constructor<OUT> ctor = findMatchingConstructor(loader,
                                                              prospectClassname,
                                                              requiredSuperclass,
                                                              new Class[] { inClass });
        return new Functions.Unary<OUT, IN>() {
            public OUT call(IN in) {
                try {
                    return ctor.newInstance(in);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e); // can't happen, we checked this in advance
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e); // can't happen, we checked this in advance
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e); // Pass it along
                }
            }
        };
    }

    /**
     * Loads the specified prospect class using the specified class loader, verifies that it is assignable to requiredSuperclass,
     * and checks it for a public constructor that takes two arguments of type inClass1 and inClass2 respectively.
     * If it finds everything it needs, it returns a factory that can be used to create an instance of OUT
     * given an instance of IN_1 and an instance of IN_2.
     *
     * @param loader              the ClassLoader to use when loading prospectClassname, or null to use the this methods own classloader.
     * @param prospectClassname   the name of the class to load.  If null, this method immediately returns null without taking further action.
     * @param requiredSuperclass  a superclass or interface that prospectClassname is expected to extend or implement.  Must not be null.
     * @param inClass1             the type of the first argument that the constructor and factory method will take.  Must not be null.
     * @param inClass2             the type of the second argument that the constructor and factory method will take.  Must not be null.
     * @return a factory that can be used to create instances of prospectClassname, or null if prospectClassname is null.
     * @throws ClassNotFoundException     if prospectClassname can't be found by the specified class loader
     * @throws WrongSuperclassException   if prospectClassname was found but is not assignable to requiredSuperclass
     * @throws AbstractClassException     if prospectClassname was found but turned out to be abstract
     * @throws NoMatchingPublicConstructorException  if prospectClassname was found but did not have a public constructor-from-(inClass1, inClass2)
     */
    public static <IN_1, IN_2, OUT> Functions.Binary<OUT, IN_1, IN_2>
    createFactoryOutOfBinaryConstructor(ClassLoader loader,
                                        String prospectClassname,
                                        Class<OUT> requiredSuperclass,
                                        Class<IN_1> inClass1,
                                        Class<IN_2> inClass2)
            throws AbstractClassException, NoMatchingPublicConstructorException,
                   WrongSuperclassException, ClassNotFoundException
    {
        if (prospectClassname == null)
            return null;
        final Constructor<OUT> ctor = findMatchingConstructor(loader,
                                                              prospectClassname,
                                                              requiredSuperclass,
                                                              new Class[] { inClass1, inClass2 });
        return new Functions.Binary<OUT, IN_1, IN_2>() {
            public OUT call(IN_1 in_1, IN_2 in_2) {
                try {
                    return ctor.newInstance(in_1, in_2);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e); // can't happen, we checked this in advance
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e); // can't happen, we checked this in advance
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e); // Pass it along
                }
            }
        };
    }

    /**
     * Loads the specified prospect class using the specified class loader, verifies that it is assignable to requiredSuperclass,
     * and checks it for a public constructor that takes three arguments of type inClass1, inClass2, and inClass3 respectively.
     * If it finds everything it needs, it returns a factory that can be used to create an instance of OUT
     * given an instance of IN_1, an instance of IN_2, and an instance of IN_3.
     *
     * @param loader              the ClassLoader to use when loading prospectClassname, or null to use the this methods own classloader.
     * @param prospectClassname   the name of the class to load.  If null, this method immediately returns null without taking further action.
     * @param requiredSuperclass  a superclass or interface that prospectClassname is expected to extend or implement.  Must not be null.
     * @param inClass1             the type of the first argument that the constructor and factory method will take.  Must not be null.
     * @param inClass2             the type of the second argument that the constructor and factory method will take.  Must not be null.
     * @param inClass3             the type of the third argument that the constructor and factory method will take.  Must not be null.
     * @return a factory that can be used to create instances of prospectClassname, or null if prospectClassname is null.
     * @throws ClassNotFoundException     if prospectClassname can't be found by the specified class loader
     * @throws WrongSuperclassException   if prospectClassname was found but is not assignable to requiredSuperclass
     * @throws AbstractClassException     if prospectClassname was found but turned out to be abstract
     * @throws NoMatchingPublicConstructorException  if prospectClassname was found but did not have a public constructor-from-(inClass1, inClass2)
     */
    public static <IN_1, IN_2, IN_3, OUT> Functions.Ternary<OUT, IN_1, IN_2, IN_3>
    createFactoryOutOfTernaryConstructor(ClassLoader loader,
                                         String prospectClassname,
                                         Class<OUT> requiredSuperclass,
                                         Class<IN_1> inClass1,
                                         Class<IN_2> inClass2,
                                         Class<IN_3> inClass3)
            throws AbstractClassException, NoMatchingPublicConstructorException,
                   WrongSuperclassException, ClassNotFoundException
    {
        if (prospectClassname == null)
            return null;
        final Constructor<OUT> ctor = findMatchingConstructor(loader,
                                                              prospectClassname,
                                                              requiredSuperclass,
                                                              new Class[] { inClass1, inClass2, inClass3 });
        return new Functions.Ternary<OUT, IN_1, IN_2, IN_3>() {
            public OUT call(IN_1 in_1, IN_2 in_2, IN_3 in_3) {
                try {
                    return ctor.newInstance(in_1, in_2, in_3);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e); // can't happen, we checked this in advance
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e); // can't happen, we checked this in advance
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e); // Pass it along
                }
            }
        };
    }

    /** Exception thrown when a loaded class fails to be assignable to the expected superclass. */
    public static class WrongSuperclassException extends Exception {}

    /** Exception thrown when a loaded class turns out to be abstract. */
    public static class AbstractClassException extends Exception {}

    /** Exception thrown when a loaded class lacks a public constructor taking the expected arguments. */
    public static class NoMatchingPublicConstructorException extends Exception {}
}