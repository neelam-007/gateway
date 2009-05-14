/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.CommentAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientUnknownAssertion;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientPolicyFactory {
    public static ClientPolicyFactory getInstance() {
        if ( _instance == null ) _instance = new ClientPolicyFactory();
        return _instance;
    }

    public ClientAssertion makeClientPolicy(Assertion genericAssertion) throws PolicyAssertionException {
        if (genericAssertion instanceof CommentAssertion) return null;

        try {
            Class genericClass = genericAssertion.getClass();
            String clientClassname = (String)genericAssertion.meta().get(AssertionMetadata.CLIENT_ASSERTION_CLASSNAME);
            if (clientClassname == null)
                return makeUnknownAssertion(genericAssertion);
            Class clientClass = genericAssertion.getClass().getClassLoader().loadClass(clientClassname);
            Constructor ctor = clientClass.getConstructor(genericClass);
            return (ClientAssertion) ctor.newInstance(genericAssertion);
        } catch (InstantiationException ie) {
            throw new RuntimeException(ie);
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        } catch (InvocationTargetException ite) {
            throw new RuntimeException(ite);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            return makeUnknownAssertion(genericAssertion);
        }
    }

    protected ClientUnknownAssertion makeUnknownAssertion(Assertion genericAssertion) {
        return new ClientUnknownAssertion(genericAssertion);
    }

    private static ClientPolicyFactory _instance;
}
