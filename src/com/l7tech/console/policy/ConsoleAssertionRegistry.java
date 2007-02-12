package com.l7tech.console.policy;

import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.common.util.ConstructorInvocation;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.Functions;
import com.l7tech.console.action.DefaultAssertionPropertiesAction;
import com.l7tech.console.panels.AssertionPropertiesEditor;
import com.l7tech.console.tree.AbstractAssertionPaletteNode;
import com.l7tech.console.tree.DefaultAssertionPaletteNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.DefaultAssertionPolicyNode;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.MetadataFinder;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URL;

/**
 * The AssertionRegistry for the SecureSpan Manager.  Extends the basic registry by adding functionality
 * for finding default GUI-related classes to the DefaultAssertionMetadata default metadata finders.
 */
public class ConsoleAssertionRegistry extends AssertionRegistry {
    protected static final Logger logger = Logger.getLogger(ConsoleAssertionRegistry.class.getName());

    /** Prototype instances of assertions loaded from the server. */
    private Set<Assertion> modulePrototypes = new HashSet<Assertion>();

    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        //
        // Add metadata default getters that are specific to the SSM environment
        //

        DefaultAssertionMetadata.putDefaultGetter(AssertionMetadata.PALETTE_NODE_FACTORY, new PaletteNodeFactoryMetadataFinder());

        DefaultAssertionMetadata.putDefaultGetter(AssertionMetadata.POLICY_NODE_FACTORY, new PolicyNodeFactoryMetadataFinder());

        DefaultAssertionMetadata.putDefaultGetter(AssertionMetadata.PROPERTIES_ACTION_FACTORY, new PropertiesActionMetadataFinder());

