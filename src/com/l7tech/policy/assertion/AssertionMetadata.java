package com.l7tech.policy.assertion;

/**
 * Provides information about an assertions client and server implementation, GUI, policy serialization,
 * validation types, licensing requirements, audit message IDs, and other information.
 * @see DefaultAssertionMetadata  for the default implementation
 */
public interface AssertionMetadata {
    /** String.  Base name of this assertion, ie "OneOrMore". */
    String BASE_NAME = "baseName";

    /** String.  Name to display on "Properties..." pop-up menu action if using the DefaultAssertionPropertiesAction. */
    String PROPERTIES_ACTION_NAME = "propertiesActionName";

    /** String.  Tooltip/description for "Properties..." pop-up menu action if using the DefaultAssertionPropertiesAction. */
    String PROPERTIES_ACTION_DESC = "propertiesActionDesc";

    /** String.  Name to display on the palette node for this assertion, if using DefaultAssertionPaletteNode. */
    String PALETTE_NODE_NAME = "paletteNodeName";

    /** String classname.  Name of AbstractAssertionPaletteNode subclass to use when creating palette nodes for this assertion. */
    String PALETTE_NODE_CLASSNAME = "paletteNodeClassname";

    /**
     * Functions.Unary< AbstractAssertionPaletteNode, Assertion >; SSM only.
     * A factory that can be used to make a new AbstractAssertionPaletteNode for the assertion palette for
     * a given assertion prototype instance.
     * <p/>
     * This factory's call() method takes a single Assertion as its only argument, and returns a new AbstractAssertionPaletteNode instance.
     */
    String PALETTE_NODE_FACTORY = "paletteNodeFactory";

    /**
     * Functions.Unary< String, Assertion >.  Generator of name to display on the policy node for this assertion,
     * if using DefaultAssertionPolicyNode.  This is a generator rather than just simple String so that it can
     * vary based on the configuration of the particular assertion whose policy node is being displayed.
     */
    String POLICY_NODE_NAME = "policyNodeName";

    /** String file path.  Icon to display for the policy node for this assertion, if using DefaultAssertionPolicyNode. */
    String POLICY_NODE_ICON = "policyNodeIcon";

    /**
     * String classname.  Name of AssertionTreeNode subclass to use when creating tree nodes for this assertion.
     * Ignored if a valid POLICY_NODE_FACTORY is provided.
     */
    String POLICY_NODE_CLASSNAME = "policyNodeClassname";

    /**
     * Functions.Unary< AssertionTreeNode, Assertion > instance; SSM only.
     * A factory that can be used to make a new AssertionTreeNode for the policy editor window for
     * a given assertion bean.
     * <p/>
     * This factory's call() method takes a single Assertion as its only argument, and returns a new AssertionTreeNode instance.
     * <p/>
     * The SSM's default MetadataFinder for this property will try to create a factory that instantiates POLICY_NODE_CLASSNAME;
     * failing that, it will create a factory that produces DefaultAssertionPolicyNode instances.
     *
     */
    String POLICY_NODE_FACTORY = "policyNodeFactory";

    /**
     * String classname.  Name of ActionListener subclass to invoke when the Properties.. action is invoked.
     * <p/>
     * This is ingored if a valid PROPERTIES_ACTION_FACTORY is provided.
     * <p/>
     * If this is null, or this class can't be found (or doesn't work), the
     * SSM's default MetadataFinder for the PROPERTIES_ACTION_FACTORY will attempt to use the
     * DefaultAssertionPropertiesAction, as long as a PROPERTIES_EDITOR_FACTORY is available.
     */
    String PROPERTIES_ACTION_CLASSNAME = "propertiesActionClassname";

