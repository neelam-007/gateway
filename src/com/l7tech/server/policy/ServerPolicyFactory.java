/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy;

import com.l7tech.common.util.ConstructorInvocation;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.policy.PolicyFactory;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.policy.assertion.ResponseXpathAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.ServerRequestAcceleratedXpathAssertion;
import com.l7tech.server.policy.assertion.ServerResponseAcceleratedXpathAssertion;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * This is for getting a tree of ServerAssertion objects from the corresponding Assertion objects (data).
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
            // Prevent Tarari assertions from being loaded on non-Tarari SSGs
            // TODO find an abstraction for this assertion censorship
            if (TarariLoader.getGlobalContext() != null) {
                if (genericAssertion instanceof RequestXpathAssertion)
                    return new ServerRequestAcceleratedXpathAssertion((RequestXpathAssertion)genericAssertion, applicationContext);

                if (genericAssertion instanceof ResponseXpathAssertion)
                    return new ServerResponseAcceleratedXpathAssertion((ResponseXpathAssertion)genericAssertion, applicationContext);
            }

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
        } catch (UnimplementedAssertionException e) {
            throw new RuntimeException("Error creating specific assertion for '"+genericAssertion.getClass().getName()+"'", e);
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext)
      throws BeansException {
        this.applicationContext = applicationContext;
    }
}
