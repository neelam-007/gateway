/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy;

import com.l7tech.policy.PolicyFactory;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.common.util.ConstructorInvocation;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerPolicyFactory extends PolicyFactory implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    public ServerAssertion makeServerPolicy(Assertion rootAssertion) {
        return (ServerAssertion)makeSpecificPolicy(rootAssertion);
    }

    protected String getProductRootPackageName() {
        return "com.l7tech.server.policy.assertion";
    }

    protected String getProductClassnamePrefix() {
        return "Server";
    }

    protected Object makeSpecificPolicy(Assertion genericAssertion) {
        try {
            Class genericAssertionClass = genericAssertion.getClass();
            Class specificAssertionClass = resolveProductClass(genericAssertionClass);
            Constructor ctor = ConstructorInvocation.findMatchingConstructor(specificAssertionClass, new Class[]{genericAssertionClass, ApplicationContext.class});
            if (ctor != null) {
                return ctor.newInstance(new Object[]{genericAssertion, applicationContext});
            }
            return getConstructor(genericAssertionClass).newInstance(new Object[]{genericAssertion});
        } catch (InstantiationException ie) {
            throw new RuntimeException("Error creating specific assertion for '"+genericAssertion.getClass().getName()+"'", ie);
        } catch (IllegalAccessException iae) {
            throw new RuntimeException("Error creating specific assertion for '"+genericAssertion.getClass().getName()+"'", iae);
        } catch (InvocationTargetException ite) {
            throw new RuntimeException("Error creating specific assertion for '"+genericAssertion.getClass().getName()+"'", ite);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Error creating specific assertion for '"+genericAssertion.getClass().getName()+"'", e);
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext)
      throws BeansException {
        this.applicationContext = applicationContext;
    }
}
