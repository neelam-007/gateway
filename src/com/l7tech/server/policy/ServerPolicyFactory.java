/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy;

import com.l7tech.common.util.ConstructorInvocation;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.policy.PolicyFactory;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CommentAssertion;
import com.l7tech.policy.assertion.OversizedTextAssertion;
import com.l7tech.server.policy.assertion.ServerAcceleratedOversizedTextAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Constructor;

/**
 * This is for getting a tree of ServerAssertion objects from the corresponding Assertion objects (data).
 * @author alex
 */
public class ServerPolicyFactory extends PolicyFactory implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    public ServerAssertion makeServerPolicy(Assertion rootAssertion) throws ServerPolicyException {
        return (ServerAssertion)makeSpecificPolicy(rootAssertion);
    }

    protected String getProductRootPackageName() {
        return "com.l7tech.server.policy.assertion";
    }

    protected String getProductClassnamePrefix() {
        return "Server";
    }

    protected Object makeSpecificPolicy(Assertion genericAssertion) throws ServerPolicyException {
        try {
            // Prevent Tarari assertions from being loaded on non-Tarari SSGs
            // TODO find an abstraction for this assertion censorship
            if (TarariLoader.getGlobalContext() != null) {
                if (genericAssertion instanceof OversizedTextAssertion)
                    return new ServerAcceleratedOversizedTextAssertion((OversizedTextAssertion)genericAssertion, applicationContext);
            }

            if (genericAssertion instanceof CommentAssertion) return null;

            Class genericAssertionClass = genericAssertion.getClass();
            Class specificAssertionClass = resolveProductClass(genericAssertionClass);
            Constructor ctor = ConstructorInvocation.findMatchingConstructor(specificAssertionClass, new Class[]{genericAssertionClass, ApplicationContext.class});
            if (ctor != null) {
                return ctor.newInstance(new Object[]{genericAssertion, applicationContext});
            }
            return getConstructor(genericAssertionClass).newInstance(new Object[]{genericAssertion});
        } catch (Exception ie) {
            throw new ServerPolicyException(genericAssertion, "Error creating specific assertion for '"+genericAssertion.getClass().getName()+"'", ie);
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext)
      throws BeansException {
        this.applicationContext = applicationContext;
    }
}
