package com.l7tech.policy.assertion;

/**
 * Provides information about an assertions client and server implementation, GUI, policy serialization,
 * validation types, licensing requirements, and other information.
 * <p/>
 * Implementations must be threadsafe, but implementations of various interfaces returned by various properties
 * queries need only be threadsafe if required by the contract for the property/interface in question.
 *
 * @see DefaultAssertionMetadata DefaultAssertionMetadata, the default implementation
 */
public interface AssertionMetadata {
    /**
     * String.  Base name of this assertion, ie "OneOrMore".
     * <p/>
     * This is used for constructing other default names, including default classnames, and so should
     * be a valid classname fragment.
     * <p/>
     * Defaults to assertion classname with the package name and any trailing "Assertion" removed.
     */
    String BASE_NAME = "baseName";

    /**
     * String.  Base package name of this assertion, ie "com.l7tech".
     * Defaults to assertion package name with any trailing ".policy.assertion" removed.
     */
    String BASE_PACKAGE = "basePackage";

    /**
     * String.  Class name of server assertion instance.  If null, or if this class can't be found or doesn't
     * have a public constructor accepting either FooAssertion or (FooAssertion, Applicationcontext),
     * this assertion cannot be used in policies enforced by the Gateway.
     * <p/>
     * The {@link com.l7tech.policy.AssertionRegistry} installs a default {@link MetadataFinder} for this property
     * whose behavior varies depending on
     * whether this assertion is part of the core product.  An assertion is considered part of the core product
     * if it is listed in either {@link com.l7tech.policy.AllAssertions#SERIALIZABLE_EVERYTHING} or
     * {@link com.l7tech.policy.AllAssertions#GATEWAY_EVERYTHING}.
     * <h3>For core assertions (present in AllAssertions):</h3>
     * The default is "${basePackage}.server.policy.assertion${localPackage}.Server${className}",
     * where className is the assertion class name not including the package name, and localPackage is
     * the assertion package name
     * with basePackage and any leading ".policy" and/or ".assertion" removed from the front.
     * <p/>
     * For example,
     * for the assertion "com.l7tech.policy.assertion.composite.OneOrMoreAssertion",
     * this defaults to "com.l7tech.server.policy.assertion.composite.ServerOneOrMoreAssertion".
     * <h3>For all other assertions (not present in AllAssertions):</h3>
     * The default is "${basePackage}.server.Server${className}".  For example, for the
     * assertion "com.yoyodyne.assertions.sqlquery.SqlQueryAssertion", this defaults to
     * "com.yoyodyne.assertions.sqlquery.server.ServerSqlQueryAssertion".
     */
    String SERVER_ASSERTION_CLASSNAME = "serverAssertionClassname";

    /**
     * String.  Class name of client assertion instance.  If null, or if this class can't be found or doesn't have
     * a public constructor accepting FooAssertion, this assertion will be ignored by the XML VPN client.
     * <p/>
     * The {@link com.l7tech.policy.AssertionRegistry} installs a default {@link MetadataFinder} for this property
     * whose behavior varies depending on
     * whether this assertion is part of the core product.  An assertion is considered part of the core product
     * if it is listed in either {@link com.l7tech.policy.AllAssertions#SERIALIZABLE_EVERYTHING} or
     * {@link com.l7tech.policy.AllAssertions#GATEWAY_EVERYTHING}.
     * <h3>For core assertions (present in AllAssertions):</h3>
     * The default is "${basePackage}.proxy.policy.assertion${localPackage}.Client${className}",
     * where className is the assertion class name not including the package name, and localPackage is
     * the assertion package name
     * with basePackage and any leading ".policy" and/or ".assertion" removed from the front.
     * <p/>
     * For example,
     * for the assertion "com.l7tech.policy.assertion.composite.OneOrMoreAssertion",
     * this defaults to "com.l7tech.proxy.policy.assertion.composite.ClientOneOrMoreAssertion".
     * <h3>For all other assertions (not present in AllAssertions):</h3>
     * The default is "${basePackage}.client.Client${className}".  For example, for the
     * assertion "com.yoyodyne.assertions.sqlquery.SqlQueryAssertion", this defaults to
     * "com.yoyodyne.assertions.sqlquery.client.ClientSqlQueryAssertion".
     */
    String CLIENT_ASSERTION_CLASSNAME = "clientAssertionClassname";

