package com.l7tech.server.policy;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.policy.AssertionLicense;
import com.l7tech.policy.assertion.*;
import com.l7tech.server.policy.assertion.ServerAcceleratedOversizedTextAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.Injector;
import com.l7tech.util.ConstructorInvocation;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.TarariLoader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is for getting a tree of ServerAssertion objects from the corresponding Assertion objects (data).
 */
public class ServerPolicyFactory implements ApplicationContextAware {
    protected static final Logger logger = Logger.getLogger(ServerPolicyFactory.class.getName());

    private final AssertionLicense licenseManager;
    private final Injector injector;
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

    public ServerPolicyFactory(final AssertionLicense licenseManager,
                               final Injector injector) {
        this.licenseManager = licenseManager;
        this.injector = injector;
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
        if (genericAssertion == null)
            throw new ServerPolicyException(null, "Root policy assertion is null -- policy contains no enabled assertions?");

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
            @Override
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
     * @return the compiled assertion.  Never null.
     */
    private ServerAssertion doMakeServerAssertion(Assertion genericAssertion) throws ServerPolicyException, LicenseException {
        if (genericAssertion == null)
            throw new ServerPolicyException(null, "Generic assertion is null");

        try {
            if (isLicenseEnforcement() && !licenseManager.isAssertionEnabled(genericAssertion))
                throw new LicenseException("The specified assertion is not supported on this Gateway: " + genericAssertion.getClass());

            // Disabled assertions and comment assertions are expected to have been filtered out (eg, by the PolicyCache) before making it here
            if (!genericAssertion.isEnabled())
                throw new ServerPolicyException(genericAssertion, "Assertion cannot be compiled because it is disabled");
            if (genericAssertion instanceof CommentAssertion)
                throw new ServerPolicyException(genericAssertion, "Assertion cannot be compiled because it is a comment assertion");

            // Prevent Tarari assertions from being loaded on non-Tarari SSGs
            // TODO find an abstraction for this assertion censorship
            if (TarariLoader.getGlobalContext() != null) {
                if (genericAssertion instanceof OversizedTextAssertion) {
                    final ServerAcceleratedOversizedTextAssertion serverAssertion =
                            new ServerAcceleratedOversizedTextAssertion((OversizedTextAssertion)genericAssertion, applicationContext);
                    injector.inject( serverAssertion );
                    return serverAssertion;
                }
            }

            Class genericAssertionClass = genericAssertion.getClass();
            String productClassname = (String)genericAssertion.meta().get(AssertionMetadata.SERVER_ASSERTION_CLASSNAME);
            if (productClassname == null)
                throw new ServerPolicyException(genericAssertion, "Error creating specific assertion for '"+genericAssertion.getClass().getName()+"': assertion declares no server implementation");

            final Thread currentThread = Thread.currentThread();
            final ClassLoader contextClassLoader = currentThread.getContextClassLoader();
            final ClassLoader assClassLoader = genericAssertion.getClass().getClassLoader();
            currentThread.setContextClassLoader( assClassLoader );
            try {
                Class specificAssertionClass = assClassLoader.loadClass(productClassname);

                if (!ServerAssertion.class.isAssignableFrom(specificAssertionClass))
                    throw new ServerPolicyException(genericAssertion, productClassname + " is not a ServerAssertion");

                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE,
                               MessageFormat.format("Instantiating server assertion of type {0} for assertion {1} ordinal: {2} in policy GOID: {3}",
                                                    specificAssertionClass.getName(),
                                                    genericAssertionClass.getName(),
                                                    genericAssertion.getOrdinal(),
                                                    genericAssertion.ownerPolicyGoid()));

                Class[][] patterns = new Class[][] {
                        new Class[] { genericAssertionClass },
                        new Class[] { genericAssertionClass, ApplicationContext.class }, // allows BeanFactory.class, etc
                };
                for (Class[] pattern : patterns) {
                    Constructor ctor = ConstructorInvocation.findMatchingConstructor(specificAssertionClass, pattern);
                    if (ctor != null) {
                        Object[] params = new Object[pattern.length];
                        if (params.length > 0)
                            params[0] = genericAssertion;
                        for (int i = 1; i < params.length; ++i)
                            params[i] = applicationContext;
                        final ServerAssertion serverAssertion = (ServerAssertion) ctor.newInstance(params);
                        injector.inject( serverAssertion );
                        if (serverAssertion instanceof InitializingBean) {
                            InitializingBean initializingBean = (InitializingBean) serverAssertion;
                            initializingBean.afterPropertiesSet();
                        }
                        return serverAssertion;
                    }
                }
            } finally {
                currentThread.setContextClassLoader( contextClassLoader );
            }

            throw new ServerPolicyException(genericAssertion, productClassname + " does not have at least a public constructor-from-" + genericAssertionClass);
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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
      throws BeansException {
        this.applicationContext = applicationContext;
    }

}