        DefaultAssertionMetadata.putDefaultGetter(AssertionMetadata.PROPERTIES_EDITOR_FACTORY, new PropertiesEditorFactoryMetadataFinder());
    }

    public void updateModularAssertions() throws RemoteException {
        for (Assertion prototype : modulePrototypes)
            unregisterAssertion(prototype);

        ClusterStatusAdmin cluster = Registry.getDefault().getClusterStatusAdmin();
        Collection<ClusterStatusAdmin.ModuleInfo> modules = cluster.getAssertionModuleInfo();
        for (ClusterStatusAdmin.ModuleInfo module : modules)
            registerAssertionsFromModule(cluster, module);
    }

    private void registerAssertionsFromModule(final ClusterStatusAdmin cluster, final ClusterStatusAdmin.ModuleInfo module) {
        final Collection<String> assertionClassnames = module.assertionClasses;

        final String moduleFilename = module.moduleFilename;

        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                ClassLoader assloader = makeModuleClassLoader(cluster, moduleFilename);
                for (String assertionClassname : assertionClassnames) {
                    try {
                        Class assclass = assloader.loadClass(assertionClassname);
                        if (!Assertion.class.isAssignableFrom(assclass))
                            throw new ClassCastException(assclass.getName());
                        Assertion prototype = (Assertion)assclass.newInstance();

                        logger.info("Registering remote assertion " + prototype.getClass().getName());
                        modulePrototypes.add(prototype);
                        registerAssertion(prototype.getClass());

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

    // Make a ClassLoader that will load classes from the specified module, using the specified cluster status admin as
    // the source of remote class bytes
    private ClassLoader makeModuleClassLoader(final ClusterStatusAdmin cluster, final String moduleFilename) {
        return new ClassLoader(getClass().getClassLoader()) {

            protected Class<?> findClass(final String name) throws ClassNotFoundException {
                String resourcepath = name.replace('.', '/').concat(".class");
                try {
                    final byte[] bytes = cluster.getAssertionModuleResource(moduleFilename, resourcepath);
                    if (bytes == null)
                        throw new ClassNotFoundException("Class not found in module " + moduleFilename + ": " + name);

                    return AccessController.doPrivileged(new PrivilegedAction<Class>() {
                        public Class run() {
                            return defineClass(name, bytes, 0, bytes.length);
                        }
                    });

                } catch (ClusterStatusAdmin.ModuleNotFoundException e) {
                    throw new ClassNotFoundException("Unable to load class from module " + moduleFilename + ": " + ExceptionUtils.getMessage(e), e);
                } catch (RemoteException e) {
                    throw new ClassNotFoundException("Unable to load class from module " + moduleFilename + ": " + ExceptionUtils.getMessage(e), e);
                }
            }

            public InputStream getResourceAsStream(String name) {
                try {
                    byte[] got = cluster.getAssertionModuleResource(moduleFilename, name);
                    if (got == null)
                        return null;

                    return new ByteArrayInputStream(got);
                } catch (ClusterStatusAdmin.ModuleNotFoundException e) {
                    logger.log(Level.WARNING, "Unable to find resource from module: " + ExceptionUtils.getMessage(e), e);
                    return null;
                } catch (RemoteException e) {
                    logger.log(Level.WARNING, "Unable to find resource from module: " + ExceptionUtils.getMessage(e), e);
                    return null;
                }
            }

            protected URL findResource(String name) {
                logger.log(Level.WARNING, "*** findResource called on module class loader: resource=" + name);
                return super.findResource(name);
            }

            protected Enumeration<URL> findResources(String name) throws IOException {
                logger.log(Level.WARNING, "*** findResources called on module class loader: resource=" + name);
                return super.findResources(name);
            }
        };
    }

    private static class PaletteNodeFactoryMetadataFinder<AT extends Assertion> implements MetadataFinder {
        public Object get(AssertionMetadata meta, String key) {
            String classname = (String)meta.get(AssertionMetadata.PALETTE_NODE_CLASSNAME);
            //noinspection unchecked
            final Class<AT> assclass = meta.getAssertionClass();
            Functions.Unary<AbstractAssertionPaletteNode, AT> factory =
                    findPaletteNodeFactory(assclass.getClassLoader(), classname, assclass);
            if (factory != null)
                return DefaultAssertionMetadata.cache(meta, key, factory);

            // Try to use the default
            factory = new Functions.Unary<AbstractAssertionPaletteNode, AT>() {
                public AbstractAssertionPaletteNode call(AT assertion) {
                    return new DefaultAssertionPaletteNode<AT>(assertion);
                }
            };
            return DefaultAssertionMetadata.cache(meta, key, factory);
        }
    }

    private static class PolicyNodeFactoryMetadataFinder<AT extends Assertion> implements MetadataFinder {
        public Object get(AssertionMetadata meta, String key) {
            String classname = (String)meta.get(AssertionMetadata.POLICY_NODE_CLASSNAME);
            //noinspection unchecked
            final Class<AT> assclass = meta.getAssertionClass();
            Functions.Unary<AssertionTreeNode, AT> factory =
                    findPolicyNodeFactory(assclass.getClassLoader(), classname, assclass);
            if (factory != null)
                return DefaultAssertionMetadata.cache(meta, key, factory);

            // Try to use the default
            factory = new Functions.Unary< AssertionTreeNode, AT >() {
                public AssertionTreeNode call(AT assertion) {
                    return new DefaultAssertionPolicyNode<AT>(assertion);
                }
            };
            return DefaultAssertionMetadata.cache(meta, key, factory);
        }
    }

    private static class PropertiesActionMetadataFinder<AT extends Assertion> implements MetadataFinder {
        public Object get(AssertionMetadata meta, String key) {
            String classname = (String)meta.get(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME);
            //noinspection unchecked
            final Class<AT> assclass = meta.getAssertionClass();
            String assname = assclass.getName();
            Functions.Unary<Action, AssertionTreeNode> factory = findPropertiesActionFactory(assclass.getClassLoader(), classname, assname);
            if (factory != null)
                return DefaultAssertionMetadata.cache(meta, key, factory);
            // Try using the default action
            //noinspection unchecked
            final Functions.Binary<AssertionPropertiesEditor< AT >, Frame, AT > apeFactory =
                    (Functions.Binary<AssertionPropertiesEditor< AT >, Frame, AT>)
                            meta.get(AssertionMetadata.PROPERTIES_EDITOR_FACTORY);
            if (apeFactory == null) {
                // No APE for this assertion = no "Properties..." action
                return DefaultAssertionMetadata.cache(meta, key, null);
            }

            factory = new Functions.Unary<Action, AssertionTreeNode>() {
                public Action call(AssertionTreeNode assertionTreeNode) {
                    //noinspection unchecked
                    return new DefaultAssertionPropertiesAction<AT, AssertionTreeNode<AT>>(assertionTreeNode, apeFactory);
                }
            };
            return DefaultAssertionMetadata.cache(meta, key, factory);
        }
    }

    private static class PropertiesEditorFactoryMetadataFinder<AT extends Assertion> implements MetadataFinder {
        public Object get(AssertionMetadata meta, String key) {
            //noinspection unchecked
            final Class<AT> assclass = meta.getAssertionClass();
            String apeClassname = (String)meta.get(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME);
            Functions.Binary<AssertionPropertiesEditor<AT>, Frame, AT> factory =
                    ConsoleAssertionRegistry.findPropertiesEditorFactory(assclass.getClassLoader(), apeClassname, assclass);
            if (factory != null)
                return DefaultAssertionMetadata.cache(meta, key, factory);

            // Since we have no fallback otherwise, try two likely possibilities before giving up
            apeClassname = "com.l7tech.console.panels." + meta.get(AssertionMetadata.BASE_NAME) + "PropertiesDialog";
            factory = findPropertiesEditorFactory(assclass.getClassLoader(), apeClassname, assclass);
            if (factory != null)
                return DefaultAssertionMetadata.cache(meta, key, factory);

            apeClassname = "com.l7tech.console.panels." + meta.get(AssertionMetadata.BASE_NAME) + "AssertionPropertiesDialog";
            factory = findPropertiesEditorFactory(assclass.getClassLoader(), apeClassname, assclass);
            if (factory != null)
                return DefaultAssertionMetadata.cache(meta, key, factory);

            // No default available -- just return null, disabling "Properties..." for this assertion.
            // TODO maybe someday we can build a simple bean editor GUI for the assertion
            return DefaultAssertionMetadata.cache(meta, key, null);
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
