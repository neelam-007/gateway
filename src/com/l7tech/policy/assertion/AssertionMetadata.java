package com.l7tech.policy.assertion;

/**
 * Provides information about an assertions client and server implementation, GUI, policy serialization,
 * validation types, licensing requirements, audit message IDs, and other information.
 * @see DefaultAssertionMetadata  for the default implementation
 */
public interface AssertionMetadata {
    /** String.  Base name of this assertion, ie "OneOrMore". */
    String BASE_NAME = "baseName";

    /** String classname.  Name of ActionListener subclass to invoke when the Properties.. action is invoked. */
    String PROPERTIES_ACTION = "propertiesAction";

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

    /** String. Space separated list of parent feature set names. */
    String PARENT_FEATURE_SETS = "parentFeatureSets";

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
