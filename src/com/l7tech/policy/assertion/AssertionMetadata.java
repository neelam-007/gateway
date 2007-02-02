package com.l7tech.policy.assertion;

/**
 * Provides information about an assertions client and server implementation, GUI, policy serialization,
 * validation types, licensing requirements, audit message IDs, and other information.
 * @see DefaultAssertionMetadata  for the default implementation
 */
public interface AssertionMetadata {
    /** String.  Base name of this assertion, ie "OneOrMore". */
    String PROP_BASE_NAME = "baseName";

    /** String classname.  Name of ActionListener subclass to invoke when the Properties.. action is invoked. */
    String PROP_PROPERTIES_ACTION = "propertiesAction";

    /** String classname.  Name of TypeMapping subclass to use for serializing this assertion. */
    String PROP_WSP_TYPE_MAPPING = "wspTypeMapping";

    /** Boolean. True if this assertion should be passed through to the Bridge. */
    String PROP_USED_BY_CLIENT = "usedByClient";

    /** String. Short name to use for this assertion, ie "Response XPath pattern". */
    String PROP_SHORT_NAME = "shortName";

    /** String. Long name to use for this assertion, ie "The response must match a specified XPath pattern". */
    String PROP_LONG_NAME = "longName";

    /** String. Description to use for this assertion, if any. */
    String PROP_DESCRIPTION = "description";

    /** String. If a GUI properties file should be used for this assertion, this holds its base name (default locale). */
    String PROP_PROPERTIES_FILE = "propertiesFile";

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
