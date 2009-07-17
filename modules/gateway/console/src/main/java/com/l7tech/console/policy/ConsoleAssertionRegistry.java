package com.l7tech.console.policy;

import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.util.ConstructorInvocation;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.ClassUtils;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.console.action.DefaultAssertionPropertiesAction;
import com.l7tech.console.panels.AssertionPropertiesEditor;
import com.l7tech.console.panels.DefaultAssertionPropertiesEditor;
import com.l7tech.console.tree.AbstractAssertionPaletteNode;
import com.l7tech.console.tree.DefaultAssertionPaletteNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.DefaultAssertionPolicyNode;
import com.l7tech.console.tree.policy.advice.Advice;
import com.l7tech.console.tree.policy.advice.DefaultAssertionAdvice;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.CustomAssertionRMIClassLoaderSpi;
import com.l7tech.util.FilterClassLoader;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import static com.l7tech.policy.assertion.DefaultAssertionMetadata.*;
import com.l7tech.policy.assertion.MetadataFinder;
import com.l7tech.policy.wsp.ClassLoaderUtil;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.SecureClassLoader;
import java.security.ProtectionDomain;
import java.security.AllPermission;
import java.security.cert.Certificate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.IOException;

/**
 * The AssertionRegistry for the SecureSpan Manager.  Extends the basic registry by adding functionality
 * for finding default GUI-related classes to the DefaultAssertionMetadata default metadata finders.
 */
public class ConsoleAssertionRegistry extends AssertionRegistry {
    protected static final Logger logger = Logger.getLogger(ConsoleAssertionRegistry.class.getName());

    /** Prototype instances of assertions loaded from the server. */
    private final Set<Assertion> modulePrototypes = new HashSet<Assertion>();

    /** Base packages of every modular assertion, for recognizing NoClassDefFoundErrors due to module unload. */
    private final Map<String, String> moduleNameByBasePackage = new ConcurrentHashMap<String, String>();

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        //
        // Add metadata default getters that are specific to the SSM environment
        //

        putDefaultGetter(PALETTE_NODE_FACTORY, new PaletteNodeFactoryMetadataFinder());

        putDefaultGetter(POLICY_ADVICE_INSTANCE, new PolicyAdviceInstanceMetadataFinder());

        putDefaultGetter(POLICY_NODE_FACTORY, new PolicyNodeFactoryMetadataFinder());

        putDefaultGetter(POLICY_NODE_NAME, new MetadataFinder() {
            private String getDefaultName(AssertionMetadata meta) {
                Object name = meta.get(SHORT_NAME);
                if (name != null)
                    return name.toString();
                return meta.getAssertionClass().getSimpleName();
            }

            @Override
            public Object get(final AssertionMetadata meta, String key) {
                return cache(meta, key, getDefaultName(meta));
            }
        });

        putDefaultGetter(PROPERTIES_ACTION_FACTORY, new PropertiesActionMetadataFinder());

