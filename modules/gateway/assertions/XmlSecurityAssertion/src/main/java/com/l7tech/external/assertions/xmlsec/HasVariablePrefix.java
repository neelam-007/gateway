package com.l7tech.external.assertions.xmlsec;

/**
 *
 */
public interface HasVariablePrefix {
    String getVariablePrefix();

    void setVariablePrefix(String variablePrefix);

    /**
     * Prepend the current variable prefix, if any, to the specified variable name.  If the current prefix is
     * null or empty this will return the input variable name unchanged.
     *
     * @param var  the variable name to prefix.  Required.
     * @return the variable name with the current prefix prepended, along with a dot; or the variable name unchanged if the prefix is currently null or empty.
     */
    String prefix(String var);
}
