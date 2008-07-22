/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.util.ConstructorInvocation;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.TarariLoader;
import com.l7tech.policy.AssertionLicense;
import com.l7tech.policy.assertion.*;
import com.l7tech.server.policy.assertion.ServerAcceleratedOversizedTextAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.text.MessageFormat;

/**
 * This is for getting a tree of ServerAssertion objects from the corresponding Assertion objects (data).
 */
public class ServerPolicyFactory implements ApplicationContextAware {
    protected static final Logger logger = Logger.getLogger(ServerPolicyFactory.class.getName());

    private final AssertionLicense licenseManager;
    private ApplicationContext applicationContext;
    private static ThreadLocal<LinkedList<Boolean>> licenseEnforcement = new ThreadLocal<LinkedList<Boolean>>() {
        @Override
        protected LinkedList<Boolean> initialValue() {
            return new LinkedList<Boolean>();
        }
    };

    /**
     * Execute the provided Callable with license enforcement set (or unset), then restore it after it's done
     * @throws Exception alas.
     */
    static <T> T doWithEnforcement(boolean enforcement, Callable<T> callable) throws Exception {
        try {
            licenseEnforcement.get().push(enforcement);
            return callable.call();
        } finally {
            licenseEnforcement.get().pop();
        }
    }

    /**
     * @return the current license enforcement flag
     * @throws NullPointerException if the flag has not yet been set
     */
    boolean isLicenseEnforcement() throws NullPointerException {
        return licenseEnforcement.get().peek();
    }

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
     * @throws LicenseException if this policy subtree made use of an unlicensed assertion
     */
    public ServerAssertion compilePolicy(final Assertion genericAssertion, boolean licenseEnforcement) throws ServerPolicyException, LicenseException {
        if (licenseEnforcement) {
            // Scan for UnknownAssertion, and treat as license failure
            Iterator it = genericAssertion.preorderIterator();
            while (it.hasNext()) {
                final Object assertion = it.next();
                if (assertion instanceof UnknownAssertion)
                    throw new LicenseException(((UnknownAssertion)assertion).getDetailMessage());
            }
        }

        Callable<ServerAssertion> c = new Callable<ServerAssertion>() {
            public ServerAssertion call() throws Exception {
                return doMakeServerAssertion(genericAssertion);
            }
        };

        try {
            return doWithEnforcement(licenseEnforcement, c);
        } catch (ServerPolicyException e) {
            throw e;
        } catch (LicenseException e) {
            throw e;
        } catch (Exception e) {
            throw new ServerPolicyException(genericAssertion, e);
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
     * @throws LicenseException if this policy subtree made use of an unlicensed assertion
     */
    public ServerAssertion compileSubtree(Assertion genericAsertion) throws ServerPolicyException, LicenseException {
        return doMakeServerAssertion(genericAsertion);
    }

    /**
     * Compile the specified assertion tree, with license enforcement.
     */
    private ServerAssertion doMakeServerAssertion(Assertion genericAssertion) throws ServerPolicyException, LicenseException {
        try {
            if (isLicenseEnforcement() && !licenseManager.isAssertionEnabled(genericAssertion))
                throw new LicenseException("The specified assertion is not supported on this Gateway: " + genericAssertion.getClass());

            // Prevent Tarari assertions from being loaded on non-Tarari SSGs
            // TODO find an abstraction for this assertion censorship
            if (TarariLoader.getGlobalContext() != null) {
                if (genericAssertion instanceof OversizedTextAssertion)
                    return new ServerAcceleratedOversizedTextAssertion((OversizedTextAssertion)genericAssertion, applicationContext);
            }

            if (genericAssertion instanceof CommentAssertion) return null;

            Class genericAssertionClass = genericAssertion.getClass();
            String productClassname = (String)genericAssertion.meta().get(AssertionMetadata.SERVER_ASSERTION_CLASSNAME);
            if (productClassname == null)
                throw new ServerPolicyException(genericAssertion, "Error creating specific assertion for '"+genericAssertion.getClass().getName()+"': assertion declares no server implementation");
            Class specificAssertionClass = genericAssertion.getClass().getClassLoader().loadClass(productClassname);

            if (!ServerAssertion.class.isAssignableFrom(specificAssertionClass))
                throw new ServerPolicyException(genericAssertion, productClassname + " is not a ServerAssertion");

            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE,
                           MessageFormat.format("Instantiating server assertion of type {0} for assertion {1} ordinal: {2} in policy OID: {3}",
                                                specificAssertionClass.getName(),
                                                genericAssertionClass.getName(),
                                                genericAssertion.getOrdinal(),
                                                genericAssertion.ownerPolicyOid()));

            Constructor ctor = ConstructorInvocation.findMatchingConstructor(specificAssertionClass, new Class[] {genericAssertionClass, ApplicationContext.class});
            if (ctor != null)
                return (ServerAssertion)ctor.newInstance(genericAssertion, applicationContext);

            ctor = ConstructorInvocation.findMatchingConstructor(specificAssertionClass, new Class[] { genericAssertionClass });
            if (ctor == null)
                throw new ServerPolicyException(genericAssertion, productClassname + " does not have at least a public constructor-from-" + genericAssertionClass);
            return (ServerAssertion)ctor.newInstance(genericAssertion);
        } catch (LicenseException le) {
            throw le;
        } catch (InvocationTargetException ite) {
            // Handle exceptions thrown by the server assertion constructor
            Throwable cause = ite.getCause();
            if (cause instanceof LicenseException)
                throw (LicenseException) cause;
            throw new ServerPolicyException(genericAssertion, "Error creating specific assertion for '"+genericAssertion.getClass().getName()+"': " + ExceptionUtils.getMessage(ite), ite);
        } catch (Exception ie) {
            throw new ServerPolicyException(genericAssertion, "Error creating specific assertion for '"+genericAssertion.getClass().getName()+"': " + ExceptionUtils.getMessage(ie), ie);
        } catch (Throwable t) {
            throw new ServerPolicyException(genericAssertion, "Error creating specific assertion for '"+genericAssertion.getClass().getName()+"': " + ExceptionUtils.getMessage(t), t);
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext)
      throws BeansException {
        this.applicationContext = applicationContext;
    }

}
