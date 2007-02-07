package com.l7tech.console.policy;

import com.l7tech.common.util.ConstructorInvocation;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.Functions;
import com.l7tech.console.SsmApplication;
import com.l7tech.console.action.DefaultAssertionPropertiesAction;
import com.l7tech.console.panels.AssertionPropertiesEditor;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.DefaultAssertionPolicyNode;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.*;
import org.springframework.beans.factory.InitializingBean;

import javax.swing.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The AssertionRegistry for the SecureSpan Manager.  Extends the basic registry by adding functionality
 * for finding default GUI-related classes to the DefaultAssertionMetadata default metadata finders.
 */
public class ConsoleAssertionRegistry extends AssertionRegistry implements InitializingBean {
    protected static final Logger logger = Logger.getLogger(ConsoleAssertionRegistry.class.getName());

    /** @noinspection UnusedDeclaration,FieldCanBeLocal */ // TODO remove this if I turn out not to need it
    private final SsmApplication ssmApplication;

    public ConsoleAssertionRegistry(SsmApplication ssmApplication) {
        this.ssmApplication = ssmApplication;
    }

    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        //
        // Add metadata default getters that are specified to the SSM environment
        //
        DefaultAssertionMetadata.putDefaultGetter(AssertionMetadata.POLICY_NODE_FACTORY, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                String classname = (String)meta.get(AssertionMetadata.POLICY_NODE_CLASSNAME);
                final Class assclass = meta.getAssertionClass();
                String assname = assclass.getName();
                Functions.Unary< AssertionTreeNode, Assertion > factory = findPolicyNodeFactory(assclass.getClassLoader(), classname, assname);
                if (factory != null)
                    return DefaultAssertionMetadata.cache(meta, key, factory);

                // Try to use the default
                factory = new Functions.Unary< AssertionTreeNode, Assertion >() {
                    public AssertionTreeNode call(Assertion assertion) {
                        return new DefaultAssertionPolicyNode<Assertion>(assertion);
                    }
                };
                return DefaultAssertionMetadata.cache(meta, key, factory);
            }
        });

        DefaultAssertionMetadata.putDefaultGetter(AssertionMetadata.PROPERTIES_ACTION_FACTORY, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                String classname = (String)meta.get(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME);
                final Class assclass = meta.getAssertionClass();
                String assname = assclass.getName();
                Functions.Unary<Action, AssertionTreeNode> factory = findPropertiesActionFactory(assclass.getClassLoader(), classname, assname);
                if (factory != null)
                    return DefaultAssertionMetadata.cache(meta, key, factory);
                // Try using the default action
                //noinspection unchecked
                final Functions.Nullary<AssertionPropertiesEditor<Assertion>> apeFactory =
                        (Functions.Nullary<AssertionPropertiesEditor<Assertion>>)
                                meta.get(AssertionMetadata.PROPERTIES_EDITOR_FACTORY);
                if (apeFactory == null) {
                    // No APE for this assertion = no "Properties..." action
                    return DefaultAssertionMetadata.cache(meta, key, null);
                }

                factory = new Functions.Unary<Action, AssertionTreeNode>() {
                    public Action call(AssertionTreeNode assertionTreeNode) {
                        //noinspection unchecked
                        return new DefaultAssertionPropertiesAction<Assertion, AssertionTreeNode<Assertion>>(assertionTreeNode, apeFactory);
                    }
                };
                return DefaultAssertionMetadata.cache(meta, key, factory);
            }
        });

        DefaultAssertionMetadata.putDefaultGetter(AssertionMetadata.PROPERTIES_EDITOR_FACTORY, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                final Class assclass = meta.getAssertionClass();
                final String assname = assclass.getName();
                String apeClassname = (String)meta.get(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME);
                Functions.Nullary<AssertionPropertiesEditor<Assertion>> factory =
                        findPropertiesEditorFactory(assclass.getClassLoader(), apeClassname, assname);
                if (factory != null)
                    return DefaultAssertionMetadata.cache(meta, key, factory);

                // Since we have no fallback otherwise, try two likely possibilities before giving up 
                apeClassname = "com.l7tech.console.panels." + meta.get(AssertionMetadata.BASE_NAME) + "PropertiesDialog";
                factory = findPropertiesEditorFactory(assclass.getClassLoader(), apeClassname, assname);
                if (factory != null)
                    return DefaultAssertionMetadata.cache(meta, key, factory);

                apeClassname = "com.l7tech.console.panels." + meta.get(AssertionMetadata.BASE_NAME) + "AssertionPropertiesDialog";
                factory = findPropertiesEditorFactory(assclass.getClassLoader(), apeClassname, assname);
                if (factory != null)
                    return DefaultAssertionMetadata.cache(meta, key, factory);

                // No default available -- just return null, disabling "Properties..." for this assertion.
                // TODO maybe someday we can build a simple bean editor GUI for the assertion
                return DefaultAssertionMetadata.cache(meta, key, null);
            }
        });
    }

    private static class WrongSuperclassException extends Exception {}
    private static class AbstractClassException extends Exception {}
    private static class NoMatchingPublicConstructorException extends Exception {}

    private static <IN, OUT> Functions.Unary<OUT, IN> createFactoryOutOfUnaryConstructor(ClassLoader loader,
                                                                                         String prospectClassname,
                                                                                         Class<OUT> requiredSuperclass,
                                                                                         Class<IN> inClass)
            throws WrongSuperclassException, AbstractClassException, ClassNotFoundException, NoMatchingPublicConstructorException
    {
        if (prospectClassname == null)
            return null;
        //noinspection unchecked
        Class<OUT> prospectClass = (Class<OUT>)Class.forName(prospectClassname, true, loader);
        if (!requiredSuperclass.isAssignableFrom(prospectClass))
            throw new WrongSuperclassException();
        if (Modifier.isAbstract(prospectClass.getModifiers()))
            throw new AbstractClassException();
        //noinspection unchecked
        final Constructor<OUT> unaryCtor = ConstructorInvocation.findMatchingConstructor(prospectClass, new Class[] {inClass});
        if (unaryCtor == null || !Modifier.isPublic(unaryCtor.getModifiers()))
            throw new NoMatchingPublicConstructorException();
        return new Functions.Unary<OUT, IN>() {
            public OUT call(IN in) {
                try {
                    return unaryCtor.newInstance(in);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e); // can't happen, we checked this in advance
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e); // can't happen, we checked this in advance
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e); // Pass it along
                }
            }
        };
    }

    private static Functions.Unary< AssertionTreeNode, Assertion >
    findPolicyNodeFactory(ClassLoader loader, String classname, String assname) {
        try {
            return createFactoryOutOfUnaryConstructor(loader, classname, AssertionTreeNode.class, Assertion.class);
        } catch (WrongSuperclassException e) {
            logger.warning("Policy node class for assertion " + assname + " is not assignable to AssertionTreeNode" +
            " (policy node class = " + classname + ")");
        } catch (AbstractClassException e) {
            logger.warning("Policy node class for assertion " + assname + " is abstract" +
            " (policy node class = " + classname + ")");
        } catch (ClassNotFoundException e) {
            logger.log(Level.FINER, "Unable to load policy node class for assertion class " + assname +
                                      ": " + ExceptionUtils.getMessage(e), e);
        } catch (NoMatchingPublicConstructorException e) {
            logger.warning("Policy node class for assertion " + assname +
                           " lacks public constructor-from-Assertion" +
                            " (properties action class = " + classname + ")");
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
            return createFactoryOutOfUnaryConstructor(loader, actionClassname, Action.class, AssertionTreeNode.class);
        } catch (WrongSuperclassException e) {
            logger.warning("Properties action class for assertion " + assertionClassname + " is not assignable to Action" +
                            " (properties action class = " + actionClassname + ")");
        } catch (AbstractClassException e) {
            logger.warning("Properties action class for assertion " + assertionClassname + " is abstract" +
                            " (properties action class = " + actionClassname + ")");
        } catch (ClassNotFoundException e) {
            logger.log(Level.FINER, "Unable to load properties action class for assertion class " + assertionClassname +
                                      ": " + ExceptionUtils.getMessage(e), e);
        } catch (NoMatchingPublicConstructorException e) {
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
     * @param assertionClassname   the name of the assertion class, for logging purposes.
     * @return a nullary Constructor< ? extends AssertionPropertiesEditor >, or null if nothing suitable could be located
     */
    private static <AT extends Assertion> Functions.Nullary< AssertionPropertiesEditor< AT > >
    findPropertiesEditorFactory(ClassLoader loader, String apeClassname, String assertionClassname)
    {
        if (apeClassname == null)
            return null;
        Class<? extends AssertionPropertiesEditor<AT>> apeClass = null;

        try {
            //noinspection unchecked
            apeClass = (Class<? extends AssertionPropertiesEditor<AT>>)Class.forName(apeClassname, true, loader);
            final Constructor<? extends AssertionPropertiesEditor<AT>> nullary = apeClass.getConstructor();
            if (Modifier.isAbstract(apeClass.getModifiers())) {
                logger.warning("Properties editor class is abstract for assertion " + assertionClassname +
                               " (properties editor class = " + apeClass.getName() + ")");
                return null;
            }
            if (!Modifier.isPublic(nullary.getModifiers())) {
                logger.warning("Properties editor class nullary constructor is not public for assertion " +
                               assertionClassname + " (properties editor class = " + apeClass.getName() + ")");
                return null;
            }
            return new Functions.Nullary< AssertionPropertiesEditor< AT > >() {
                public AssertionPropertiesEditor<AT> call() {
                    try {
                        return nullary.newInstance();
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e); // can't happen, we checked this
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e); // can't happen, we checked this
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e); // broken APE
                    }
                }
            };
        } catch (ClassNotFoundException e) {
            // Probably was just a generated-by-default classname that doesn't actually exist
            if (logger.isLoggable(Level.FINER))
                logger.log(Level.FINER, "Properties editor class not found for assertion " + assertionClassname + ": " + ExceptionUtils.getMessage(e), e);
            return null;
        } catch (NoSuchMethodException e) {
            logger.warning("Properties editor class lacks nullary constructor for assertion " + assertionClassname +
                           " (properties editor class = " + apeClass.getName() + ")");
            return null;
        }
    }
}
