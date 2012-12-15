package com.l7tech.policy.assertion;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides default information about an {@link Assertion}'s client and server implementation, GUI, policy serialization,
 * validation types, licensing requirements, audit message IDs, and other information.
 * <p/>
 * A new assertion can override these properties with static values (or with {@link MetadataFinder} instances that will find
 * them lazily).  In your new {@link Assertion} direct subclass:
 *<pre>
 *   public AssertionMetadata meta() {
 *       DefaultAssertionMetadata meta = super.defaultMeta();
 *       meta.put(AssertionMetadata.USED_BY_CLIENT, Boolean.TRUE);
 *       return meta;
 *   }
 *</pre>
 * <p/>
 * It is safe for multiple threads to use this class simultaneously.
 * <p/>
 * This class is part of the Layer 7 API and so must avoid using any post-Java-1.3 features, or any parts
 * of the Layer 7 code base other than the other bits that are already in the Layer 7 API.
 *
 * @noinspection unchecked,UnnecessaryUnboxing
 */
public class DefaultAssertionMetadata implements AssertionMetadata {
    protected static final Logger logger = Logger.getLogger(DefaultAssertionMetadata.class.getName());

    private static final Object NO_VALUE = new Object();
    private static final Pattern ucfirst = Pattern.compile("(\\b[a-z])");
    private static final Pattern badlocalnamechars = Pattern.compile("[^a-zA-Z0-9_]"); // very, very conservative.. us-ascii only!
    private static final Pattern camelSplitter = Pattern.compile("(?<=[a-z])(?=[A-Z])");

    /**
     * Convert a String like "foo b8r blatz_foo 99" into title caps, like "Foo B8r Blatz_foo 99".
     *
     * @param in the string to convert.  Must not be null.
     * @return a new String in title caps.  Never null.
     */
    private static String toTitleCaps(String in) {
        Matcher matcher = ucfirst.matcher(in);
        StringBuffer sb = new StringBuffer();
        while (matcher.find())
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Contains the code that generates the default metadata properties for this assertion.
     * Never put NO_VALUE into this map; instead, just use null or don't provide a default getter for that property.
     */
    private static final Map defaultGetters = Collections.synchronizedMap(new HashMap() {{
        put(BASE_NAME, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                String className = meta.getAssertionClass().getName();
                return cache(meta, key, Assertion.getBaseName(className));
            }
        });

        put(BASE_PACKAGE, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                String pack = getPackageName(meta.getAssertionClass().getName());
                if (pack.startsWith(".")) // can't happen
                    return "";

                String spass = ".policy.assertion";
                int ps = pack.indexOf(spass);
                if (ps >= 0) {
                    pack = pack.substring(0, ps);
                    return cache(meta, key, pack);
                }

                return cache(meta, key, pack);
            }

            public String getPackageName(String fullName) {
                int di = fullName.lastIndexOf(".");
                if (di < 2)
                    return "";
                return fullName.substring(0, di);
            }
        });

        // installed by AssertionRegistry because it relies on non-API classes
        put(SERVER_ASSERTION_CLASSNAME, null);

        // installed by AssertionRegistry because it relies on non-API classes
        put(CLIENT_ASSERTION_CLASSNAME, null);