        putDefaultGetter(PROPERTIES_EDITOR_FACTORY, new PropertiesEditorFactoryMetadataFinder());
    }

    public void updateModularAssertions() {
        for (Assertion prototype : modulePrototypes)
            unregisterAssertion(prototype);
        if (!TopComponents.getInstance().isApplet())
            CustomAssertionRMIClassLoaderSpi.resetRemoteClassLoader();
        moduleNameByBasePackage.clear();

        try {
            ClusterStatusAdmin cluster = Registry.getDefault().getClusterStatusAdmin();
            Collection<ClusterStatusAdmin.ModuleInfo> modules = cluster.getAssertionModuleInfo();
            for (ClusterStatusAdmin.ModuleInfo module : modules)
                registerAssertionsFromModule(module);
        } catch (RuntimeException e) {
            if (ExceptionUtils.causedBy(e, NoSuchMethodException.class)) {
                logger.fine("Gateway does not support modular assertions");
                return;
            }
            else if (ExceptionUtils.causedBy(e, LicenseException.class)) {
                logger.fine("Gateway license error when getting modular assertion info");
                throw e;
            }

            throw new RuntimeException("Unexpected error getting modular assertion info: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void registerAssertionsFromModule(final ClusterStatusAdmin.ModuleInfo module) {
        final Collection<String> assertionClassnames = module.assertionClasses;

        final String moduleFilename = module.moduleFilename;

        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                ClassLoader assloader = TopComponents.getInstance().isApplet()
                                        ? getAppletModularAssertionClassLoader(getRootPackage(assertionClassnames))
                                        : null;
                if (assloader == null)
                    assloader = ClassLoaderUtil.getClassLoader();
                if (assloader == null)
                    throw new IllegalStateException("Not running as applet but no WSP class loader set");
                for (String assertionClassname : assertionClassnames) {
                    try {
                        Class assclass = assloader.loadClass(assertionClassname);
                        if (!Assertion.class.isAssignableFrom(assclass))
                            throw new ClassCastException(assclass.getName());
                        Assertion prototype = (Assertion)assclass.newInstance();
                        String basePackage = String.valueOf(prototype.meta().get(AssertionMetadata.BASE_PACKAGE));

                        logger.info("Registering remote assertion " + prototype.getClass().getName());
                        modulePrototypes.add(prototype);
                        registerAssertion(prototype.getClass());
                        if (basePackage.length() > 0)
                            moduleNameByBasePackage.put(basePackage, module.moduleFilename);
                    } catch (NoClassDefFoundError e) {
                        logger.log(Level.WARNING, "Unable to load remote class " + assertionClassname + " from module " + moduleFilename + ": " + ExceptionUtils.getMessage(e), e);
                    } catch (ClassNotFoundException e) {
                        logger.log(Level.WARNING, "Unable to load remote class " + assertionClassname + " from module " + moduleFilename + ": " + ExceptionUtils.getMessage(e), e);
                    } catch (ClassCastException e) {
                        logger.log(Level.WARNING, "Remote Assertion class does not extend Assertion: " + assertionClassname + " (from module " + moduleFilename + ")");
                    } catch (IllegalAccessException e) {
                        logger.log(Level.WARNING, "Unable to instantiate remote Assertion class " + assertionClassname + " from module " + moduleFilename + ": " + ExceptionUtils.getMessage(e), e);
                    } catch (InstantiationException e) {
                        logger.log(Level.WARNING, "Unable to instantiate remote Assertion class " + assertionClassname + " from module " + moduleFilename + ": " + ExceptionUtils.getMessage(e), e);
                    }
                }
                return null;
            }
        });
    }

    /**
     * Get the shared parent package for the given assertoin classes.
     *
     *   com.l7tech.external.assertions.myass.MyAssertion
     *   com.l7tech.external.assertions.myass.MyAssertion2
     *
     * parent is "com.l7tech.external.assertions.myass"
     *
     *   com.l7tech.external.assertions.myass1.MyAssertion
     *   com.l7tech.external.assertions.myass2.MyAssertion2
     *
     * parent is "com.l7tech.external.assertions"
     */
    private String getRootPackage( final Collection<String> assertionClassnames ) {
        String packageName = "com.l7tech.external.assertions";

        if ( assertionClassnames != null && !assertionClassnames.isEmpty() ) {
            packageName = ClassUtils.getPackageName( assertionClassnames.iterator().next() );

            for ( String className : assertionClassnames ) {
                String classPackageName = ClassUtils.getPackageName( className );
                if ( !classPackageName.equals(packageName) &&
                     ClassUtils.getPackageName(packageName).equals( ClassUtils.getPackageName(classPackageName) ) ) {
                    packageName = ClassUtils.getPackageName(packageName);
                    break; // Only allow one package up, so any assertions in packages must have common parent package
                }
            }
        }

        return packageName;
    }

    /**
     * Get the classloader to use for the modular assertion.
     *
     * If untrusted, this is the regular classloader. If trusted a
     * classloader is created to load only the modular assertion classes
     * as trusted classes.
     */
    private ClassLoader getAppletModularAssertionClassLoader( final String assertionPackage ) {
        ClassLoader loader = getClass().getClassLoader();

        // Load classes with permission to access required property
        try {
            final ClassLoader resourceLoader = loader;
            final CodeSource cs = new CodeSource(new URL("file:/opt/SecureSpan/Manager/lib/assertions/" + assertionPackage), (Certificate[])null);
            final Permissions permissions = new Permissions();
            permissions.add(new AllPermission()); 
            final ProtectionDomain protectionDomain = new ProtectionDomain(cs, permissions);

            loader = new SecureClassLoader( new FilterClassLoader(resourceLoader, null, Collections.singleton(assertionPackage), false) ) {
                @Override
                public Class<?> findClass(final String name) throws ClassNotFoundException {
                    InputStream resIn = null;
                    try {
                        URL resUrl = null;
                        if ( name.startsWith( assertionPackage )) {
                            String resName = name.replace(".", "/").concat(".class");
                            resUrl = resourceLoader.getResource(resName);
                        }
                        if (resUrl == null)
                            throw new ClassNotFoundException("Resource not found for class '" + name + "'.");

                        resIn = resUrl.openStream();
                        byte[] classData = IOUtils.slurpStream(resIn, 102400);
                        return defineClass(name, classData, 0, classData.length, protectionDomain);
                    } catch(IOException ioe) {
                        throw new ClassNotFoundException("Error loading resource for class '" + name + "'.", ioe);
                    } finally {
                        ResourceUtils.closeQuietly( resIn );
                    }
                }
            };
        }
        catch( MalformedURLException mue ) {
            logger.log( Level.WARNING, "Error creating modular assertion class loader", mue );
        }
        catch(SecurityException se) {
            logger.log( Level.INFO, "Not able to create modular assertion class loader, assertion will be untrusted.", ExceptionUtils.getDebugException(se));
        }

        return loader;
    }

    /**
     * Examines the provided path to see if it looks like it might be referring to a loaded module.
     * If so, returns the name of the possibly-matching module.
     *
     * @param path the path to examine, ie "com/l7tech/external/assertions/echorouting/console/EchoRoutingPropertiesDialog$2"
     * @return the name of a matching module, ie "ComparisonAssertion-3.7.0.aar", or null.
     */
    public String getModuleNameMatchingPackage(String path) {
        if (path == null || path.length() < 2)
            return null;

        path = path.replace('/', '.');
        for (Map.Entry<String, String> entry : moduleNameByBasePackage.entrySet()) {
            String pack = entry.getKey();
            if (path.startsWith(pack))
                return entry.getValue();
        }

        return null;
    }

    /** @return true if at least one modular assertion is currently registered. */
    public boolean isAnyModularAssertionRegistered() {
        return !modulePrototypes.isEmpty();
    }

    private static class PaletteNodeFactoryMetadataFinder<AT extends Assertion> implements MetadataFinder {
        @Override
        public Object get(AssertionMetadata meta, String key) {
            String classname = (String)meta.get(PALETTE_NODE_CLASSNAME);
            //noinspection unchecked
            final Class<AT> assclass = meta.getAssertionClass();
            Functions.Unary<AbstractAssertionPaletteNode, AT> factory =
                    findPaletteNodeFactory(assclass.getClassLoader(), classname, assclass);
            if (factory != null)
                return cache(meta, key, factory);

            // Try to use the default
            factory = new Functions.Unary<AbstractAssertionPaletteNode, AT>() {
                @Override
                public AbstractAssertionPaletteNode call(AT assertion) {
                    return new DefaultAssertionPaletteNode<AT>(assertion);
                }
            };
            return cache(meta, key, factory);
        }
    }

    private static class PolicyNodeFactoryMetadataFinder<AT extends Assertion> implements MetadataFinder {
        @Override
        public Object get(AssertionMetadata meta, String key) {
            String classname = (String)meta.get(POLICY_NODE_CLASSNAME);
            //noinspection unchecked
            final Class<AT> assclass = meta.getAssertionClass();
            Functions.Unary<AssertionTreeNode, AT> factory =
                    findPolicyNodeFactory(assclass.getClassLoader(), classname, assclass);
            if (factory != null)
                return cache(meta, key, factory);

            // Try to use the default
            factory = new Functions.Unary< AssertionTreeNode, AT >() {
                @Override
                public AssertionTreeNode call(AT assertion) {
                    return new DefaultAssertionPolicyNode<AT>(assertion);
                }
            };
            return cache(meta, key, factory);
        }
    }

    private static class PolicyAdviceInstanceMetadataFinder implements MetadataFinder {
        @Override
        public Object get(AssertionMetadata meta, String key) {
            String assname = meta.getAssertionClass().getName();
            String classname = (String)meta.get(POLICY_ADVICE_CLASSNAME);

            if (classname == null || "none".equalsIgnoreCase(classname.trim()))
                return cache(meta, key, null);

            if ("default".equalsIgnoreCase(classname.trim()) || "auto".equalsIgnoreCase(classname.trim()))
                return cache(meta, key, new DefaultAssertionAdvice());

            try {
                Class adviceClass  = meta.getAssertionClass().getClassLoader().loadClass(classname);

                if (Advice.class.isAssignableFrom(adviceClass))
                    return cache(meta, key, adviceClass.newInstance());

                logger.warning("Policy Advice class for assertion " + assname + " does not implement Advice interface");
                // Fallthrough and return null

            } catch (ClassNotFoundException e) {
                // Probably was just a generated-by-default classname that doesn't actually exist
                logger.log(Level.FINEST, "Unable to load advice class", e);
                // Fallthrough and return null
            } catch (IllegalAccessException e) {
                logger.log(Level.WARNING, "Unable to instantiate advice class for assertion " + assname, e);
                // Fallthrough and return null
            } catch (InstantiationException e) {
                logger.log(Level.WARNING, "Unable to instantiate advice class for assertion " + assname, e);
                // Fallthrough and return null
            }
            return cache(meta, key, null);
        }
    }

    private static class PropertiesActionMetadataFinder<AT extends Assertion> implements MetadataFinder {
        @Override
        public Object get(AssertionMetadata meta, String key) {
            String classname = (String)meta.get(PROPERTIES_ACTION_CLASSNAME);
            //noinspection unchecked
            final Class<AT> assclass = meta.getAssertionClass();
            String assname = assclass.getName();
            Functions.Unary<Action, AssertionTreeNode> factory = findPropertiesActionFactory(assclass.getClassLoader(), classname, assname);
            if (factory != null)
                return cache(meta, key, factory);
            // Try using the default action
            //noinspection unchecked
            final Functions.Binary<AssertionPropertiesEditor< AT >, Frame, AT > apeFactory =
                    (Functions.Binary<AssertionPropertiesEditor< AT >, Frame, AT>)
                            meta.get(PROPERTIES_EDITOR_FACTORY);
            if (apeFactory == null) {
                // No APE for this assertion = no "Properties..." action
                return cache(meta, key, null);
            }

            factory = new Functions.Unary<Action, AssertionTreeNode>() {
                @Override
                public Action call(AssertionTreeNode assertionTreeNode) {
                    //noinspection unchecked
                    return new DefaultAssertionPropertiesAction<AT, AssertionTreeNode<AT>>(assertionTreeNode, apeFactory);
                }
            };
            return cache(meta, key, factory);
        }
    }

    private static class PropertiesEditorFactoryMetadataFinder<AT extends Assertion> implements MetadataFinder {
        @Override
        public Object get(AssertionMetadata meta, String key) {
            //noinspection unchecked
            final Class<AT> assclass = meta.getAssertionClass();
            String apeClassname = (String)meta.get(PROPERTIES_EDITOR_CLASSNAME);
            Functions.Binary<AssertionPropertiesEditor<AT>, Frame, AT> factory =
                    findPropertiesEditorFactory(assclass.getClassLoader(), apeClassname, assclass);
            if (factory != null)
                return cache(meta, key, factory);

            // No default available -- can we use the default bean editor?
            if (DefaultAssertionPropertiesEditor.hasEditableProperties(assclass)) {
                // Yep -- use the default
                return cache(meta, key, new Functions.Binary<AssertionPropertiesEditor<AT>, Frame, AT>() {
                    @Override
                    public AssertionPropertiesEditor<AT> call(Frame frame, AT at) {
                        return new DefaultAssertionPropertiesEditor<AT>(frame, at);
                    }
                });
            }

            // Nope -- no properties editor here
            return cache(meta, key, null);
        }
    }

    private static <AT extends Assertion> Functions.Unary<AbstractAssertionPaletteNode, AT>
    findPaletteNodeFactory(ClassLoader loader, String classname, Class<AT> assclass) {
        if (classname == null)
            return null;

        final String assname = assclass.getName();
        try {
            try {
                Functions.Unary<AbstractAssertionPaletteNode, AT> factory =
                        ConstructorInvocation.createFactoryOutOfUnaryConstructor(loader,
                                                                                 classname,
                                                                                 AbstractAssertionPaletteNode.class,                                                                              assclass);
                if (factory != null)
                    return factory;

            } catch (ConstructorInvocation.NoMatchingPublicConstructorException e) {
                // fallthrough and try Plan B
            }

            // Check for public nullary constructor
            final Constructor<AbstractAssertionPaletteNode> nullary =
                    ConstructorInvocation.findMatchingConstructor(loader, classname, AbstractAssertionPaletteNode.class, new Class[0]);

            return nullary == null ? null : new Functions.Unary<AbstractAssertionPaletteNode, AT>() {
                @Override
                public AbstractAssertionPaletteNode call(AT prototype) {
                    try {
                        return nullary.newInstance();
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e); // can't happen
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e); // can't happen
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

        } catch (ConstructorInvocation.WrongSuperclassException e) {
            logger.warning("Palette node class for assertion " + assname + " is not assignable to AbstractAssertionPaletteNode" +
            " (palette node class = " + classname + ")");
        } catch (ConstructorInvocation.AbstractClassException e) {
            logger.warning("Palette node class for assertion " + assname + " is abstract" +
            " (palette node class = " + classname + ")");
        } catch (ClassNotFoundException e) {
            // Probably was just a generated-by-default classname that doesn't actually exist
            logger.log(Level.FINER, "Unable to load palette node class for assertion class " + assname +
                                      ": " + ExceptionUtils.getMessage(e), e);
        } catch (ConstructorInvocation.NoMatchingPublicConstructorException e) {
            logger.warning("Palette node class for assertion " + assname +
                           " lacks public constructor-from-Assertion and nullary constructor" +
                            " (palette node class = " + classname + ")");
        }
        return null;
    }

    private static <AT extends Assertion> Functions.Unary<AssertionTreeNode, AT>
    findPolicyNodeFactory(ClassLoader loader, String classname, Class<AT> assclass) {
        final String assname = assclass.getName();
        try {
            return ConstructorInvocation.createFactoryOutOfUnaryConstructor(loader, classname, AssertionTreeNode.class, assclass);
        } catch (ConstructorInvocation.WrongSuperclassException e) {
            logger.warning("Policy node class for assertion " + assname + " is not assignable to AssertionTreeNode" +
            " (policy node class = " + classname + ")");
        } catch (ConstructorInvocation.AbstractClassException e) {
            logger.warning("Policy node class for assertion " + assname + " is abstract" +
            " (policy node class = " + classname + ")");
        } catch (ClassNotFoundException e) {
            // Probably was just a generated-by-default classname that doesn't actually exist
            logger.log(Level.FINER, "Unable to load policy node class for assertion class " + assname +
                                      ": " + ExceptionUtils.getMessage(e), e);
        } catch (ConstructorInvocation.NoMatchingPublicConstructorException e) {
            logger.warning("Policy node class for assertion " + assname +
                           " lacks public constructor-from-Assertion" +
                            " (policy node class = " + classname + ")");
        }
        return null;
    }

    /**
     * Attempt to find a public unary constructor-from-AssertionTreeNode in the specified class and
     * create a factory around it if we do.
     *
     * @param loader               ClassLoader to use when trying to load actionClassname, or null to use current classloader.
     * @param actionClassname     the name of the class to try to load and examine.  If null, this method immediately returns null.
     * @param assertionClassname  the name of the assertion class, for logging purposes.
     * @return a unary constructor-from-AssertionTreeNode for this Action class;
     *         or null if the class coudln't be found, wasn't an Action, was abstract, or lacked a public
     *         constructor-from-AssertionTreeNode.
     */
    private static Functions.Unary< Action, AssertionTreeNode >
    findPropertiesActionFactory(ClassLoader loader, String actionClassname, String assertionClassname)
    {
        try {
            return ConstructorInvocation.createFactoryOutOfUnaryConstructor(loader, actionClassname, Action.class, AssertionTreeNode.class);
        } catch (ConstructorInvocation.WrongSuperclassException e) {
            logger.warning("Properties action class for assertion " + assertionClassname + " is not assignable to Action" +
                            " (properties action class = " + actionClassname + ")");
        } catch (ConstructorInvocation.AbstractClassException e) {
            logger.warning("Properties action class for assertion " + assertionClassname + " is abstract" +
                            " (properties action class = " + actionClassname + ")");
        } catch (ClassNotFoundException e) {
            // Probably was just a generated-by-default classname that doesn't actually exist
            logger.log(Level.FINER, "Unable to load properties action class for assertion class " + assertionClassname +
                                      ": " + ExceptionUtils.getMessage(e), e);
        } catch (ConstructorInvocation.NoMatchingPublicConstructorException e) {
            logger.warning("Properties action class for assertion " + assertionClassname +
                           " lacks public constructor-from-AssertionTreeNode" +
                            " (properties action class = " + actionClassname + ")");
        }
        return null;
    }

    /**
     * Try to find a working nullary public constructor of AssertionPropertiesEditor at the specified classname
     * and create a PropertiesEditorFactory if we find one.
     *
     * @param loader               ClassLoader to use when trying to load apeClassname, or null to use current classloader.
     * @param apeClassname         the name of the class to try to load and examine.  If null, this method immediately returns null.
     * @param assertionClass       the name assertion class.  Must not be null.
     * @return a nullary Constructor< ? extends AssertionPropertiesEditor >, or null if nothing suitable could be located
     */
    private static <AT extends Assertion> Functions.Binary< AssertionPropertiesEditor<AT>, Frame, AT >
    findPropertiesEditorFactory(ClassLoader loader, String apeClassname, Class<AT> assertionClass)
    {
        if (apeClassname == null)
            return null;

        final String assertionClassname = assertionClass.getName();
        try {
            // First check for (Frame, Assertion)
            try {
                final Constructor<AssertionPropertiesEditor> ctorFrameAss =
                        ConstructorInvocation.findMatchingConstructor(loader,
                                                                      apeClassname,
                                                                      AssertionPropertiesEditor.class,
                                                                      new Class[] { Frame.class, assertionClass });
                if (ctorFrameAss != null) return new Functions.Binary<AssertionPropertiesEditor<AT>, Frame, AT>() {
                    @Override
                    public AssertionPropertiesEditor<AT> call(Frame parent, AT assertion) {
                        try {
                            //noinspection unchecked
                            return ctorFrameAss.newInstance(parent, assertion);
                        } catch (InstantiationException e) {
                            throw new RuntimeException(e); // can't happen
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e); // can't happen
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
            } catch (ConstructorInvocation.NoMatchingPublicConstructorException e) {
                // fallthrough and try next one
            }

            // Check for (Frame)  (we can then setData() the assertion)
            try {
                final Constructor<AssertionPropertiesEditor> ctorFrame =
                        ConstructorInvocation.findMatchingConstructor(loader,
                                                                      apeClassname,
                                                                      AssertionPropertiesEditor.class,
                                                                      new Class[] { Frame.class });
                if (ctorFrame != null) return new Functions.Binary<AssertionPropertiesEditor<AT>, Frame, AT>() {
                    @Override
                    public AssertionPropertiesEditor<AT> call(Frame parent, AT assertion) {
                        try {
                            //noinspection unchecked
                            AssertionPropertiesEditor<AT> ape = ctorFrame.newInstance(parent);
                            ape.setData(assertion);
                            return ape;
                        } catch (InstantiationException e) {
                            throw new RuntimeException(e); // can't happen
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e); // can't happen
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
            } catch (ConstructorInvocation.NoMatchingPublicConstructorException e) {
                // fallthrough and try next one
            }

            // Check for (Assertion)  (we'll ignore the parent)
            try {
                final Constructor<AssertionPropertiesEditor> ctorAss =
                        ConstructorInvocation.findMatchingConstructor(loader,
                                                                      apeClassname,
                                                                      AssertionPropertiesEditor.class,
                                                                      new Class[] { assertionClass });
                if (ctorAss != null) return new Functions.Binary<AssertionPropertiesEditor<AT>, Frame, AT>() {
                    @Override
                    public AssertionPropertiesEditor<AT> call(Frame parent, AT assertion) {
                        try {
                            //noinspection unchecked
                            return ctorAss.newInstance(assertion);
                        } catch (InstantiationException e) {
                            throw new RuntimeException(e); // can't happen
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e); // can't happen
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
            } catch (ConstructorInvocation.NoMatchingPublicConstructorException e) {
                // fallthrough and try next one
            }

            // Check for nullary (we'll ignore the parent and setData() the assertion)
            try {
                final Constructor<AssertionPropertiesEditor> ctorNullary =
                        ConstructorInvocation.findMatchingConstructor(loader,
                                                                      apeClassname,
                                                                      AssertionPropertiesEditor.class,
                                                                      new Class[] { });
                if (ctorNullary != null) return new Functions.Binary<AssertionPropertiesEditor<AT>, Frame, AT>() {
                    @Override
                    public AssertionPropertiesEditor<AT> call(Frame parent, AT assertion) {
                        try {
                            //noinspection unchecked
                            AssertionPropertiesEditor<AT> ape =  ctorNullary.newInstance();
                            ape.setData(assertion);
                            return ape;
                        } catch (InstantiationException e) {
                            throw new RuntimeException(e); // can't happen
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e); // can't happen
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
            } catch (ConstructorInvocation.NoMatchingPublicConstructorException e) {
                // fallthrough and give up
            }

            // Give up
            logger.warning("Properties editor class does not have a public constructor from Frame for assertion " +
                           assertionClassname + " (properties editor class = " + apeClassname + ")");
            return null;

        } catch (ConstructorInvocation.AbstractClassException e) {
            logger.warning("Properties editor class is abstract for assertion " + assertionClassname +
                           " (properties editor class = " + apeClassname + ")");
        } catch (ClassNotFoundException e) {
            // Probably was just a generated-by-default classname that doesn't actually exist
            if (logger.isLoggable(Level.FINER))
                logger.log(Level.FINER, "Properties editor class not found for assertion " + assertionClassname + ": " + ExceptionUtils.getMessage(e), e);
        } catch (ConstructorInvocation.WrongSuperclassException e) {
            logger.warning("Properties editor class is not assignable to AssertionPropertiesEditor for assertion " + assertionClassname +
                           " (properties editor class = " + apeClassname + ")");
        }
        return null;
    }

}
