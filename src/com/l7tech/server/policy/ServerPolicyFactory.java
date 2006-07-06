/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy;

import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.util.ConstructorInvocation;
import com.l7tech.common.LicenseManager;
import com.l7tech.policy.PolicyFactoryUtil;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CommentAssertion;
import com.l7tech.policy.assertion.OversizedTextAssertion;
import com.l7tech.server.policy.assertion.ServerAcceleratedOversizedTextAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.GatewayFeatureSets;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Constructor;

/**
 * This is for getting a tree of ServerAssertion objects from the corresponding Assertion objects (data).
 * @author alex
 */
public class ServerPolicyFactory implements ApplicationContextAware {
    private final LicenseManager licenseManager;
    private ApplicationContext applicationContext;

    public ServerPolicyFactory(LicenseManager licenseManager) {
        this.licenseManager = licenseManager;
    }

    private boolean isAssertionEnabled(Class<? extends Assertion> assertionClass) {
        return licenseManager.isFeatureEnabled(GatewayFeatureSets.getFeatureForAssertionClassname(assertionClass.getName()));
    }

    public ServerAssertion makeServerAssertion(Assertion genericAssertion) throws ServerPolicyException {
        try {
            if (!isAssertionEnabled(genericAssertion.getClass())) 
                throw new ServerPolicyException(genericAssertion,
                                                "The specified assertion is not supported on this Gateway: " +
                                                        genericAssertion.getClass());

            // Prevent Tarari assertions from being loaded on non-Tarari SSGs
            // TODO find an abstraction for this assertion censorship
            if (TarariLoader.getGlobalContext() != null) {
                if (genericAssertion instanceof OversizedTextAssertion)
                    return new ServerAcceleratedOversizedTextAssertion((OversizedTextAssertion)genericAssertion, applicationContext);
            }

            if (genericAssertion instanceof CommentAssertion) return null;

            Class genericAssertionClass = genericAssertion.getClass();
            String productClassname = PolicyFactoryUtil.getProductClassname(genericAssertionClass, "com.l7tech.server.policy.assertion", "Server");
            Class specificAssertionClass = Class.forName(productClassname);

            if (!ServerAssertion.class.isAssignableFrom(specificAssertionClass))
                throw new ServerPolicyException(genericAssertion, productClassname + " is not a ServerAssertion");

            Constructor ctor = ConstructorInvocation.findMatchingConstructor(specificAssertionClass, new Class[] {genericAssertionClass, ApplicationContext.class});
            if (ctor != null)
                return (ServerAssertion)ctor.newInstance(genericAssertion, applicationContext);

            ctor = ConstructorInvocation.findMatchingConstructor(specificAssertionClass, new Class[] { genericAssertionClass });
            return (ServerAssertion)ctor.newInstance(genericAssertion);
        } catch (Exception ie) {
            throw new ServerPolicyException(genericAssertion, "Error creating specific assertion for '"+genericAssertion.getClass().getName()+"'", ie);
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext)
      throws BeansException {
        this.applicationContext = applicationContext;
    }

}
