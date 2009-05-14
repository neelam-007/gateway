package com.l7tech.xml.xpath;

/**
 * Interface implemented by users of {@link CompiledXpath} that wish to be able to specify variable values
 * at runtime.  A CompiledXpath that encounters a variable reference during evaluation will ask {@link XpathVariableContext}
 * for the current thread-local XpathVariableFinder. 
 */
public interface XpathVariableFinder {
    /**
     * Get the value of the specified variable.
     *
     * @param namespaceUri  Reserved.  Implementors must throw NoSuchXpathVariableException if this parameter is anything other than the empty string. 
     * @param variableName  The name of the variable being referenced.  Never null or empty.
     * @return the value of the specified variable, which may be null.
     * @throws NoSuchXpathVariableException if the specified variable name is not currently recognized.
     */
    Object getVariableValue(String namespaceUri, String variableName) throws NoSuchXpathVariableException;
}