    /**
     * Functions.Unary< Action, AssertionTreeNode > instance; SSM only.
     * Properties Action factory that can be used to make a new "Properties..." action
     * for this assertion.
     * <p/>
     * This factory's call() method takes a single AssertionTreeNode as its only argument, and returns a Action instance.
     * <p/>
     * If null, no "Properties..." action will be offered by the default assertion policy node.
     */
    String PROPERTIES_ACTION_FACTORY = "propertiesActionFactory";

    /**
     * String classname.  Name of AssertionPropertiesEditor implementor to invoke when the default properties action is invoked.
     * <p/>
     * This is ignored if a valid PROPERTIES_EDITOR_FACTORY is provided.
     * <p/>
     * If this is null, or this class can't be found (or doesn't work), the SSM's default MetadataFinder
     * for the PROPERTIES_EDITOR_FACTORY will currently just give up and return null, disabling the "Properties..."
     * action in the DefaultAssertionPolicyNode for this assertion.
     */
    String PROPERTIES_EDITOR_CLASSNAME = "propertiesEditorClassname";

    /**
     * Functions.Nullary< AssertionPropertiesEditor > instance; SSM only.
     * AssertionPropertiesEditor factory that can be used to make a new APE instance
     * that will edit properties for this assertion.
     * <p/>
     * This is a nullary factory that takes no argument and returns a new AssertionPropertiesEditor instance.
     * <p/>
     * If null, no "Properties..." action will be offered by the default assertion policy node.
     */
    String PROPERTIES_EDITOR_FACTORY = "propertiesEditorFactory";

    /**
     * String classname.  Name of custom TypeMapping to use for serializing this assertion, or null to just use AssertionTypeMapping.
     * If non-null, the specified TypeMapping class must exist and must have a nullary constructor.
     */
    String WSP_TYPE_MAPPING_CLASSNAME = "wspTypeMappingClassname";

    /**
     * TypeMapping instance.  Actual ready-to-use TypeMapping instance for serializing/parsing this assertion.
     * If null, this assertion will NOT be convertable to/from L7 policy XML.
     */
    String WSP_TYPE_MAPPING_INSTANCE = "wspTypeMappingInstance";

    /**
     * String.  Name of XML element local part that represents this assertion in a policy XML.
     * If null, this assertion will NOT be convertable to/from L7 policy XML.
     */
    String WSP_EXTERNAL_NAME = "wspExternalName";
    
    /** Boolean. True if this assertion should be passed through to the Bridge. */
    String USED_BY_CLIENT = "usedByClient";

    /** String. Short name to use for this assertion, ie "Response XPath pattern". */
    String SHORT_NAME = "shortName";

    /** String. Long name to use for this assertion, ie "The response must match a specified XPath pattern". */
    String LONG_NAME = "longName";

    /** String. Description to use for this assertion, if any. */
    String DESCRIPTION = "description";

    /** String. If a GUI properties file should be used for this assertion, this holds its base name (default locale). */
    String PROPERTIES_FILE = "propertiesFile";

    /** Assertion[].  Array of variant configurations of this assertion, or null or empty if there are no variants. */
    String VARIANT_PROTOTYPES = "variantPrototypes";

    /**
     * String[].  Array of palette folder names this assertion should appear in.
     */
    String PALETTE_FOLDERS = "paletteFolders";

    /** String. Space separated list of parent feature set names. */
    String PARENT_FEATURE_SETS = "parentFeatureSets";

    /**
     * Map<String, String[2]>.  Possibly-new cluster properties used by this assertion's server implementation, or null.
     * Keys are names of cluster properties (and server config keys).  Values are tuples of [description, default].
     * This is the same format returned by ClusterStatusAdmin#getKnownProperties.
     */
    String CLUSTER_PROPERTIES = "clusterProperties";

    /** @return the concrete Assertion class.  Returned class is never null and is always assignable to Assertion. */
    Class getAssertionClass();

    /**
     * Get the specified property.
     *
     * @param key the property name to get.  Must not be null.
     * @return The specified property value, if it exists, otherwise null.
     */
    Object get(String key);
}
