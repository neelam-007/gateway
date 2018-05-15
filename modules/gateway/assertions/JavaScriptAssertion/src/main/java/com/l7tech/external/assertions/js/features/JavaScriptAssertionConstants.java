package com.l7tech.external.assertions.js.features;

/**
 * Class defining all the property names and default values
 */
public final class JavaScriptAssertionConstants {

    private JavaScriptAssertionConstants() {}

    public static final String ECMA_VERSION_PROPERTY = "js.ecmaVersion";
    public static final String DEFAULT_ECMA_VERSION = "es6";

    public static final String EXECUTION_TIMEOUT_PROPERTY = "js.executionTimeout";
    public static final String DEFAULT_EXECUTION_TIMEOUT_STRING = "${gateway." + EXECUTION_TIMEOUT_PROPERTY + "}";
    public static final int DEFAULT_EXECUTION_TIMEOUT = 1500;

    public static final String USE_STRICT_MODE_DIRECTIVE_STATEMENT = "\"use strict\";";

}
