package com.l7tech.remote.jini.export;

import com.sun.jini.config.Config;
import com.sun.jini.start.*;
import net.jini.config.Configuration;
import net.jini.export.ProxyAccessor;
import net.jini.loader.pref.PreferredClassLoader;
import net.jini.security.BasicProxyPreparer;
import net.jini.security.ProxyPreparer;
import net.jini.security.policy.DynamicPolicyProvider;
import net.jini.security.policy.PolicyFileProvider;

import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.MarshalledObject;
import java.rmi.RMISecurityManager;
import java.security.AllPermission;
import java.security.Permission;
import java.security.Policy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class used to launch shared, non-activatable, in-process
 * services. Clients construct this object with the details
 * of the service to be launched, then call <code>create</code>
 * to launch the service in invoking object's VM.
 * <P>
 * This is the modified ServiceDescriptor implementation that implements
 * classloader related changes to enable services within the servlet
 * container.
 * <P>
 * Services need to implement the following "non-activatable
 * constructor":
 * <blockquote><pre>&lt;impl&gt;(String[] args, LifeCycle lc)</blockquote></pre>
 * where,
 * <UL>
 * <LI>args - are the service configuration arguments
 * <LI>lc   - is the hosting environment's
 * {@link com.sun.jini.start.LifeCycle} reference.
 * </UL>
 */

