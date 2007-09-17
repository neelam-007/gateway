/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.test.performance;

import com.sun.japex.Japex;
import com.sun.japex.JapexDriverBase;
import com.sun.japex.TestCase;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A Japex driver with facilities that parallels concepts in JUnit TestCase.
 *
 * <p>(I wrote this to replace {@link com.sun.japex.jdsl.junit.JUnitDriver}
 * because that driver is wrong. Specifically, it incorrectly includes
 * {@link junit.framework.TestCase#setUp()} and {@link junit.framework.TestCase#tearDown()}
 * in performance timing.)
 *
 * <p>This driver can time test methods in any class. But it has facilities that
 * can call no-arg methods to set up and tear down class and instance, similar
 * to those of JUnit {@link junit.framework.TestCase TestCase}. These methods don't
 * have to be named setUp and tearDown, but is best kept for familiarity. And
 * the test class does not have to be extended from JUnit <code>TestCase</code>
 * either. But it <b>must have a no-arg constructor</b>.
 *
 * <p>It is best illustrated using an example Japex configuration file that uses this driver:
 * <blockquote><pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?>
 * &lt;testSuite name="JapexTest" xmlns="http://www.sun.com/japex/testSuite">
 *     &lt;param name="japex.classPath" value="${japex.home}/jdsl/jdsl.jar"/>
 *     &lt;param name="japex.classPath" value="${japex.home}/jdsl/junit.jar"/>
 *     &lt;param name="japex.classPath" value="classes"/>
 *
 *     &lt;param name="japex.resultUnit" value="tps"/>      &lt;!-- must be tps -->
 *     &lt;param name="japex.warmupsPerDriver" value="0"/>  &lt;!-- always 0 -->
 *     &lt;param name="japex.runsPerDriver" value="1"/>     &lt;!-- always 1 -->
 *     &lt;param name="japex.warmupIterations" value="2"/>  &lt;!-- alternatively, japex.warmupTime; can be overridden in any &lt;testCase> -->
 *     &lt;param name="japex.runIterations" value="10"/>    &lt;!-- alternatively, japex.runTime; can be overridden in any &lt;testCase> -->
 *
 *     &lt;driver name="JUnitDriver">                       &lt;!-- must be the only driver -->
 *         &lt;param name="japex.driverClass" value="JUnitDriver"/>
 *         &lt;param name="layer7.runInInitializeDriver" value="TestCaseA.setUpClass"/>
 *         &lt;param name="layer7.runInTerminateDriver" value="TestCaseA.tearDownClass"/>
 *     &lt;/driver>
 *
 *     &lt;testCase name="TestCaseA.testX">
 *         &lt;param name="layer7.className" value="TestCaseA"/>
 *         &lt;param name="layer7.methodName" value="testX"/>
 *         &lt;param name="layer7.runInPrepare" value="setUp"/>
 *         &lt;param name="layer7.runInFinish" value="tearDown"/>
 *     &lt;/testCase>
 *
 *     &lt;testCase name="TestCaseA.testY">
 *         &lt;param name="layer7.className" value="TestCaseA"/>
 *         &lt;param name="layer7.methodName" value="testY"/>
 *         &lt;param name="layer7.runInPrepare" value="setUp"/>
 *         &lt;param name="layer7.runInFinish" value="tearDown"/>
 *     &lt;/testCase>
 *
 * &lt;/testSuite>
 * </pre></blockquote>
 *
 * <p>This results in this execution sequence:
 * <ul>
 *  <li>TestCaseA.setUpClass()
 *  <li>
 *  <li>TestCaseA.TestCaseA() no-arg constructor
 *  <li>TestCaseA.setUp()
 *  <li>TestCaseA.testX() - 2 iterations for warm-up
 *  <li>TestCaseA.testX() - 10 iterations for performance measurement
 *  <li>TestCaseA.tearDown()
 *  <li>
 *  <li>TestCaseA.TestCaseA() no-arg constructor
 *  <li>TestCaseA.setUp()
 *  <li>TestCaseA.testY() - 2 iterations for warm-up
 *  <li>TestCaseA.testY() - 10 iterations for performance measurement
 *  <li>TestCaseA.tearDown()
 *  <li>
 *  <li>TestCaseA.tearDownClass()
 * </ul>
 * See <a href="https://japex.dev.java.net/docs/manual.html">Japex Manual</a>
 * for description of basic Japex parameters.
 * See {@link Constants} for description of parameters specific to this driver.
 *
 * @see com.l7tech.test.performance
 * @author rmak
 */
public class JUnitDriver extends JapexDriverBase {

    /** The current instance of the class to be tested; reassigned for each testCase. */
    private Object _object;

    /** The current method to be tested; reassigned for each testCase. */
    private Method _method;

    @Override
    public void initializeDriver() {
        try {
            for (Method method : parseClassAndMethods(getParam(Constants.RUN_IN_INITIALIZE_DRIVER), true)) {
                method.invoke(null);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void prepare(TestCase testCase) {
        try {
            final String className = testCase.getParam(Constants.CLASS_NAME);
            if (className == null) {
                throw new IllegalArgumentException("Missing parameter \"" + Constants.CLASS_NAME + "\".");
            }

            final String methodName = testCase.getParam(Constants.METHOD_NAME);
            if (methodName == null) {
                throw new IllegalArgumentException("Missing parameter \"" + Constants.METHOD_NAME + "\".");
            }

            // Instantiates an instance using the no-arg constructor.
            final Class clazz = getClass().getClassLoader().loadClass(className);
            _object = clazz.getConstructor().newInstance();
            _method = clazz.getMethod(methodName);

            for (Method method : parseMethods(clazz, testCase.getParam(Constants.RUN_IN_PREPARE))) {
                if (Japex.verbose) {
                    System.out.println("             " + Thread.currentThread().getName() + " invoking " + className + "." + method.getName() + "()");
                }
                method.invoke(_object);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void warmup(TestCase testCase) {
        run(testCase);
    }

    @Override
    public void run(TestCase testCase) {
        try {
            _method.invoke(_object);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void finish(TestCase testCase) {
        try {
            final Class clazz = _object.getClass();
            for (Method method : parseMethods(clazz, testCase.getParam(Constants.RUN_IN_FINISH))) {
                if (Japex.verbose) {
                    System.out.println("             " + Thread.currentThread().getName() + " invoking " + clazz.getName() + "." + method.getName() + "()");
                }
                method.invoke(_object);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void terminateDriver() {
        try {
            for (Method method : parseClassAndMethods(getParam(Constants.RUN_IN_TERMINATE_DRIVER), true)) {
                method.invoke(null);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parses a space-separated list of no-argument method in the form <i>class name</i>.<i>method name</i>.
     *
     * @param s                 a space-separated list of no-argument method in the form <i>class name</i>.<i>method name</i>; can be null
     * @param staticMethodOnly  look for static method only if true
     * @return list of methods; can be empty but never null
     * @throws ClassNotFoundException if a class is not available
     * @throws NoSuchMethodException if a method does not exist
     * @throws IllegalArgumentException if list is malformed, or a method is not static but <code>staticMethodOnly</code> is true
     */
    private List<Method> parseClassAndMethods(String s, boolean staticMethodOnly) throws ClassNotFoundException, NoSuchMethodException {
        if (s == null) {
            return Collections.emptyList();
        }

        final List<Method> methods = new ArrayList<Method>();
        for (String classAndMethod : s.split("\\s+")) {
            if (classAndMethod.length() == 0) continue;
            final int dot = classAndMethod.lastIndexOf('.');
            if (dot == -1) {
                throw new IllegalArgumentException("Missing . between class name and method name: " + classAndMethod);
            }

            final String className = classAndMethod.substring(0, dot);
            final String methodName = classAndMethod.substring(dot + 1, classAndMethod.length());
            if (className.length() == 0) {
                throw new IllegalArgumentException("Missing class name before dot: " + classAndMethod);
            }
            if (methodName.length() == 0) {
                throw new IllegalArgumentException("Missing method name after dot: " + classAndMethod);
            }

            final Class clazz = getClass().getClassLoader().loadClass(className);
            final Method method = clazz.getDeclaredMethod(methodName);
            method.setAccessible(true);
            if (staticMethodOnly) {
                if (! Modifier.isStatic(method.getModifiers())) {
                    throw new IllegalArgumentException("Method is not static: " + classAndMethod);
                }
            }
            methods.add(method);
        }
        return methods;
    }

    /**
     * Parses a space-separated list of no-argument method names.
     *
     * @param clazz     the class containing those methods
     * @param s         a space-separated list of no-argument method names; can be null
     * @return list of methods; can be empty but never null
     * @throws NoSuchMethodException if a method name does not exist in the given class
     */
    private List<Method> parseMethods(Class clazz, String s) throws NoSuchMethodException {
        if (s == null) {
            return Collections.emptyList();
        }

        final List<Method> methods = new ArrayList<Method>();
        for (String methodName : s.split("\\s+")) {
            if (methodName.length() == 0) continue;
            final Method method = clazz.getDeclaredMethod(methodName);
            method.setAccessible(true);
            methods.add(method);
        }
        return methods;
    }
}
