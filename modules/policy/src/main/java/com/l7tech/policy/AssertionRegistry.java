package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.MetadataFinder;
import com.l7tech.policy.wsp.*;
import com.l7tech.util.ClassUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.policy.assertion.AssertionMetadata.ASSERTION_FACTORY;
import static com.l7tech.policy.assertion.AssertionMetadata.CLIENT_ASSERTION_CLASSNAME;
import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_NODE_NAME_FACTORY;
import static com.l7tech.policy.assertion.AssertionMetadata.SERVER_ASSERTION_CLASSNAME;
import static com.l7tech.policy.assertion.AssertionMetadata.USED_BY_CLIENT;
import static com.l7tech.policy.assertion.AssertionMetadata.WSP_TYPE_MAPPING_CLASSNAME;
import static com.l7tech.policy.assertion.AssertionMetadata.WSP_TYPE_MAPPING_INSTANCE;
import static com.l7tech.policy.assertion.DefaultAssertionMetadata.*;

/**
 * An AssertionRegistry keeps track of a set of Assertion classes, each represented by a single prototype
 * instance.
 * <p/>
 * The Bridge, Gateway, and Manager each have their own implementation, of which there is usually one instance
 * per process.
 */
public class AssertionRegistry implements AssertionFinder, TypeMappingFinder, ApplicationContextAware, InitializingBean, DisposableBean {
    protected static final Logger logger = Logger.getLogger(AssertionRegistry.class.getName());

    // Install the default getters that can't be included in DefaultAssertionMetadata itself due to the compile13 closure
    private static final AtomicBoolean enhancedMetadataDefaultsInstalled = new AtomicBoolean(false);

    static {
        installEnhancedMetadataDefaults();
    }

    private final Map<String, Assertion> prototypes = new ConcurrentHashMap<>();
    private final Map<String, Assertion> byExternalName = new ConcurrentHashMap<>();
    private ApplicationContext applicationContext;
    private boolean shuttingDown = false;

    public AssertionRegistry() {
        installEnhancedMetadataDefaults();
    }

    public final void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;

        // Pre-populate with hardcoded assertions
        for (Assertion assertion : AllAssertions.SERIALIZABLE_EVERYTHING)
            registerAssertion(assertion.getClass());

