package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.MetadataFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.TypeMappingFinder;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.AssertionMapping;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.Functions;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * An AssertionRegistry keeps track of a set of Assertion classes, each represented by a single prototype
 * instance.
 * <p/>
 * The Bridge, Gateway, and Manager each have their own implementation, of which there is usually one instance
 * per process.
 */
public class AssertionRegistry implements AssertionFinder, TypeMappingFinder, ApplicationContextAware, InitializingBean {
    protected static final Logger logger = Logger.getLogger(AssertionRegistry.class.getName());

    // Install the default getters that can't be included in DefaultAssertionMetadata itself due to the compile13 closure
    private static final AtomicBoolean enhancedMetadataDefaultsInstalled = new AtomicBoolean(false);

    static {
        installEnhancedMetadataDefaults();
    }

    private final Map<String, Assertion> prototypes = new ConcurrentHashMap<String, Assertion>();
    private final Map<String, Assertion> byExternalName = new ConcurrentHashMap<String, Assertion>();
    private ApplicationContext applicationContext;

    public AssertionRegistry() {
        installEnhancedMetadataDefaults();
    }

    public final void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;

        // Pre-populate with hardcoded assertions
        for (Assertion assertion : AllAssertions.SERIALIZABLE_EVERYTHING)
            registerAssertion(assertion.getClass());
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Register an assertion subclass.  This will invoke newInstance on the specified
     * class to create a prototype instance.
     * <p/>
     * Nothing happens if the specified assertion class has already been registered with this registry.
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
        } catch (InstantiationException e) {
            throw new RuntimeException(e); // can't happen; assertion must have public nullary c'tor
        } catch (IllegalAccessException e) {
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
        byExternalName.values().remove(prototype);
        if (prototypes.values().remove(prototype)) {
            publishEvent(new AssertionUnregistrationEvent(this, prototype));
            return true;
        }
        return false;
    }

    protected Set<String> getAssertionClassnames() {
        Set<String> ret =  new HashSet<String>();
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
        return new HashSet<Assertion>(prototypes.values());
    }

    public Assertion findByExternalName(String externalName) {
        return byExternalName.get(externalName);
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
        if (ass == null)
            return null;
        return (TypeMapping)ass.meta().get(AssertionMetadata.WSP_TYPE_MAPPING_INSTANCE);
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

        DefaultAssertionMetadata.putDefaultGetter(AssertionMetadata.ASSERTION_FACTORY, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                final Class metadataClass = meta.getAssertionClass();
                return new Functions.Unary< Assertion, Assertion > () {
                    public Assertion call(Assertion assertion) {
                        Class clazz = assertion != null ? assertion.getClass() : metadataClass;
                        try {
                            return (Assertion)clazz.newInstance();
                        } catch (InstantiationException e) {
                            throw new RuntimeException(e); // shouldn't happen -- assertion should have public nullary ctor
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e); // shouldn't happen -- assertion should have public nullary ctor
                        }
                    }
                };
            }
        });

        DefaultAssertionMetadata.putDefaultGetter(AssertionMetadata.POLICY_NODE_NAME, new MetadataFinder() {
            public Object get(final AssertionMetadata meta, String key) {
                return DefaultAssertionMetadata.cache(meta, key, new Functions.Unary< String, Assertion >() {
                    public String call(Assertion assertion) {
                        return (String)meta.get(AssertionMetadata.SHORT_NAME);
                    }
                });
            }
        });

        DefaultAssertionMetadata.putDefaultGetter(AssertionMetadata.WSP_TYPE_MAPPING_INSTANCE, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                Class assClass = meta.getAssertionClass();
                String typeMappingClassname = (String)meta.get(AssertionMetadata.WSP_TYPE_MAPPING_CLASSNAME);
                if (typeMappingClassname != null) {
                    try {
                        Class typeMappingClass = Class.forName(typeMappingClassname);
                        if (typeMappingClass != null)
                            return typeMappingClass.newInstance();
                    } catch (ClassNotFoundException e) {
                        logger.log(Level.FINER, "Assertion " + assClass.getName() + " metadata declares a custom TypeMapping, but the class was not found: " + ExceptionUtils.getMessage(e), e);
                        /* FALLTHROUGH and try default assertion mapping */
                    } catch (IllegalAccessException e) {
                        logger.log(Level.WARNING, "Assertion " + assClass.getName() + " metadata declares a custom TypeMapping, but it could not be instantiated with the nullary constructor: " + ExceptionUtils.getMessage(e), e);
                        /* FALLTHROUGH and try default assertion mapping */
                    } catch (InstantiationException e) {
                        logger.log(Level.WARNING, "Assertion " + assClass.getName() + " metadata declares a custom TypeMapping, but it could not be instantiated with the nullary constructor: " + ExceptionUtils.getMessage(e), e);
                        /* FALLTHROUGH and try default assertion mapping */
                    }
                }
                String externalName = (String)meta.get(AssertionMetadata.WSP_EXTERNAL_NAME);
                return DefaultAssertionMetadata.cache(meta, key, new AssertionMapping(assClass, externalName));
            }
        });

        DefaultAssertionMetadata.putDefaultGetter(AssertionMetadata.USED_BY_CLIENT, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                Class assertionClass = meta.getAssertionClass();
                for (Assertion ass : AllAssertions.BRIDGE_EVERYTHING) {
                    if (ass.getClass() == assertionClass)
                        return DefaultAssertionMetadata.cache(meta, key, Boolean.TRUE);
                }
                return DefaultAssertionMetadata.cache(meta, key, Boolean.FALSE);
            }
        });

        DefaultAssertionMetadata.putDefaultGetter(AssertionMetadata.FEATURE_SET_NAME, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                Class assertionClass = meta.getAssertionClass();
                for (Assertion ass : AllAssertions.SERIALIZABLE_EVERYTHING) {
                    if (ass.getClass() == assertionClass)
                        return DefaultAssertionMetadata.cache(meta, key, null);
                }

                // Unknown assertion; treat as modular
                return "set:modularAssertions";
            }
        });

        enhancedMetadataDefaultsInstalled.set(true);
    }

    public void afterPropertiesSet() throws Exception {
        // TODO remove this hack, along with WspReader.getDefault(), as soon as everything gets WspReader through Spring
        WspConstants.setTypeMappingFinder(this);
    }
}
