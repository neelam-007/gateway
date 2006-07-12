/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy;

import com.l7tech.common.util.ConstructorInvocation;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.policy.AssertionLicense;
import com.l7tech.policy.PolicyFactoryUtil;
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
public class ServerPolicyFactory implements ApplicationContextAware {
    private final AssertionLicense licenseManager;
    private ApplicationContext applicationContext;
    static ThreadLocal<Boolean> licenseEnforcement = new ThreadLocal<Boolean>() {
        protected Boolean initialValue() {
            return null;
        }
    };

    public ServerPolicyFactory(AssertionLicense licenseManager) {
        this.licenseManager = licenseManager;
    }

    /**
     * Compile the specified generic policy tree using the specified license enforcement setting.
     *
     * @param genericAssertion  root of the assertion subtree to compile.  Must not be null.
     * @param licenseEnforcement  if true, the compilation will fail if an unlicensed assertion is referenced.
     *                            if false, no license enforcement will be performed.
     * @return the server assertion subtree for this generic subtree.  Never null.
     * @throws ServerPolicyException if this policy subtree could not be compiled
     */
    public ServerAssertion compilePolicy(Assertion genericAssertion, boolean licenseEnforcement) throws ServerPolicyException {
        try {
            ServerPolicyFactory.licenseEnforcement.set(licenseEnforcement);
            return doMakeServerAssertion(genericAssertion);
        } finally {
            ServerPolicyFactory.licenseEnforcement.set(null);
        }
    }

    /**
     * Compile the specified assertion subtree using the current license enforcement settings.
     * This should only be called from within server composite assertion constructors; others should use
     * {@link #compilePolicy} instead to set an initial license enforcement mode.
     *
     * @param genericAsertion  root of the assertion subtree to compile.  Must not be null.
     * @return the server assertion subtree for this generic subtree.  Never null.
     * @throws ServerPolicyException if this policy subtree could not be compiled
     */
    public ServerAssertion compileSubtree(Assertion genericAsertion) throws ServerPolicyException {
        return doMakeServerAssertion(genericAsertion);
    }

    /** Compile the specified assertion tree, with license enforcement. */
    private ServerAssertion doMakeServerAssertion(Assertion genericAssertion) throws ServerPolicyException {
        try {
            Boolean le = licenseEnforcement.get();
            if (le == null)
                throw new ServerPolicyException(genericAssertion, "No license enforcement state set; use compilePolicy() instead of compileSubtree()");
            if (le && !licenseManager.isAssertionEnabled(genericAssertion.getClass().getName()))
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
