package com.l7tech.policy.assertion;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Provides information about an assertions client and server implementation, GUI, policy serialization,
 * validation types, licensing requirements, audit message IDs, and other information.
 * <p/>
 * A new assertion can override these properties with static values (or with Getter instances that will find
 * them lazily).  In your new Assertion direct subclass:
 *<pre>
 *   public AssertionMetadata meta() {
 *       DefaultAssertionMetadata meta = super.defaultMeta();
 *       meta.put(AssertionMetadata.USED_BY_CLIENT, Boolean.TRUE);
 *       return meta;
 *   }
 *</pre>
 * <p/>
 * It is safe for multiple threads to use this class simultaneously.
 *
 * @noinspection unchecked,UnnecessaryUnboxing
 */
public class DefaultAssertionMetadata implements AssertionMetadata {
    protected static final Logger logger = Logger.getLogger(DefaultAssertionMetadata.class.getName());

    /**
     * Interface implemented by lazy getters of assertion properties.
     */
    public static interface Getter {
        /**
         * Get the specified property for the specified AssertionMetadata instance.
         * <p/>
         * Implementors should keep in mind that multiple threads may be calling
         * this method at the same time, possibly passing the same AssertionMetadata instance.
         *
         * @param meta the AssertionMetadata instance whose property to find or generate.  Must not be null.
         * @param key the name of the property that is being fetched.  Must not be null.
         * @return the property value, if able to find or generate one, or null.
         */
        Object get(AssertionMetadata meta, String key);
    }

    /**
     * Contains the code that generates the default metadata properties for this assertion.
     */
    private static final Map defaultGetters = Collections.synchronizedMap(new HashMap() {{
        put(BASE_NAME, new Getter() {
            public Object get(AssertionMetadata meta, String key) {
                String className = meta.getAssertionClass().getName();
                return cache(meta, key, Assertion.getBaseName(className));
            }
        });

        put(PROPERTIES_ACTION, new Getter() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, "com.l7tech.console.action." + meta.get(BASE_NAME) + "PropertiesAction");
            }
        });

