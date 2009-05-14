package com.l7tech.xml.xpath;

import com.l7tech.xml.InvalidXpathException;
import org.jaxen.JaxenException;

/**
 * Creates and holds a DomCompiledXpath or else the reason why compilation failed.
 */
public class DeferredFailureDomCompiledXpathHolder {
    private final DomCompiledXpath compiledXpath;
    private final InvalidXpathException compileFailure;

    /**
     * Compile the specified XPath as a DomCompiledXpath and retain the compiled xpath or the compiler error
     * for future use.
     *
     * @param xpath the xpath to compile.  Required.
     */
    public DeferredFailureDomCompiledXpathHolder(CompilableXpath xpath) {
        DomCompiledXpath xp;
        InvalidXpathException fail;
        try {
            xp = new DomCompiledXpath(xpath);
            fail = null;
        } catch (InvalidXpathException e) {
            xp = null;
            fail = e;
        }
        this.compiledXpath = xp;
        this.compileFailure = fail;
    }

    /**
     * Get the compiled XPath, if it was compiled successfully.
     *
     * @return the DomCompiledXpath instance.  Never null.
     * @throws InvalidXpathException if the XPath did not compile.
     */
    public DomCompiledXpath getCompiledXpath() throws JaxenException {
        if (compileFailure != null)
            throw new JaxenException(compileFailure);
        if (compiledXpath == null)
            throw new JaxenException("compiledXpath is null"); // can't happen
        return compiledXpath;
    }

    /** @return the compiler error, if compilation failed, or null if it succeeded. */
    public InvalidXpathException getCompileFailure() {
        return compileFailure;
    }
}