        put(CLIENT_ASSERTION_TARGETS,  new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, new String[]{"request"});
            }
        });

        put(PROPERTIES_ACTION_NAME, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, toTitleCaps((String)meta.get(SHORT_NAME)) + " Properties");
            }
        });

        put(PROPERTIES_ACTION_DESC, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                String shortName = (String)meta.get(SHORT_NAME);
                return cache(meta, key, "Change the properties of the " + shortName + " assertion.");
            }
        });

        put(PROPERTIES_ACTION_ICON, new MetadataFinder(){
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, "com/l7tech/console/resources/Properties16.gif");
            }
        });

        put(PALETTE_NODE_NAME, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, meta.get(SHORT_NAME));
            }
        });

        put(PALETTE_NODE_ICON, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, "com/l7tech/console/resources/policy16.gif");
            }
        });

        put(BASE_64_NODE_IMAGE, null); // default no custom node image

        put(CLIENT_ASSERTION_POLICY_ICON, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, "com/l7tech/proxy/resources/tree/policy16.gif");
            }
        });

        put(CLIENT_ASSERTION_POLICY_ICON_OPEN, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, meta.get(CLIENT_ASSERTION_POLICY_ICON));
            }
        });

        put(PALETTE_NODE_SORT_PRIORITY, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, 0);
            }
        });

        put(PALETTE_NODE_CLASSNAME, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, basepack(meta) + ".console." + basename(meta) + "PaletteNode");
            }
        });

        put(PALETTE_NODE_FACTORY, null); // installed by ConsoleAssertionRegistry because it relies on console classes

        put(ASSERTION_FACTORY, null); // installed by AssertionRegistry because it relies on non-API classes

        put(POLICY_NODE_NAME, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, meta.get(SHORT_NAME));
            }
        });

        put(POLICY_NODE_NAME_FACTORY, null); // installed by AssertionRegistry because it relies on non-API classes

        put(POLICY_NODE_ICON, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, meta.get(PALETTE_NODE_ICON));
            }
        });

        put(POLICY_NODE_ICON_OPEN, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, meta.get(PALETTE_NODE_ICON));
            }
        });

        put(POLICY_NODE_CLASSNAME, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, basepack(meta) + ".console." + basename(meta) + "PolicyNode");
            }
        });

        put(POLICY_NODE_FACTORY, null); // installed by ConsoleAssertionRegistry because it relies on console classes

        put(POLICY_ADVICE_CLASSNAME, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, basepack(meta) + ".console." + basename(meta) + "Advice");
            }
        });

        put(POLICY_ADVICE_INSTANCE, null); // installed by ConsoleAssertionRegistry because it relies on console classes

        put(POLICY_VALIDATOR_CLASSNAME, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, basepack(meta) + ".policy." + basename(meta) + "AssertionValidator");
            }
        });

        put(PROPERTIES_ACTION_CLASSNAME, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, basepack(meta) + ".console." + basename(meta) + "PropertiesAction");
            }
        });

        put(PROPERTIES_ACTION_FACTORY, null); // installed by ConsoleAssertionRegistry because it relies on console classes

        put(PROPERTIES_EDITOR_CLASSNAME, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, basepack(meta) + ".console." + basename(meta) + "PropertiesDialog");
            }
        });

        put(PROPERTIES_EDITOR_FACTORY, null); // installed by ConsoleAssertionRegistry because it relies on console classes

        put(PROPERTIES_EDITOR_SUPPRESS_SHEET_DISPLAY, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return Boolean.FALSE;
            }
        });

        put(WSP_TYPE_MAPPING_CLASSNAME, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, basepack(meta) + ".wsp." + basename(meta) + "AssertionMapping");
            }
        });

        put(WSP_TYPE_MAPPING_INSTANCE, null); // installed by AssertionRegistry because it touches non-API classes

        put(WSP_EXTERNAL_NAME, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                String base = (String)meta.get(BASE_NAME);
                base = badlocalnamechars.matcher(base).replaceAll("_");
                return cache(meta, key, base);
            }
        });

        put(WSP_SUBTYPE_FINDER, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return null;
            }
        });

        put(WSP_COMPATIBILITY_MAPPINGS, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return null;
            }
        });

        // Default finder for usedByClient.  AssertionRegistry upgrades this to a smarter one that knows
        // how to check AllAssertions.BRIDGE_EVERYTHING.
        put(USED_BY_CLIENT, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, Boolean.FALSE);
            }
        });

        // Default finder for shortName.  This looks at the asertion class name, and changes (ie) FooBarAssertion into "Foo Bar".
        put(SHORT_NAME, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, camelSplitter.matcher((CharSequence)meta.get(BASE_NAME)).replaceAll(" "));
            }
        });

        // Default finder for longName.  This looks at the assertion class name, and changes (ie) FooBarAssertion into "Foo Bar Assertion".
        put(LONG_NAME, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, meta.get(SHORT_NAME) + " Assertion");
            }
        });

        // Default finder for description.  The SSM replaces this with a smarter one that looks in properties files.
        put(DESCRIPTION, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                Object longName = meta.get(LONG_NAME);
                String value = longName == null ? "This is the " + meta.get(SHORT_NAME) + " assertion." : longName.toString();
                return cache(meta, key, value);
            }
        });

        put(PROPERTIES_FILE, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, basepack(meta) + ".console.resources." + basename(meta) + "Assertion.properties");
            }
        });

        put(VARIANT_PROTOTYPES, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, new Assertion[0]);
            }
        });

        put(PALETTE_FOLDERS, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, new String[] { });
            }
        });

        // Default finder for featureSetName.  AssertionRegistry upgrades this to a smarter one that knows
        // how to check AllAssertions.SERIALIZABLE_EVERYTHING.
        put(FEATURE_SET_NAME, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return null;
            }
        });

        put(CLUSTER_PROPERTIES, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, new HashMap());
            }
        });

        put(GLOBAL_ACTION_CLASSNAMES, new String[0]);

        put(GLOBAL_ACTIONS, null); // installed by ConsoleAssertionRegistry because it relies on swing classes

        put("", new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return null;
            }
        });

        //default holder value for routing assertion
        put(IS_ROUTING_ASSERTION, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, Boolean.FALSE);
            }
        });
    }});

    /**
     * Get the base name for this assertion, ie "OneOrMore".
     *
     * @param meta  AssertionMetadata instance. Required.
     * @return the base name from the metadata, ie "OneOrMore".
     */
    public static String basename(AssertionMetadata meta) {
        return meta.get(BASE_NAME).toString();
    }

    /**
     * Get the base package for this assertion, ie "com.l7tech".
     *
     * @param meta  AssertionMetadata instance. Required.
     * @return the base class package for this metadata; ie, "com.yoyodyne.layer7" if the assertion classname is "com.yoyodyne.layer7.assertion"
     */
    public static String basepack(AssertionMetadata meta) {
        return meta.get(BASE_PACKAGE).toString();
    }

    /**
     * Get the base path for this assertion's resources, ie "com/l7tech".
     *
     * @param meta  AssertionMetadata instance. Required.
     * @return the base resource path for this metadata; ie, "com/yoyodyne/layer7" if the assertion classname is "com.yoyodyne.layer7.assertion" 
     */
    public static String basepath(AssertionMetadata meta) {
        return meta.get(BASE_PACKAGE).toString().replace('.', '/');
    }

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
    public static MetadataFinder getDefaultGetter(String key) {
        return (MetadataFinder)defaultGetters.get(key);
    }

    /**
     * Change the Getter which will be used to generate a property at runtime if one isn't set already for a given AssertionMetadata instance.
     *
     * @param key the property name whose Getter to set.  Must not be null.
     * @param metadataFinder a new Getter for this property.  Must not be null.
     */
    public static void putDefaultGetter(String key, MetadataFinder metadataFinder) {
        if (metadataFinder == null) throw new IllegalArgumentException("Getter must not be null");
        defaultGetters.put(key, metadataFinder);
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

    @Override
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
     * Get a value as a String by calling toString() on the value (if any).
     *
     * @param key the property name to get.  Must not be null.
     * @return The result of calling toString() on the specified property value, if it exists; otherwise null.
     */
    public String getString(String key) {
        Object got = get(key);
        return got == null ? null : got.toString();
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

    @Override
    public Object get(String key) {
        Object got = get(properties, key);
        if (NO_VALUE == got)
            return null;
        return got != null ? got : get(defaultGetters, key);
    }

    /**
     * Get the specified property from the specified Map, invoking get() on the value if it's a Getter.
     *
     * @param map the Map to examine.  Must not be null.
     * @param key the property name to get.  Must not be null.
     * @return The specified property value, if it exists; NO_VALUE if it is explicitly set to nothing; otherwise null.
     */
    protected Object get(Map map, String key) {
        Object got = map.get(key);
        return got instanceof MetadataFinder ? ((MetadataFinder)got).get(this, key) : got;
    }

    /**
     * Set a customized property (or Getter) just for this AssertionMetadata instance.
     *
     * @param key the property name to set.  Must not be null.
     * @param value a static value for this property, or a Getter that will find it lazily, or null to un-customize this property.
     */
    public void put(String key, @Nullable Object value) {
        if (value == null)
            properties.remove(key);
        else
            properties.put(key, value);
    }

    /**
     * Uncustomize the specified property, clearing it to null, and prevent any default Getter from overriding this.
     *
     * @param key the property that should be cleared.
     */
    public void putNull(String key) {
        properties.put(key, NO_VALUE);
    }
}
