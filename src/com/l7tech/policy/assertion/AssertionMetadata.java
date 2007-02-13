package com.l7tech.policy.assertion;

/**
 * Provides information about an assertions client and server implementation, GUI, policy serialization,
 * validation types, licensing requirements, audit message IDs, and other information.
 * @see DefaultAssertionMetadata  for the default implementation
 */
public interface AssertionMetadata {
    /** String.  Base name of this assertion, ie "OneOrMore". */
    String BASE_NAME = "baseName";

    /**
     * String.  Base package name of this assertion, ie "com.l7tech".
     * Defaults to assertion package name with any trailing "assertion" package removed.
     */
    String BASE_PACKAGE = "basePackage";

    /**
     * String.  Class name of server assertion instance.
     * Defaults to assertion classname with baseName replaced by "Server${baseName}" and basePackage replaced by "${basePackage}.server".
     */
    String SERVER_ASSERTION_CLASSNAME = "serverAssertionClassname";

    /**
     * String.  Class name of client assertion instance.
     * Defaults to assertion classname with baseName replaced by "Client${baseName}" and basePackage replaced by "${basePackage}.proxy".
     */
    String CLIENT_ASSERTION_CLASSNAME = "clientAssertionClassname";

    /** String.  Name to display on "Properties..." pop-up menu action if using the DefaultAssertionPropertiesAction. */
    String PROPERTIES_ACTION_NAME = "propertiesActionName";

    /** String.  Tooltip/description for "Properties..." pop-up menu action if using the DefaultAssertionPropertiesAction. */
    String PROPERTIES_ACTION_DESC = "propertiesActionDesc";

    /** String.  Name to display on the palette node for this assertion, if using DefaultAssertionPaletteNode. */
    String PALETTE_NODE_NAME = "paletteNodeName";

    /** String file path.  Icon to display for the palette node for this assertion, if using DefaultAssertionPaletteNode. */
    String PALETTE_NODE_ICON = "paletteNodeIcon";

    /** String classname.  Name of AbstractAssertionPaletteNode subclass to use when creating palette nodes for this assertion. */
    String PALETTE_NODE_CLASSNAME = "paletteNodeClassname";

    /**
     * Functions.Unary< AbstractAssertionPaletteNode, Assertion >; SSM only.
     * A factory that can be used to make a new AbstractAssertionPaletteNode for the assertion palette for
     * a given assertion prototype instance.  This is used by palette folder nodes to add palette nodes for registered
     * assertions that have declared a desire to appear in that palette folder, by listing its ID in their
     * paletteFolders metadata property.
     * <p/>
     * This factory's call() method takes a single Assertion prototype instance as its only argument,
     * and returns a new AbstractAssertionPaletteNode instance.
     * <p/>
     * The SSM's default MetadataFinder for this property will try to load paletteNodeClassname and will
     * generate a factory that produces them if that class exists and has either a nullary constructor
     * or a unary constructor-from-Assertion; otherwise it will generate a factory that produces
     * DefaultAssertionPaletteNode instances.
     * <p/>
     * If this is null, this assertion will not be offered in the assertion palette, regardless of the contents of
     * its paletteFolders property, although it may still be usable via XML copy/paste in the policy editor panel.
     */
    String PALETTE_NODE_FACTORY = "paletteNodeFactory";

    /**
     * Functions.Unary< FooBarAssertion, FooBarAssertion >; SSM only.
     * Creates a new Assertion bean instance configured appropriately given the specified variant prototype.
     * This is used by DefaultAssertionPaletteNode to configure a new assertion instance.
     * <p/>
     * This factory takes a single FooBarAssertion instance as its only argument, and returns a new FooBarAssertion instance.
     * The passed in instance is a prototype representing a variant as returned by VARIANT_PROTOTYPES; it may be null
     * if this particular assertion metadata does not declare any configuration variants.
     * <p/>
     * The returned assertion must be a brand new instance (ie, not a prototype instance) that can be added to policies,
     * freely modified, etc.
     * <p/>
     * The SSM's default MetadataFinder for this property will create a factory that just calls getClass().newInstance()
     * on the passed-in prototype, if any, or on the metadata's assertion class otherwise.
     */
    String ASSERTION_FACTORY = "assertionFactory";

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
     */
    String POLICY_NODE_FACTORY = "policyNodeFactory";

    /**
     * String classname.  Name of {@link com.l7tech.console.tree.policy.advice.Advice} subclass to use when
     * this assertion is added to a policy, the special string "auto" to enable the default advice (which
     * shows the property dialog, if one is configured), or the special string "none" to use no Advice for this
     * assertion.
     * <p/>
     * Ignored if a valid POLICY_ADVICE_INSTANCE is provided.
     * <p/>
     * The default value is com.l7tech.console.tree.policy.advice.${BASE_NAME}Advice
     */
    String POLICY_ADVICE_CLASSNAME = "policyAdviceClassname";

