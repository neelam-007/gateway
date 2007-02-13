package com.l7tech.policy.assertion;

import com.l7tech.common.util.ClassUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
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
 * <p/>
 * This class is part of the Layer 7 API and so must avoid using any post-Java-1.3 features, or any parts
 * of the Layer 7 code base other than the other bits that are already in the Layer 7 API.
 *
 * @noinspection unchecked,UnnecessaryUnboxing
 */
public class DefaultAssertionMetadata implements AssertionMetadata {
    protected static final Logger logger = Logger.getLogger(DefaultAssertionMetadata.class.getName());

    private static final Pattern ucfirst = Pattern.compile("(\\b[a-z])");
    private static final Pattern badlocalnamechars = Pattern.compile("[^a-zA-Z0-9_]"); // very, very conservative.. us-ascii only!

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
     */
    private static final Map defaultGetters = Collections.synchronizedMap(new HashMap() {{
        put(BASE_NAME, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                String className = meta.getAssertionClass().getName();
                return cache(meta, key, Assertion.getBaseName(className));
            }
        });

        put(BASE_PACKAGE, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                String pack = ClassUtils.getPackageName(meta.getAssertionClass());
                if (pack.startsWith(".")) // can't happen
                    return "";

                String spass = ".policy.assertion.";
                int ps = pack.indexOf(spass);
                if (ps >= 0) {
                    pack = pack.substring(0, ps);
                    return cache(meta, key, pack);
                }

                String sass = ".assertion.";
                ps = pack.indexOf(sass);
                if (ps >= 0) {
                    pack = pack.substring(0, ps);
                    return cache(meta, key, pack);
                }

                String sp = ".policy.";
                ps = pack.indexOf(sp);
                if (ps >= 0) {
                    pack = pack.substring(0, ps);
                    return cache(meta, key, pack);
                }

                return cache(meta, key, pack);
            }
        });

        put(SERVER_ASSERTION_CLASSNAME, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, makeDefaultSpecificClass(meta, "server", "Server"));
            }
        });

        put(CLIENT_ASSERTION_CLASSNAME, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, makeDefaultSpecificClass(meta, "proxy", "Client"));
            }
        });

        put(PROPERTIES_ACTION_NAME, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, toTitleCaps((String)meta.get(SHORT_NAME)) + " Properties");
            }
        });

        put(PROPERTIES_ACTION_DESC, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                String shortName = (String)meta.get(SHORT_NAME);
                return cache(meta, key, "Change the properties of the " + shortName + " assertion.");
            }
        });

        put(PALETTE_NODE_NAME, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, meta.get(SHORT_NAME));
            }
        });

        put(PALETTE_NODE_ICON, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, basepath(meta) + "/console/resources/policy16.gif");
            }
        });

        put(PALETTE_NODE_CLASSNAME, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, basepack(meta) + ".console.tree." + meta.get(BASE_NAME) + "PaletteNode");
            }
        });

        put(PALETTE_NODE_FACTORY, null); // installed by ConsoleAssertionRegistry because it relies on console classes

        put(ASSERTION_FACTORY, null); // installed by AssertionRegistry because it relies on non-API classes

        put(POLICY_NODE_NAME, null); // installed by AssertionRegistry because it relies on non-API classes

        put(POLICY_NODE_ICON, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, basepath(meta) + "/console/resources/policy16.gif");
            }
        });

        put(POLICY_NODE_CLASSNAME, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, basepack(meta) + ".console.tree.policy." + meta.get(BASE_NAME) + "PolicyNode");
            }
        });

        put(POLICY_NODE_FACTORY, null); // installed by ConsoleAssertionRegistry because it relies on console classes

        put(POLICY_ADVICE_CLASSNAME, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, basepack(meta) + ".console.tree.policy.advice." + meta.get(BASE_NAME) + "Advice");
            }
        });

        put(POLICY_ADVICE_INSTANCE, null); // installed by ConsoleAssertionRegistry because it relies on console classes

        put(PROPERTIES_ACTION_CLASSNAME, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, basepack(meta) + ".console.action." + meta.get(BASE_NAME) + "PropertiesAction");
            }
        });

        put(PROPERTIES_ACTION_FACTORY, null); // installed by ConsoleAssertionRegistry because it relies on console classes

        put(PROPERTIES_EDITOR_CLASSNAME, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, basepack(meta) + ".console.panels." + meta.get(BASE_NAME) + "PropertiesDialog");
            }
        });

        put(PROPERTIES_EDITOR_FACTORY, null); // installed by ConsoleAssertionRegistry because it relies on console classes

        put(PROPERTIES_EDITOR_SUPPRESS_SHEET_DISPLAY, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return Boolean.FALSE;
            }
        });

        put(WSP_TYPE_MAPPING_CLASSNAME, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, basepack(meta) + ".policy.wsp." + meta.get(BASE_NAME) + "AssertionMapping");
            }
        });

        put(WSP_TYPE_MAPPING_INSTANCE, null); // installed by AssertionRegistry because it touches non-API classes

        put(WSP_EXTERNAL_NAME, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                String base = (String)meta.get(BASE_NAME);
                base = badlocalnamechars.matcher(base).replaceAll("_");
                return cache(meta, key, base);
            }
        });

        put(WSP_SUBTYPE_FINDER, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return null;
            }
        });

        put(WSP_COMPATIBILITY_MAPPINGS, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return null;
            }
        });

        // Default finder for usedByClient.  AssertionRegistry upgrades this to a smarter one that knows
        // how to check AllAssertions.BRIDGE_EVERYTHING.
        put(USED_BY_CLIENT, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, Boolean.FALSE);
            }
        });

        // Default finder for shortName.  This looks at the asertion class name, and changes (ie) FooBarAssertion into "Foo Bar".
        put(SHORT_NAME, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, Pattern.compile("(?<=[a-z])(?=[A-Z])").matcher((CharSequence)meta.get(BASE_NAME)).replaceAll(" "));
            }
        });

        // Default finder for longName.  This looks at the assertion class name, and changes (ie) FooBarAssertion into "Foo Bar Assertion".
        put(LONG_NAME, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, meta.get(SHORT_NAME) + " Assertion");
            }
        });

        // Default finder for description.  The SSM replaces this with a smarter one that looks in properties files.
        put(DESCRIPTION, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, "This is the " + meta.get(SHORT_NAME) + " assertion.");
            }
        });

        put(PROPERTIES_FILE, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, basepack(meta) + ".console.resources." + meta.get(BASE_NAME) + "Assertion.properties");
            }
        });

        put(VARIANT_PROTOTYPES, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, new Assertion[0]);
            }
        });

        put(PALETTE_FOLDERS, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, new String[] { });
            }
        });

        // Default finder for featureSetName.  AssertionRegistry upgrades this to a smarter one that knows
        // how to check AllAssertions.SERIALIZABLE_EVERYTHING.
        put(FEATURE_SET_NAME, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return null;
            }
        });

        put(CLUSTER_PROPERTIES, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return cache(meta, key, new HashMap());
            }
        });

        put("", new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return null;
            }
        });
    }});

    /**
     * Make the default specific class for the specified assertion metadata.
     * <p/>
     * For example,
     * for the assertion "com.l7tech.policy.assertion.composite.OneOrMoreAssertion"
     * and the type "Server",
     * this returns "com.l7tech.server.policy.assertion.composite.ServerOneOrMoreAssertion".
     * <p/>
     * For the assertion "com.yoyodyne.layer7.policy.assertion.test.muggo.FumpleSnortz"
     * and the type "Client",
     * this returns 'com.yoyodyne.layer7.client.policy.assertion.test.muggo.ClientFumpleSnortz".
     *
     * @param meta  the AssertionMetadata.  Required.
     * @param packageInsert the package to insert after the base packe, ie "proxy" or "server".  Required. 
     * @param classPrefix  the prepend to the base classname, ie "Client" or "Server", with initial capital.  Required.
     * @return the default specific class name for this metadata and type.
     */
    private static String makeDefaultSpecificClass(AssertionMetadata meta, String packageInsert, String classPrefix) {
        String assname = meta.getAssertionClass().getName();
        String basePack = basepack(meta);
        String rest = ClassUtils.stripPrefix(assname, basePack + "."); // "com.yoyodyne.assertion.a.b.FooAssertion" => "assertion.a.b.FooAssertion"
        rest = ClassUtils.stripPrefix(rest, "policy.");
        rest = ClassUtils.stripPrefix(rest, "assertion.");                 // "assertion.a.b.FooAssertion" => "a.b.FooAssertion"
        rest = ClassUtils.stripSuffix(rest, ClassUtils.getClassName(rest));// "a.b.FooAssertion" => "a.b"
        rest = ClassUtils.stripSuffix(rest, ".");
        if (rest.length() > 0) rest = rest + ".";
        return basepack(meta) + "." + packageInsert + ".policy.assertion." + rest + classPrefix + ClassUtils.getClassName(assname);
    }

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
        return got instanceof MetadataFinder ? ((MetadataFinder)got).get(this, key) : got;
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