    /**
     * String[]. Messages that are processed by the client assertion. If null, this defaults to ["request"]
     *
     * <p>Permited values are "request" and "response".</p>
     */
    String CLIENT_ASSERTION_TARGETS = "clientAssertionTargets";

    /**
     * String file path.  Icon to display for the client policy node for this assertion in the XML VPN Client
     * Defaults to "com/l7tech/proxy/resources/tree/policy16.gif", which is a picture of a small piece of paper with
     * writing, and is always available on the VPN Client
     *
     * This key was introduced in SecureSpan Gateway version 5.2.
     */
    String CLIENT_ASSERTION_POLICY_ICON = "clientAssertionPolicyIcon";

    /**
     * String file path.  Icon to display for the client policy node for this assertion in the XML VPN Client
     * when it is open.
     *
     * Defaults to CLIENT_ASSERTION_POLICY_ICON
     *
     * This key was introduced in SecureSpan Gateway version 5.2.
     */
    String CLIENT_ASSERTION_POLICY_ICON_OPEN = "clientAssertionPolicyIconOpen";

    /**
     * String.  Name to display on "Properties..." pop-up menu action if using the DefaultAssertionPropertiesAction.
     * Also name to display on the properties dialog window
     * <P/>
     * Defaults to SHORT_NAME converted to title caps, then followed by " Properties".
     * For example, a SHORT_NAME of "Request XML integrity" would lead to a default properties action name of
     * "Request XML Integrity Properties".
     */
    String PROPERTIES_ACTION_NAME = "propertiesActionName";

    /**
     * String.  Tooltip/description for "Properties..." pop-up menu action if using the DefaultAssertionPropertiesAction.
     * Defaults to "Change the properties of the ${shortName} assertion."
     */
    String PROPERTIES_ACTION_DESC = "propertiesActionDesc";

    /**
     * String. Path of image icon to use for the action.
     * Defaults to com/l7tech/console/resources/Properties16.gif
     *
     * This key was introduced in SecureSpan Gateway version 5.1.
     */
    String PROPERTIES_ACTION_ICON = "propertiesActionIcon";

    /**
     * String.  Name to display on the palette node for this assertion, if using DefaultAssertionPaletteNode.
     * Defaults to SHORT_NAME.
     */
    String PALETTE_NODE_NAME = "paletteNodeName";

    /**
     * String file path.  Icon to display for the palette node for this assertion, if using DefaultAssertionPaletteNode.
     * Defaults to "com/l7tech/console/resources/policy16.gif", which is a picture of a small piece of paper with
     * writing, and is always available on both SSM and manager applet.
     */
    String PALETTE_NODE_ICON = "paletteNodeIcon";

    /**
     * Integer. The sort priority for the palette node for this assertion, if using DefaultAssertionPaletteNode.
     * Defaults to "0". Nodes with higer priority appear higher in the palette folder. When two nodes have the same
     * sort priority they are ordered by name (ignoring case).
     *
     * NOTE: currently only used for the following categories: xmlSecurity
     */
    String PALETTE_NODE_SORT_PRIORITY = "paletteNodeSortPriority";