        put(WSP_TYPE_MAPPING_CLASSNAME, new Getter() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, "com.l7tech.policy.wsp." + meta.get(BASE_NAME) + "AssertionMapping");
            }
        });

        put(WSP_EXTERNAL_NAME, new Getter() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, meta.get(BASE_NAME));
            }
        });

        // Default finder for wspTypeMappingInstance.  AssertionRegistry upgrades this to a smarter one that knows
        // how to fall back to AssertionMapping.
        put(WSP_TYPE_MAPPING_INSTANCE, new Getter() {
            public Object get(AssertionMetadata meta, String key) {
                try {
                    return Class.forName((String)meta.get(AssertionMetadata.WSP_TYPE_MAPPING_CLASSNAME));
                } catch (ClassNotFoundException e) {
                    // Give up.  AssertionRegistry will override this Getter with a smarter one that
                    // knows how to fall back to AssertionMapping.
                    return null;
                }
            }
        });

        // Default finder for usedByClient.  AssertionRegistyr upgrades this to a smarter one that knows
        // how to check AllAssertions.BRIDGE_EVERYTHING.
        put(USED_BY_CLIENT, new Getter() {
            public Object get(AssertionMetadata meta, String key) {
                return Boolean.FALSE;
            }
        });

        // Default finder for shortName.  The SSM replaces this with a smarter one that looks in properties files.
        put(SHORT_NAME, new Getter() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, Pattern.compile("(?<=[a-z])(?=[A-Z])").matcher((CharSequence)meta.get(BASE_NAME)).replaceAll(" "));
            }
        });

        // Default finder for longName.  The SSM replaces this with a smarter one that looks in properties files.
        put(LONG_NAME, new Getter() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, meta.get(SHORT_NAME) + " Assertion");
            }
        });

        // Default finder for description.  The SSM replaces this with a smarter one that looks in properties files.
        put(DESCRIPTION, new Getter() {
            public Object get(AssertionMetadata meta, String key) {
                return "";
            }
        });

        put(PROPERTIES_FILE, new Getter() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, "com.l7tech.console.resources." + meta.get(BASE_NAME) + "Assertion.properties");
            }
        });

        put(PARENT_FEATURE_SETS, new Getter() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, "set:experimental");
            }
        });

        put("", new Getter() {
            public Object get(AssertionMetadata meta, String key) {
                return null;
            }
        });
    }});

    /**
     * If the specified AssertionMetadata is actually a DefaultAssertionMetadata, overwrite the specified
     * parameter key with the specified static value.
     * <p/>
     * Use in a default Getter, if it knows its result will not change if it is called again, to save needlessly
     * recalculating the result.
     *
     * @param meta  an AssertionMetadata instance that may be an instance of DefaultAssertionMetadata.
     * @param key   the key whose value was recently calculated.  Must not be null or empty.
     * @param value the new value to cache for this key.  Should normally not be null.
     * @return the exact value passed in, for convenience.
     */
    public static Object cache(AssertionMetadata meta, String key, Object value) {
        if (meta instanceof DefaultAssertionMetadata)
            ((DefaultAssertionMetadata)meta).put(key, value);
        return value;
    }

    /**
     * Get the Getter which will be used to generate a property at runtime if one isn't set already for a given AssertionMetadata instance.
     *
     * @param key the property name whose Getter to examine.  Must not be null.
     * @return the Getter that will be used to generate this property for a given assertion, or null if we don't have one.
     */
    public static Getter getDefaultGetter(String key) {
        return (Getter)defaultGetters.get(key);
    }

    /**
     * Change the Getter which will be used to generate a property at runtime if one isn't set already for a given AssertionMetadata instance.
     *
     * @param key the property name whose Getter to set.  Must not be null.
     * @param getter a new Getter for this property.  Must not be null.
     */
    public static void putDefaultGetter(String key, Getter getter) {
        if (getter == null) throw new IllegalArgumentException("Getter must not be null");
        defaultGetters.put(key, getter);
    }

    // Instance

    private final Assertion prototype;
    private final Map properties = Collections.synchronizedMap(new HashMap());

    /**
     * Create an AssertionMetadata instance for the specified prototype assertion.
     * Here the prototype represents its entire class.
     *
     * @param prototype an instance of the Assertion subclass this AssertionMetadata is to describe.  Must not be null.
     */
    public DefaultAssertionMetadata(Assertion prototype) {
        if (prototype == null) throw new IllegalArgumentException("Prototype must not be null");
        this.prototype = prototype;
    }

    /** @return a prototype instance of the Assertion this AssertionMetadata represents. */
    protected Assertion getPrototype() {
        return prototype;
    }

    public Class getAssertionClass() {
        return prototype.getClass();
    }

    /**
     * Convenience accessor for the very frequently-needed property {@link #BASE_NAME}.
     *
     * @return the base name of the assertion, ie "FooBar" for com.l7tech.policy.assertion.blahblah.FooBarAssertion.
     */
    public String getBaseName() {
        return getString(BASE_NAME);
    }

    /**
     * @param key the property name to get.  Must not be null.
     * @return The specified property value, if it exists and is a String, otherwise null.
     */
    public String getString(String key) {
        Object got = get(key);
        return got instanceof String ? ((String)got) : null;
    }

    /**
     * @param key the property name to get.  Must not be null.
     * @return The specified property value, if it exists and is a Boolean, otherwise false.
     */
    public boolean getBoolean(String key) {
        Object got = get(key);
        return got instanceof Boolean && ((Boolean)got).booleanValue();
    }

    /**
     * @param key the property name to get.  Must not be null.
     * @return The specified property value, if it exists and is an Integer, otherwize 0.
     */
    public int getInteger(String key) {
        Object got = get(key);
        return got instanceof Integer ? ((Integer)got).intValue() : 0;
    }

    public Object get(String key) {
        Object got = get(properties, key);
        return got != null ? got : get(defaultGetters, key);
    }

    /**
     * Get the specified property from the specified Map, invoking get() on the value if it's a Getter.
     *
     * @param map the Map to examine.  Must not be null.
     * @param key the property name to get.  Must not be null.
     * @return The specified property value, if it exists, otherwise null.
     */
    protected Object get(Map map, String key) {
        Object got = map.get(key);
        return got instanceof Getter ? ((Getter)got).get(this, key) : got;
    }

    /**
     * Set a customized property (or Getter) just for this AssertionMetadata instance.
     *
     * @param key the property name to set.  Must not be null.
     * @param value a static value for this property, or a Getter that will find it lazily, or null to un-customize this property.
     */
    public void put(String key, Object value) {
        if (value == null)
            properties.remove(key);
        else
            properties.put(key, value);
    }
}
