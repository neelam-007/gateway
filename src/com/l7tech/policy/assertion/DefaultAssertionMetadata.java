package com.l7tech.policy.assertion;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Provides information about an assertions client and server implementation, GUI, policy serialization,
 * validation types, licensing requirements, audit message IDs, and other information.
 *
 * @noinspection UnnecessaryUnboxing,unchecked
 */
public class DefaultAssertionMetadata implements AssertionMetadata {
    /** Interface implemented by lazy getters of assertion properties. */
    public static interface Getter {
        /**
         * Get the specified property for the specified AssertionMetadata instance.
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
    private static final Map defaultGetters = new HashMap() {{
        put(PROP_BASE_NAME, new Getter() {
            public Object get(AssertionMetadata meta, String key) {
                String className = meta.getAssertionClass().getName();
                if (className.endsWith("."))
                    throw new IllegalStateException("assertionClass name ends with dot");
                int lastDot = className.lastIndexOf(".");
                String rest = lastDot < 1 ? className : className.substring(lastDot + 1);
                if (rest.endsWith("Assertion"))
                    rest = rest.substring(0, rest.length() - "Assertion".length());
                return rest;
            }
        });

        put(PROP_PROPERTIES_ACTION, new Getter() {
            public Object get(AssertionMetadata meta, String key) {
                return "com.l7tech.console.action." + meta.get(PROP_BASE_NAME) + "PropertiesAction";
            }
        });

        put(PROP_WSP_TYPE_MAPPING, new Getter() {
            public Object get(AssertionMetadata meta, String key) {
                return "com.l7tech.policy.wsp." + meta.get(PROP_BASE_NAME) + "AssertionMapping";
            }
        });

        put(PROP_USED_BY_CLIENT, new Getter() {
            public Object get(AssertionMetadata meta, String key) {
                return Boolean.FALSE;
            }
        });

        // Default finder for shortName.  The SSM replaces this with a smarter one that looks in properties files.
        put(PROP_SHORT_NAME, new Getter() {
            public Object get(AssertionMetadata meta, String key) {
                return Pattern.compile("(?<=[a-z])(?=[A-Z])").matcher((CharSequence)meta.get(PROP_BASE_NAME)).replaceAll(" ");
            }
        });

        // Default finder for longName.  The SSM replaces this with a smarter one that looks in properties files.
        put(PROP_LONG_NAME, new Getter() {
            public Object get(AssertionMetadata meta, String key) {
                return meta.get(PROP_SHORT_NAME) + " Assertion";
            }
        });

        // Default finder for description.  The SSM replaces this with a smarter one that looks in properties files.
        put(PROP_DESCRIPTION, new Getter() {
            public Object get(AssertionMetadata meta, String key) {
                return "";
            }
        });

        put(PROP_PROPERTIES_FILE, new Getter() {
            public Object get(AssertionMetadata meta, String key) {
                return "com.l7tech.console.resources." + meta.get(PROP_BASE_NAME) + "Assertion.properties";
            }
        });

        put("", new Getter() {
            public Object get(AssertionMetadata meta, String key) {
                return null;
            }
        });
    }};

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
    private final Map properties = new HashMap();

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
     * Convenience accessor for the very frequently-needed property {@link #PROP_BASE_NAME}.
     *
     * @return the base name of the assertion, ie "FooBar" for com.l7tech.policy.assertion.blahblah.FooBarAssertion.
     */
    public String getBaseName() {
        return getString(PROP_BASE_NAME);
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
    protected void put(String key, Object value) {
        if (value == null)
            properties.remove(key);
        else
            properties.put(key, value);
    }
}