        onApplicationContextSet();
    }

    protected void onApplicationContextSet() {
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Register an assertion subclass.  This will invoke newInstance on the specified
     * class to create a prototype instance.
     * <p/>
     * If the specified assertion class has already been registered with this registry, this will unregister
     * its old prototype instance (and laster publish an AssertionUnregistrationEvent).
     * <p/>
     * Once the new prototype instance is registered this method will publish an AssertionRegistrationEvent.
     *
     * @param assertionClass the Assertion subclass to register.  Must not be null.
     * @return the prototype instance that is now registered for this class.  Never null.
     */
    public synchronized Assertion registerAssertion(Class<? extends Assertion> assertionClass) {
        try {
            final Assertion prototype = assertionClass.newInstance();
            Assertion old = prototypes.put(assertionClass.getName(), prototype);
            Object externalName = prototype.meta().get(AssertionMetadata.WSP_EXTERNAL_NAME);
            if (externalName instanceof String)
                byExternalName.put((String)externalName, prototype);
            if (old != null) publishEvent(new AssertionUnregistrationEvent(this, old));
            publishEvent(new AssertionRegistrationEvent(this, prototype));
            return prototype;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e); // can't happen; assertion must have public nullary c'tor
        }
    }

    protected void publishEvent(ApplicationEvent event) {
        ApplicationContext context = getApplicationContext();
        if (context != null) context.publishEvent(event);
    }

    /**
     * Unregister an assertion prototype.
     * <p/>
     * This method is protected by default.  Subclasses of AssertionRegistry that wish to provide this functionality
     * should override this method with a public version that does any needed activity before and after invoking
     * this superclass implementation (notifying listeners, regenerating caches, etc).
     * <p/>
     * This method just removes any assertion prototype with a matching classname from the registry and then
     * fires an {@link AssertionUnregistrationEvent}.
     *
     * @param prototype the prototype instance to unregister.
     * @return true if an assertion was unregistered (and an event fired); false if no action was taken.
     */
    protected synchronized boolean unregisterAssertion(Assertion prototype) {
        Class assclass = prototype.getClass();
        String assname = assclass.getName();

        final Iterator<Assertion> eit = byExternalName.values().iterator();
        while (eit.hasNext()) {
            Assertion ass = eit.next();
            if (ass.getClass().getName().equals(assname))
                eit.remove();
        }

        boolean found = false;
        final Iterator<Assertion> pit = prototypes.values().iterator();
        while (pit.hasNext()) {
            Assertion ass = pit.next();
            if (ass.getClass().getName().equals(assname)) {
                found = true;
                pit.remove();
            }
        }

        Assertion.clearCachedMetadata(assname);

        return found;
    }

    protected Set<String> getAssertionClassnames() {
        Set<String> ret =  new HashSet<>();
        for (Assertion assertion : prototypes.values())
            ret.add(assertion.getClass().getName());
        return ret;
    }

    /**
     * Check if the specified assertion concrete classname is registered with this registry.
     *
     * @param classname  the classname to check
     * @return true if this class name is registered with this registry
     */
    public boolean isAssertionRegistered(String classname) {
        return getAssertionClassnames().contains(classname);
    }

    public Set<Assertion> getAssertions() {
        return new HashSet<>(prototypes.values());
    }

    public Assertion findByExternalName(String externalName) {
        return byExternalName.get(externalName);
    }

    public Assertion findByClassName(String className) {
        return prototypes.get(className);
    }

    /**
     * Find an Assertion prototype instance corresponding to the specified external name, as from a WspWriter
     * policy.
     *
     * @param externalName the external name to look up.  Must not be null or empty.
     * @return the TypeMapping for this external name, or null if none was found.
     */
    public TypeMapping getTypeMapping(String externalName) {
        Assertion ass = findByExternalName(externalName);
        if (ass != null)
            return (TypeMapping)ass.meta().get(WSP_TYPE_MAPPING_INSTANCE);

        // Check for globally-visible compatibility mappings
        for (Assertion assertion : getAssertions()) {
            //noinspection unchecked
            Map<String, TypeMapping> compatMappings = (Map<String, TypeMapping>)assertion.meta().get(AssertionMetadata.WSP_COMPATIBILITY_MAPPINGS);
            if (compatMappings != null) {
                final TypeMapping mapping = compatMappings.get(externalName);
                if (mapping != null)
                    return mapping;
            }
        }

        return null;
    }

    public TypeMapping getTypeMapping(Type unrecognizedType, String version) {
        Class clazz = TypeMappingUtils.getClassForType(unrecognizedType);
        if (Assertion.class.isAssignableFrom(clazz)) {
            try {
                Assertion instance = (Assertion)clazz.newInstance();
                return (TypeMapping)instance.meta().get(WSP_TYPE_MAPPING_INSTANCE);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e); // broken bean
            }
        }

        return null;
    }

    /**
     * Install the default assertion metadata finders that couldn't be part of DefaultAssertionMetadata because
     * the latter is part of the Layer 7 API.
     * <p/>
     * This is called automatically whenever an AssertionRegistry is created.
     */
    public static void installEnhancedMetadataDefaults() {
        if (enhancedMetadataDefaultsInstalled.get())
            return;

        putDefaultGetter(SERVER_ASSERTION_CLASSNAME, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, makeDefaultSpecificClass(meta, "server", "Server"));
            }
        });

        putDefaultGetter(CLIENT_ASSERTION_CLASSNAME, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, makeDefaultSpecificClass(meta, "proxy", "Client"));
            }
        });

        putDefaultGetter(ASSERTION_FACTORY, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                final Class metadataClass = meta.getAssertionClass();
                return new Functions.Unary< Assertion, Assertion > () {
                    public Assertion call(Assertion assertion) {
                        Class clazz = assertion != null ? assertion.getClass() : metadataClass;
                        try {
                            return (Assertion)clazz.newInstance();
                        } catch (InstantiationException | IllegalAccessException e) {
                            throw new RuntimeException(e); // shouldn't happen -- assertion should have public nullary ctor
                        }
                    }
                };
            }
        });

        putDefaultGetter(POLICY_NODE_NAME_FACTORY, new MetadataFinder() {
            public Object get(final AssertionMetadata meta, String key) {
                return cache(meta, key, null);
            }
        });

        putDefaultGetter(WSP_TYPE_MAPPING_INSTANCE, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                //noinspection unchecked
                Class<? extends Assertion> assClass = meta.getAssertionClass();
                String typeMappingClassname = (String)meta.get(WSP_TYPE_MAPPING_CLASSNAME);
                if (typeMappingClassname != null) {
                    try {
                        Class typeMappingClass = assClass.getClassLoader().loadClass(typeMappingClassname);
                        if (typeMappingClass != null)
                            return typeMappingClass.newInstance();
                    } catch (ClassNotFoundException e) {
                        logger.log(Level.FINER, "Assertion " + assClass.getName() + " metadata declares a custom TypeMapping, but the class was not found: " + ExceptionUtils.getMessage(e), e);
                        /* FALLTHROUGH and try default assertion mapping */
                    } catch (IllegalAccessException | InstantiationException e) {
                        logger.log(Level.WARNING, "Assertion " + assClass.getName() + " metadata declares a custom TypeMapping, but it could not be instantiated with the nullary constructor: " + ExceptionUtils.getMessage(e), e);
                        /* FALLTHROUGH and try default assertion mapping */
                    }
                }
                String externalName = (String)meta.get(AssertionMetadata.WSP_EXTERNAL_NAME);
                return cache(meta, key, new AssertionMapping(assClass, externalName));
            }
        });

        putDefaultGetter(USED_BY_CLIENT, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, Boolean.FALSE);
            }
        });

        putDefaultGetter(AssertionMetadata.FEATURE_SET_NAME, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                Class assertionClass = meta.getAssertionClass();
                if (isCoreAssertion(assertionClass))
                    return cache(meta, key, "(fromClass)");

                // Unknown assertion; treat as modular
                return "set:modularAssertions";
            }
        });

        enhancedMetadataDefaultsInstalled.set(true);
    }

    /**
     * Check if the specified assertion class is part of the core product (that is, not from a modular assertion).
     * Assertions are considered part of the core if and only if they are listed in {@link AllAssertions#SERIALIZABLE_EVERYTHING}
     * or {@link AllAssertions#GATEWAY_EVERYTHING}.
     *
     * @param assertionClass  the assertion class to check. Required.
     * @return true iff. the specified assertion class is recognized as a core assertion.
     */
    public static boolean isCoreAssertion(Class assertionClass) {
        for (Assertion ass : AllAssertions.SERIALIZABLE_EVERYTHING) {
            if (ass.getClass() == assertionClass)
                return true;
        }
        for (Assertion ass : AllAssertions.GATEWAY_EVERYTHING) {
            if (ass.getClass() == assertionClass)
                return true;
        }
        return false;
    }

    /**
     * Make the default specific class for the specified assertion metadata, per the descriptions of
     * {@link AssertionMetadata#SERVER_ASSERTION_CLASSNAME} and {@link AssertionMetadata#CLIENT_ASSERTION_CLASSNAME}. 
     *
     * @param meta  the AssertionMetadata.  Required.
     * @param packageInsert the package to insert after the base packe, ie "proxy" or "server".  Required.
     * @param classPrefix  the prepend to the base classname, ie "Client" or "Server", with initial capital.  Required.
     * @return the default specific class name for this metadata and type.
     */
    private static String makeDefaultSpecificClass(AssertionMetadata meta, String packageInsert, String classPrefix) {
        Class assclass = meta.getAssertionClass();
        String assname = assclass.getName();                  // assertion full classname, ie "com.yoyodyne.assertion.a.b.FooAssertion"
        String className = ClassUtils.getClassName(assname);  // assertion classname without package, ie "FooAssertion"
        String basepack = basepack(meta);
        String rest = ClassUtils.stripPrefix(assname, basepack + "."); // "com.yoyodyne.assertion.a.b.FooAssertion" => "assertion.a.b.FooAssertion"

        if (!isCoreAssertion(assclass))
            return basepack + "." + classPrefix.toLowerCase() + "." + classPrefix + className;

        rest = ClassUtils.stripPrefix(rest, "policy.");
        rest = ClassUtils.stripPrefix(rest, "assertion.");                 // "assertion.a.b.FooAssertion" => "a.b.FooAssertion"
        rest = ClassUtils.stripSuffix(rest, className);// "a.b.FooAssertion" => "a.b"
        rest = ClassUtils.stripSuffix(rest, ".");
        if (rest.length() > 0) rest = rest + ".";
        return basepack + "." + packageInsert + ".policy.assertion." + rest + classPrefix + className;
    }

    public void afterPropertiesSet() throws Exception {
        // TODO remove this hack, along with WspReader.getDefault(), as soon as everything gets WspReader through Spring
        WspConstants.setTypeMappingFinder(this);
    }

    /** @return true if the destroy() method has ever been called on this instance */
    protected boolean isShuttingDown() {
        return shuttingDown;
    }

    public synchronized void destroy() throws Exception {
        shuttingDown = true;
    }
}