    /**
     * {@link com.l7tech.console.tree.policy.advice.Advice} instance.  The actual Advice instance to invoke
     * when this assertion is added to a policy.  If this is null, no Advice will be invoked.
     * <P/>
     * The SSM's default MetadataFinder for this property will query POLICY_ADVICE_CLASSNAME.
     * If it is "auto", it will instantiate {@link com.l7tech.console.tree.policy.advice.DefaultAssertionAdvice}.
     * If it is the name of an Advice subclass that can be instantiated, it will use that.
     * Otherwise, it will use null for this property.
     */
    String POLICY_ADVICE_INSTANCE = "policyAdviceInstance";

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
     * Functions.Binary< AssertionPropertiesEditor< FooBarAssertion >, Frame, FooBarAssertion > instance; SSM only.
     * AssertionPropertiesEditor factory that can be used to make a new APE instance
     * that will edit properties for this assertion (here with type depicted as FooBarAssertion).
     * <p/>
     * This is a factory that takes two arguments: a Frame to use the parent for any JDialog that may need to be
     * displayed, and the FooBarAssertion bean that is to be edited.  It returns an AssertionPropertiesEditor
     * instance ready to edit the assertion bean.
     * <p/>
     * If null, no "Properties..." action will be offered by the DefaultAssertionPolicyNode.
     * <p/>
     * The SSM's default MetadataFinder for this property will look for an AssertionPropertiesEditor implementor
     * at PROPERTIES_EDITOR_CLASSNAME and build a factory that creates instances of it as long as it has a
     * public constructor in one of the following formats (in decreasing preference order):
     * <pre>
     *    public FooBarPropertiesDialog(Frame parent, FooBarAssertion bean)
     *    public FooBarPropertiesDialog(Frame parent) // factory will call setData(bean)
     *    public FooBarPropertiesDialog(FooBarAssertion bean)
     *    public FooBarPropertiesDialog()             // factory will call setData(bean)
     * </pre>
     */
    String PROPERTIES_EDITOR_FACTORY = "propertiesEditorFactory";

    /**
     * Boolean.  Set to Boolean.TRUE if your properties dialog won't display properly as a sheet and you don't have
     * access to DialogDisplayer to turn it off yourself.
     * (Sheet display is a mechanism used by the Manager Applet to display assertion properties dialogs as
     * internal frames, within the browser tab, whenever possible.  It is not used by the standalone SSM.)
     * The DefaultAssertionPropertiesAction will disable sheet display on your dialog if this is TRUE.
     */
    String PROPERTIES_EDITOR_SUPPRESS_SHEET_DISPLAY = "propertiesEditorSuppressSheetDisplay";

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

    /**
     * Assertion[].  Array of prototype instances of variant default configurations of this assertion (including this one),
     * or null or empty if there are no other variants.
     * <p/>
     * Explanation: some assertions present multiple palette nodes represeting different initial configurations.
     * For example, the SslAssertion has one palette node in the "Transport Layer Security" folder whose initial
     * configuration is "Require SSL; client cert optional", and a second palette node in the "Access Control"
     * folder whose initial configuration is "Require SSL and client cert".
     * <p/>
     * Thus there are two different palette nodes representing the same assertion.  Unfortunately, an AssertionRegistry
     * holds only one prototype instance per assertion concrete class.
     * <p/>
     * The solution is to use this variantPrototypes property: the default prototype can be queried for a list of
     * all variant configuraitons offered by this assertion, and each variant provides its own prototype which
     * can be added to the palette folders.  Each variant prototype is then free to customize its returned metadata
     * for such things as paletteNodeName, assertionFactory, or even propertiesEditorFactory.
     */
    String VARIANT_PROTOTYPES = "variantPrototypes";

    /**
     * String[].  Array of palette folder IDs this assertion variant should appear in.
     * Here is the list of valid palette folder IDs as of version 3.7:
     * <pre>
     *  accessControl
     *  transportLayerSecurity
     *  xmlSecurity
     *  xml
     *  routing
     *  misc
     *  audit
     *  policyLogic
     *  threatProtection
     * </pre>
     * If this is null, or does not match any palette folder offered by this SSM version, this assertion
     * will not be offered in the palette (although it can still be XML copy/pasted).
     */
    String PALETTE_FOLDERS = "paletteFolders";

    /**
     * String. Feature set name for this assertion.
     * <p/>
     * An assertion can claim to be a modular assertion by returning "set:modularAssertions" here, or it can
     * return null to be assigned the default feature set name for its class.  For security reasons, any other
     * return value will currently be ignored by {@link com.l7tech.policy.assertion.Assertion#getFeatureSetName()}
     * and treated as though this property had returned null.
     * <p/>
     * AssertionRegistry installs a default MetadataFinder for this property that returns null for any assertion
     * present in AllAssertions on this system, and "set:modularAssertions" for any other assertion.
     */
    String FEATURE_SET_NAME = "featureSetName";

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