    /**
     * String classname.  Name of AbstractAssertionPaletteNode subclass to use when creating palette nodes for this assertion.
     * Defaults to "${basePackage}.console.${baseName}PaletteNode".
     */
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
     * Base64 encoded image to display for the palette and/or policy node for this assertion. Has priority over PALETTE_NODE_ICON and POLICY_NODE_ICON.
     *
     * Defaults to null.
     */
    String BASE_64_NODE_IMAGE = "base64NodeImage";

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
     * String.  Name to display on the policy node for this assertion, if using DefaultAssertionPolicyNode and
     * a custom {@link #POLICY_NODE_NAME_FACTORY} is not provided.
     * <p/>
     * Defaults to SHORT_NAME.
     */
    String POLICY_NODE_NAME = "policyNodeName";

    /**
     * Allowed Types: AssertionNodeNameFactory
     * Deprecated: Functions.Unary< String, Assertion >
     *
     * This factory is called to generate the value to display in the policy and validator windows. The
     * AssertionNodeNameFactory is favoured over the Functions.Unary as it allows the validator window to request that
     * the assertion name not be decorated, so that it is consistent with the palette window
     * <p/>
     * This is a generator rather than just a simple String so that it can vary based on the configuration of the
     * particular assertion whose policy node is being displayed.
     * <p/>
     * If the value is a String, it's value will be used
     * <p/>
     * No other types are supported
     * Defaults to null.
     */
    String POLICY_NODE_NAME_FACTORY = "policyNodeNameFactory";

    /**
     * String file path.  Icon to display for the policy node for this assertion, if using DefaultAssertionPolicyNode.
     * Defaults to PALETTE_NODE_ICON.
     * This meta data should not be used for most assertions as the policy should show the exact same icon as the
     * palette, in which case there is no need to supply this piece of meta data.
     */
    String POLICY_NODE_ICON = "policyNodeIcon";

    /**
     * String file path.  Icon to display for policy node for this assertion, when it's open.
     * Defaults to PALETTE_NODE_ICON
     *
     * This key was introduced in SecureSpan Gateway version 5.2.
     */
    String POLICY_NODE_ICON_OPEN = "policyNodeIconOpen";

    /**
     * String classname.  Name of AssertionTreeNode subclass to use when creating tree nodes for this assertion.
     * Ignored if a valid POLICY_NODE_FACTORY is provided.
     * Defaults to "${basePackate}.console.${baseName}PolicyNode".
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
     * <p>
     * Note that a single shared instance of the Advice class is created, not one per use.
     * </p>
     * <p/>
     * Ignored if a valid POLICY_ADVICE_INSTANCE is provided.
     * <p/>
     * The default value is "${basePackage}.console.${baseName}Advice"
     */
    @SuppressWarnings({"JavadocReference"})
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
    @SuppressWarnings({"JavadocReference"})
    String POLICY_ADVICE_INSTANCE = "policyAdviceInstance";

    /**
     * Boolean value to indicate whether an assertion should enable a policy validation advice when all advices
     * are retrieved for this assertion.  The default value is true.
     *
     * This property was first introduced in the Policy Diff feature for the Icefish release 8.2.
     */
    String POLICY_VALIDATION_ADVICE_ENABLED = "policyValidationAdviceEnabled";

    /**
     * String classname.  Name of an AssertionValidator subclass that can be used to validate instances of this
     * assertion in the SSM and/or on the Gateway.
     * <p/>
     * This class should have a public unary constructor from the Assertion bean subtype.
     * <p/>
     * If one of these is provided, PathValidator will invoke it whenever this assertion is located in the
     * policy.
     * <p/>
     * The validator will be invoked once per path and so should return as quickly as possible, avoiding any time-consuming
     * computation.  In particular, it should try to avoid iterating over the AssertionPath as this may
     * exacerbate the exponential slowdown as new paths are added to the policy (an added OneOrMoreAssertion
     * with six children multiplies the number of paths by six).
     * <p/>
     * Even without providing a validator class, a new assertion can enjoy the benefit of certain validation features
     * by extending an existing Assertion superclass: for example, any assertion derived from RoutingAssertion
     * will be considered as a routing event by the path validator.
     * <p/>
     * The default value is "${basePackage}.policy.${baseName}AssertionValidator".
     * <p/>
     * If this class doesn't exist or doesn't work, no validation hook will be invoked by the PathValidator for this
     * assertion (although any normal validation rules that may apply to its superclasses will continue to be
     * in effect).
     */
    String POLICY_VALIDATOR_CLASSNAME = "policyValidatorClassname";

    /**
     * Functions.Unary< Set< ValidatorFlag >, Assertion > instance.
     *
     * <p>Allow an assertion to supply additional information on it's policy
     * preconditions or other validation aspects.</p>
     *
     * <p>This factory should return all validation flags that are applicable
     * to the passed assertion instance.</p>
     *
     * <p>The default value is null.</p>
     */
    String POLICY_VALIDATOR_FLAGS_FACTORY = "policyValidatorFlagsFactory";

    /**
     * String classname.  Name of ActionListener subclass to invoke when the Properties.. action is invoked.
     * <p/>
     * This is ingored if a valid PROPERTIES_ACTION_FACTORY is provided.
     * <p/>
     * The default value is "${basePackage}.console.action.${baseName}PropertiesAction".
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
     * The default value is "${basePackage}.console.${baseName}PropertiesDialog".
     * <p/>
     * If this is null and the assertion has editable properties a generic properties edit dialog is used.
     * If this class can't be found (or doesn't work), the SSM's default MetadataFinder
     * for the PROPERTIES_EDITOR_FACTORY will currently just give up and return null, disabling the "Properties..."
     * action in the DefaultAssertionPolicyNode for this assertion.
     * <p/>
     * To disable the properties editor for an assertion set the PROPERTIES_EDITOR_FACTORY or
     * PROPERTIES_ACTION_FACTORY to null.
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
     * If it doesn't find one, but the assertion bean includes at least one WSP-visible property, it will create
     * a factory that uses {@link com.l7tech.console.panels.DefaultAssertionPropertiesEditor}, 
     * which is a generic bean property editor dialog.
     */
    @SuppressWarnings({"JavadocReference"})
    String PROPERTIES_EDITOR_FACTORY = "propertiesEditorFactory";

    /**
     * Boolean.  Set to Boolean.TRUE if your properties dialog won't display properly as a sheet and you don't have
     * access to DialogDisplayer to turn it off yourself.  Default is Boolean.FALSE.
     * <p/>
     * Sheet display is a mechanism used by the Manager Applet to display assertion properties dialogs as
     * internal frames, within the browser tab, whenever possible.  It is not used by the standalone SSM.
     * <p/>
     * The DefaultAssertionPropertiesAction will disable sheet display for your dialog instances if this is TRUE.
     */
    String PROPERTIES_EDITOR_SUPPRESS_SHEET_DISPLAY = "propertiesEditorSuppressSheetDisplay";

    /**
     * String classname.  Name of custom TypeMapping to use for serializing this assertion, or null to just use AssertionTypeMapping.
     * If non-null, the specified TypeMapping class must exist and must have a nullary constructor.
     * <p/>
     * The default value is "${basePackage}.wsp.${baseName}AssertionMapping".
     */
    String WSP_TYPE_MAPPING_CLASSNAME = "wspTypeMappingClassname";

    /**
     * TypeMapping instance.  Actual ready-to-use TypeMapping instance for serializing/parsing this assertion.
     * If null, this assertion will NOT be convertable to/from L7 policy XML.
     * <p/>
     * The default MetadataFinder for this property will attempt to instantiate WSP_TYPE_MAPPING_CLASSNAME
     * with its nullary constructor.  If this class can't be found, or doesn't work, it will create an
     * instance of AssertionMapping, passing to the constructor the assertion class and and WSP_EXTERNAL_NAME.
     */
    String WSP_TYPE_MAPPING_INSTANCE = "wspTypeMappingInstance";

    /**
     * String.  Name of XML element local part that represents this assertion in a policy XML.
     * If null, this assertion will NOT be convertable to/from L7 policy XML.
     * <p/>
     * The default value is BASE_NAME with any characters other than letters, numbers, or the underscore replaced
     * with underscore.
     */
    String WSP_EXTERNAL_NAME = "wspExternalName";

    /**
     * TypeMappingFinder instance.  TypeMappingFinder to add to the visitor to use for unrecognized types
     * or external names while freezing or thawing instances of this assertion.
     * <p/>
     * If null, no new types will be recognized while freezing or thawing instances of this assertion.
     * <p/>
     * The default value is null.
     */
    String WSP_SUBTYPE_FINDER = "wspSubtypeFinder";

    /**
     * Map< String, TypeMapping >, external name to type mapping.
     * Compatibility mappings to add to the global pool of mappings to try
     * when an element being thawed just can't be recognized.  Usually used to set up compatibility mappings
     * to translate older versions of this serialized assertion into the modern version.
     * <p/>
     * If null, no new global mappings will be installed.
     * <p/>
     * The default value is null.
     */
    String WSP_COMPATIBILITY_MAPPINGS = "wspCompatibilityMappings";
    
    /**
     * Boolean. True if this assertion should be passed through to the Bridge.
     * <p/>
     * The default value is FALSE.
     */
    String USED_BY_CLIENT = "usedByClient";

    /**
     * String. Short name to use for this assertion, ie "Response XPath pattern".
     * The default value is BASE_NAME with space inserted before uppercase characters after the first.
     * For example, for an assertion with a BASE_NAME of "EnhancedLeafRaker", this will default to "Enhanced Leaf Raker".
     */
    String SHORT_NAME = "shortName";

    /**
     * String. Long name to use for this assertion, ie "The response must match a specified XPath pattern".
     * <p/>
     * The default value is SHORT_NAME followed by " Assertion".
     */
    String LONG_NAME = "longName";

    /**
     * String. Description to use for this assertion, if any.
     * <p/>
     * The default is to use the value of LONG_NAME, or "This is the ${shortName} assertion." if LONG_NAME is null.
     */
    String DESCRIPTION = "description";

    /**
     * String. If a GUI properties file should be used for this assertion, this holds its base name (default locale).
     * <p/>
     * This property is currently ignored by the SSM but is reserved for future use.
     * <p/>
     * The default value is "${basePackage}.console.resources.${baseName}Assertion.properties".
     */
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
     * all variant configurations offered by this assertion, and each variant provides its own prototype which
     * can be added to the palette folders.  Each variant prototype is then free to customize its returned metadata
     * for such things as paletteNodeName, assertionFactory, or even propertiesEditorFactory.
     * <p/>
     * The default value is null.
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
     *  internalAssertions (since 5.3)
     * </pre>
     * <p/>
     * The default value is an empty array.
     * <p/>
     * If this is null, or does not match any palette folder offered by this SSM version, this assertion
     * will not be offered in the palette (although it can still be XML copy/pasted).
     */
    String PALETTE_FOLDERS = "paletteFolders";

    /**
     * String. Feature set name for this assertion.
     * <p/>
     * Currently, an assertion can claim to be a modular assertion by returning "set:modularAssertions" here, or it can
     * return "(fromClass)" (or null) to be assigned the default feature set name for its classname.
     * For security reasons, any other return value will currently be ignored by
     * {@link com.l7tech.policy.assertion.Assertion#getFeatureSetName()}
     * and treated as though this property had returned null.
     * <p/>
     * The default value is "(fromClass)" for any core (non-modular) assertion listed in com.l7tech.policy.AllAssertions#SERIALIZABLE_EVERYTHING
     * on the current system, and "set:modularAssertions" for any other assertion.
     */
    String FEATURE_SET_NAME = "featureSetName";

    /**
     * Functions.Unary< Set < String >, Assertion > instance.
     *
     * <p>Allow an assertion to supply additional features that may be
     * optional. This is primarily used when an assertion has some
     * subset of functionality that is not enabled in all configurations.</p>
     *
     * <p>This factory should return all feature set names required for the
     * configuration. Note that this is in addition to the assertions
     * feature set and that should NOT be returned from this method.</p>
     *
     * <p>The default value is null.</p>
     */
    String FEATURE_SET_FACTORY = "featureSetFactory";

    /**
     * Map< String, String[2]>.  Possibly-new cluster properties used by this assertion's server implementation, or null.
     * Keys are names of cluster properties (and server config keys).  Values are tuples of [description, default].
     * <p/>
     * The default value is an empty HashMap.
     */
    String CLUSTER_PROPERTIES = "clusterProperties";

    /**
     * Functions.Unary< Collection< ExtensionInterfaceBinding >, ApplicationContext >.  Gateway-only.
     * Factory method that constructs
     * a list of implementation objects of admin extension interfaces this assertion wishes to expose via the
     * Gateway's remote admin interface.
     * <p/>
     * If a value for this property is provided, it will be invoked exactly once when this assertion prototype
     * is registered on the Gateway.  If it is a Nullary it will be invoked with no arguments; if it is a Unary
     * it will be passed the Gateway's ApplicationContext.
     * Any returned bindings will be registered with the Gateway's extensionInterfaceManager.
     * <p/>
     * If an interface is annotated with both @Administrative and @Secured, then method
     * calls will be passed through the RBAC enforcement interceptor.
     * <p/>
     * Any @Transactional annotations present on the interface will be honored when methods are invoked remotely.
     * <p/>
     * There is no default value for this property.
     * <p/>
     * This key was introduced in SecureSpan Gateway version 6.2.
     */
    String EXTENSION_INTERFACES_FACTORY = "extensionInterfacesFactory";

    /**
     * String classname.  Name of class that contains a public static void method named "onModuleLoaded" that
     * takes a single argument of type {@link org.springframework.context.ApplicationContext}.
     * <p/>
     * If this class is located in the same classloader from which came the assertion class, and contains
     * the appropriate listener method, then the SecureSpan Gateway's ServerAssertionRegistry will invoke
     * the onModuleLoaded() method when the module is being loaded, immediately prior to its assertions
     * being registered in the assertion registry.
     * <p/>
     * Modular assertions can use this to take care of any initialization that should always happen at module load time,
     * regardless of whether any actual assertion instances are present in any actual policies.  For example,
     * a module could get the applicationEventProxy bean and subscribe to application events.
     * <p/>
     * As with any class loaded from the modular assertion classloader, the module load listener class
     * can also declare a public static void nullary method named "onModuleUnloaded" that will be invoked
     * whenever its module is unloaded.
     * <p/>
     * If null, or if this class doesn't exist or can't be loaded, this property is ignored and no listener
     * method is invoked.
     * <p/>
     * Note that this listener is invoked even if assertion owning this metadata is unlicensed.  The custom listener
     * code is responsible for any needed license enforcement for associated new features.
     * <p/>
     * This key was introduced in SecureSpan Gateway version 4.2.
     */
    String MODULE_LOAD_LISTENER_CLASSNAME = "moduleLoadListenerClassname";

    /**
     * ClassLoader instance.  A ClassLoader that will be invoked to find any classes or resources that
     * are not found while searching in the modular assertion classloader from which came the assertion class.
     * <p/>
     * If this is provided, the ServerAssertionRegistry will arrange to have it added to the modular assertion
     * classloader's delegate list so it will be invoked whenever the modular assertion classloader is about to fail to
     * find a requested class or resource.  (At that point it will already have tried looking in its parent
     * classloaders, so it is not possible to shadow existing classes using this mechanism.)
     * <p/>
     * If this is null, no delegate ClassLoader will registered for this assertion.
     * <p/>
     * This key was introduced in SecureSpan Gateway version 4.2.
     */
    String MODULE_CLASS_LOADER_DELEGATE_INSTANCE = "moduleClassLoaderDelegateInstance";

    /**
     * Boolean.  A holder to determine a paritcular assertion is treated as a routing assertion.
     * TRUE means that the assertion will act as an routing assertion.  FALSE means that the assertion
     * does not act as a routing assertion.
     * <p/>
     * This key was introduced in SecureSpan Gateway version 4.7
     */
    String IS_ROUTING_ASSERTION = "isRoutingAssertion";

    /**
     * String[] classnames.  SSM only.  Names of additional Action subclasses to make available in the Tasks menu
     * when this assertion is available on the Gateway.  Each Action subclass must have a public nullary constructor.
     * <p/>
     * This value is ignored if {@link #GLOBAL_ACTIONS} is specified directly.
     * <p/>
     * Currently the actions are always added to the Tasks menu, but future versions of the SSM may support
     * additional configuration of where the Action should be exposed in the UI using custom Action property values.
     * <p/>
     * If this is null or empty, no additional global actions will be exposed in the SSM GUI when this assertion
     * is available.
     * <p/>
     * The default value is an empty array.
     * <p/>
     * It is recommended that implementors subclass SecureAction if the action should only be enabled for admin
     * users with certain permissions.
     * <p/>
     * This key was introduced in SecureSpan Manager version 5.4.
     */
    String GLOBAL_ACTION_CLASSNAMES = "globalActionClassnames";

    /**
     * Action[].  SSM only.  Additional Action instances to make available in the Tasks menu
     * when this assertion is available on the Gateway.
     * <p/>
     * Currently the actions are always added to the Tasks menu, but future versions of the SSM may support
     * additional configuration of where the Action should be exposed in the UI using custom Action property values.
     * <p/>
     * If this is null or empty, no additional global actions will be exposed in the SSM GUI when this assertion
     * is available.
     * <p/>
     * The default value is an array of instances of each element of @{link #GLOBAL_ACTION_CLASSNAMES}, each instantiated
     * using a nullary constructor; or an empty array if GLOBAL_ACTION_CLASSNAMES is null or empty. 
     */
    String GLOBAL_ACTIONS = "globalActions";

    /**
     * String.  SSM only.  File name of the module containing a Modular assertion (e.g. SshAssertion-8.0.aar).
     * This key was introduced in SecureSpan Gateway version 8.0.
     */
    String MODULE_FILE_NAME = "moduleFileName";

    /** @return the concrete Assertion class.  Returned class is never null and is always assignable to Assertion. */
    <T extends Assertion> Class<T> getAssertionClass();

    /**
     * Get the specified property.
     *
     * @param key the property name to get.  Must not be null.
     * @return The specified property value, if it exists, otherwise null.
     */
    <T> T get(String key);
}
