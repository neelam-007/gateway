/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class PolicyFactory {
    /**
     * Concrete implementations provide the product root package name
     * that contains the policy assertion classes.
     * For bridge this is "com.l7tech.proxy.policy.assertion" and for
     * server this is "com.l7tech.server.policy.assertion"
     *
     * @return the product specific package name
     */
    protected abstract String getProductRootPackageName();

    protected abstract String getProductClassnamePrefix();

    public static final String PACKAGE_PREFIX = "com.l7tech.policy.assertion";

    public List makeCompositePolicy(CompositeAssertion compositeAssertion) {
        Assertion child;
        List result = new ArrayList();
        for (Iterator i = compositeAssertion.children(); i.hasNext();) {
            child = (Assertion)i.next();
            result.add(makeSpecificPolicy(child));
        }
        return result;
    }

    protected Constructor getConstructor(Class genericAssertionClass) {
        try {
            Class specificClass = resolveProductClass(genericAssertionClass);
            return specificClass.getConstructor(new Class[]{genericAssertionClass});
        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe);
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        }
    }

    /**
     * Resolve the generic class to the product specific policy assertion class
     *
     * @param genericAssertionClass the generic assertion class
     * @return
     * @throws ClassNotFoundException if the resolved class could not be found
     * @throws RuntimeException if the class name could not be dermined
     * @see PolicyFactory#getProductRootPackageName()
     */
    protected Class resolveProductClass(Class genericAssertionClass)
      throws ClassNotFoundException, RuntimeException {
        String genericAssertionClassname = genericAssertionClass.getName();
        int ppos = genericAssertionClassname.lastIndexOf(".");
        if (ppos <= 0) throw new RuntimeException("Invalid classname " + genericAssertionClassname);
        String genericPackage = genericAssertionClassname.substring(0, ppos);
        String genericName = genericAssertionClassname.substring(ppos + 1);

        StringBuffer specificClassName = new StringBuffer(getProductRootPackageName());

        if (genericPackage.equals(PACKAGE_PREFIX)) {
            specificClassName.append(".");
            specificClassName.append(getProductClassnamePrefix());
            specificClassName.append(genericName);
        } else if (genericPackage.startsWith(PACKAGE_PREFIX)) {
            specificClassName.append(genericPackage.substring(PACKAGE_PREFIX.length()));
            specificClassName.append(".");
            specificClassName.append(getProductClassnamePrefix());
            specificClassName.append(genericName);
        } else
            throw new RuntimeException("Couldn't handle " + genericAssertionClassname);

        Class specificClass;
        specificClass = Class.forName(specificClassName.toString());
        return specificClass;
    }

    protected Object makeSpecificPolicy(Assertion genericAssertion) {
        try {
            Class genericClass = genericAssertion.getClass();

            return getConstructor(genericClass).newInstance(new Object[]{genericAssertion});
        } catch (InstantiationException ie) {
            throw new RuntimeException(ie);
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        } catch (InvocationTargetException ite) {
            throw new RuntimeException(ite);
        }
    }
}