public class DefaultServiceDescriptor
  implements ServiceDescriptor {
    /**
     * The parameter types for the "activation constructor".
     */
    private static final Class[] actTypes = {
        String[].class, LifeCycle.class
    };

    private final String codebase;
    private final String policy;
    private final String classpath;
    private final String implClassName;
    private final String[] serverConfigArgs;
    private final LifeCycle lifeCycle;

    private static LifeCycle NoOpLifeCycle =
      new LifeCycle() { // default, no-op object
          public boolean unregister(Object impl) {
              return false;
          }
      };

    // currently unused
    private static AggregatePolicyProvider globalPolicy = null;
    private static Policy initialGlobalPolicy = null;
    private static final Logger logger = Logger.getLogger(DefaultServiceDescriptor.class.getName());
    /**
     * Component name for service starter configuration entries
     */
    private static final String START_PACKAGE = "com.sun.jini.start";


    /**
     * Object returned by
     * {@link com.sun.jini.start.NonActivatableServiceDescriptor#create(net.jini.config.Configuration)
     * NonActivatableServiceDescriptor.create()}
     * method that returns the proxy and implementation references
     * for the created service.
     */
    public static class Created {
        /**
         * The reference to the proxy of the created service
         */
        public final Object proxy;
        /**
         * The reference to the implementation of the created service
         */
        public final Object impl;

        /**
         * Constructs an instance of this class.
         *
         * @param impl  reference to the implementation of the created service
         * @param proxy reference to the proxy of the created service
         */
        public Created(Object impl, Object proxy) {
            this.proxy = proxy;
            this.impl = impl;
        }//end constructor
    }//end class Created

    /**
     * Trivial constructor. Simply assigns given parameters to
     * their associated, internal fields.
     *
     * @param codebase      location where clients can download required
     *                      service-related classes (for example, stubs, proxies, etc.).
     *                      Codebase components must be separated by spaces in which
     *                      each component is in <code>URL</code> format.
     * @param policy        server policy filename or URL
     * @param classpath     location where server implementation
     *                      classes can be found. Classpath components must be separated
     *                      by path separators.
     * @param implClassName name of server implementation class
     * @param serverConfig  service configuration arguments
     * @param lifeCycle     <code>LifeCycle</code> reference
     *                      for hosting environment
     */
    public DefaultServiceDescriptor(// Required Args
      String codebase,
      String policy,
      String classpath,
      String implClassName,
      // Optional Args
      String serverConfig,
      LifeCycle lifeCycle) {
        if (policy == null ||
          implClassName == null)
            throw new NullPointerException("Policy, and implementation cannot be null");

        if (serverConfig != null) {
            URL url = getClass().getClassLoader().getResource(serverConfig);
            if (url != null) {
                String cfg = url.toString();
                logger.log(Level.FINEST, "Retrieving service config from: {0}", cfg);
                serverConfig = cfg;
            }
        }

        URL url = getClass().getClassLoader().getResource(policy);
        if (url != null) {
            String pol = url.toString();
            logger.log(Level.FINEST, "Setting service policy to: {0}", pol);
            this.policy = pol;
        } else {
            this.policy = policy;
        }

        this.codebase = codebase;
        this.classpath = classpath;
        this.implClassName = implClassName;
        this.serverConfigArgs = new String[]{serverConfig};
        this.lifeCycle = (lifeCycle == null) ? NoOpLifeCycle : lifeCycle;
    }

    /**
     * Trivial constructor. Equivalent to calling the other overloaded
     * contructor with <code>null</code> for the <code>LifeCycle</code>
     * reference.
     */
    public DefaultServiceDescriptor(// Required Args
      String codebase,
      String policy,
      String classpath,
      String implClassName,
      // Optional Args
      String serverConfig) {
        this(codebase, policy, classpath, implClassName, serverConfig, null);
    }

    /**
     * Codebase accessor method.
     *
     * @return the codebase string associated with this service descriptor.
     */
    final public String getCodebase() {
        return codebase;
    }

    /**
     * Policy accessor method.
     *
     * @return the policy string associated with this service descriptor.
     */
    final public String getPolicy() {
        return policy;
    }

    /**
     * Classpath accessor method.
     *
     * @return the classpath string associated with this service descriptor.
     */
    final public String getClasspath() {
        return classpath;
    }

    /**
     * Implementation class accessor method.
     *
     * @return the implementation class string associated with
     *         this service descriptor.
     */
    final public String getImplClassName() {
        return implClassName;
    }

    /**
     * Service configuration arguments accessor method.
     *
     * @return the service configuration arguments associated with
     *         this service descriptor.
     */
    final public String[] getServerConfigArgs() {
        return (serverConfigArgs != null)
          ? (String[])serverConfigArgs.clone()
          : null;
    }

    /**
     * <code>LifeCycle</code> accessor method.
     *
     * @return the <code>LifeCycle</code> object associated with
     *         this service descriptor.
     */
    final public LifeCycle getLifeCycle() {
        return lifeCycle;
    }

    /**
     * Method that attempts to create a service based on the service
     * description information provided via constructor parameters.
     * <P>
     * This method:
     * <UL>
     * <LI> creates an <code>ActivateWrapper.ExportClassLoader</code> with
     * the provided import and export codebases
     * <LI> associates the newly created
     * <code>ExportClassLoader</code> and the corresponding service
     * policy file with the <code>AggregatePolicyProvider</code>
     * <LI> sets the newly created <code>ExportClassLoader</code> as
     * the context class loader
     * <LI> loads the service object's class and calls a constructor
     * with the following signature:
     * <blockquote><pre>&lt;impl&gt;(String[], LifeCycle)</blockquote></pre>
     * <LI> The service proxy is obtained by calling
     * {@link com.sun.jini.start.ServiceProxyAccessor#getServiceProxy() ServiceProxyAccessor.getServiceProxy()}
     * or
     * {@link net.jini.export.ProxyAccessor#getProxy() ProxyAccessor.getProxy()},
     * respectively, on the implementation instance.
     * If neither interface is supported, the
     * proxy reference is set to <code>null</code>
     * <LI> resets the context class loader to the original
     * context classloader
     * </UL>
     * The first invocation of this method will also replace the VM's
     * existing <code>Policy</code> object, if any,
     * with an <code>AggregatePolicyProvider</code>.
     *
     * @return a
     *         {@link com.sun.jini.start.NonActivatableServiceDescriptor.Created
     *         Created} instance with the service's proxy and implementation
     *         references.
     */
    public Object create(Configuration config) throws Exception {
        ensureSecurityManager();
        logger.entering(DefaultServiceDescriptor.class.getName(),
          "create", new Object[]{config});
        if (config == null) {
            throw new NullPointerException("Configuration argument cannot be null");
        }
        ProxyPreparer servicePreparer =
          (ProxyPreparer)Config.getNonNullEntry(config,
            START_PACKAGE,
            "servicePreparer", ProxyPreparer.class,
            new BasicProxyPreparer());

        Object proxy = null;
        Object impl = null;
        //Create custom class loader that preserves codebase annotations
        ClassLoader newClassLoader = null;
        Thread curThread = Thread.currentThread();
        URLClassLoader oldClassLoader = (URLClassLoader)curThread.getContextClassLoader();

        String codeBase = getCodebase();
        if (codeBase == null) codeBase = "";
        String classPath = getClasspath();
        if (classPath == null) classPath = "";
        newClassLoader = new PreferredClassLoader(oldClassLoader.getURLs(), oldClassLoader, codeBase, false);

//        synchronized (DefaultServiceDescriptor.class) {
//            // supplant global policy 1st time through
//            if (globalPolicy == null) {
//                initialGlobalPolicy = Policy.getPolicy();
//                globalPolicy =
//                  new AggregatePolicyProvider(initialGlobalPolicy);
//                Policy.setPolicy(globalPolicy);
//                logger.log(Level.FINEST,
//                           "Global policy set: {0}", globalPolicy);
//            }
//            DynamicPolicyProvider service_policy =
//              new DynamicPolicyProvider(
//                new PolicyFileProvider(getPolicy()));
//            LoaderSplitPolicyProvider split_service_policy =
//              new LoaderSplitPolicyProvider(
//                newClassLoader, service_policy,
//                new DynamicPolicyProvider(initialGlobalPolicy));
//            /* Grant "this" code enough permission to do its work
//             * under the service policy, which takes effect (below)
//             * after the context loader is (re)set.
//             */
//            split_service_policy.grant(
//              this.getClass(),
//              null, /* Principal[] */
//              new Permission[]{new AllPermission()});
//            globalPolicy.setPolicy(newClassLoader, split_service_policy);
//        }

        curThread.setContextClassLoader(newClassLoader);

        try {
            Class implClass = null;
            implClass = Class.forName(getImplClassName(), false, newClassLoader);
            logger.finest("Attempting to get implementation constructor");
            Constructor constructor =
              implClass.getDeclaredConstructor(actTypes);
            logger.log(Level.FINEST,
              "Obtained implementation constructor: {0}",
              constructor);
            constructor.setAccessible(true);
            impl =
              constructor.newInstance(new Object[]{getServerConfigArgs(), lifeCycle});
            logger.log(Level.FINEST,
              "Obtained implementation instance: {0}", impl);
            if (impl instanceof ServiceProxyAccessor) {
                proxy = ((ServiceProxyAccessor)impl).getServiceProxy();
            } else if (impl instanceof ProxyAccessor) {
                proxy = ((ProxyAccessor)impl).getProxy();
            } else {
                proxy = null; // just for insurance
            }

            if (proxy != null) {
                proxy = servicePreparer.prepareProxy(proxy);
            }

            logger.log(Level.FINEST, "Proxy =  {0}", proxy);
            curThread.setContextClassLoader(oldClassLoader);
//TODO - factor in code integrity for MO
            proxy = (new MarshalledObject(proxy)).get();
        } finally {
            curThread.setContextClassLoader(oldClassLoader);
        }
        Created created = new Created(impl, proxy);
        logger.exiting(DefaultServiceDescriptor.class.getName(),
          "create", created);
        return created;
    }

    /**
     * Utility routine that sets a security manager if one isn't already
     * present.
     */
    private void ensureSecurityManager() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
    }

    /**
     * Utility method that converts a <code>URL[]</code>
     * into a corresponding, space-separated string with
     * the same array elements.
     * <p/>
     * Note that if the array has zero elements, the return value is
     * null, not the empty string.
     */
    private static String urlsToPath(URL[] urls) {
//TODO - check if spaces in file paths are properly escaped (i.e.% chars)
        if (urls.length == 0) {
            return null;
        } else if (urls.length == 1) {
            return urls[0].toExternalForm();
        } else {
            StringBuffer path = new StringBuffer(urls[0].toExternalForm());
            for (int i = 1; i < urls.length; i++) {
                path.append(' ');
                path.append(urls[i].toExternalForm());
            }
            return path.toString();
        }
    }


    public String toString() {
        ArrayList fields = new ArrayList(6);
        fields.add(codebase != null ? codebase : "'null' codebase");
        fields.add(policy);
        fields.add(classpath != null ? classpath : "'null' classpath");
        fields.add(implClassName);
        fields.add(Arrays.asList(serverConfigArgs));
        fields.add(lifeCycle);
        return fields.toString();
    }
}

