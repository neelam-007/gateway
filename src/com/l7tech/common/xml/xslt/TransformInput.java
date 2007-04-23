package com.l7tech.common.xml.xslt;

import com.l7tech.common.xml.ElementCursor;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Map;

/**
 * Represents an input document for an XSL transformation using {@link CompiledStylesheet}.
 */
public class TransformInput {
    private final Map vars;
    private final ElementCursor elementCursor;

    /**
     * Create a TransformInput that uses the specified ElementCursor and variables array.
     *
     * @param elementCursor ElementCursor open against the document to be transformed.  Required.
     *                      This can be either a DOM or Tarari ElementCursor.
     * @param vars array of variables to make visible to the template code.  Required, but may be empty.
     */
    public TransformInput(ElementCursor elementCursor, Map vars) {
        if (vars == null || elementCursor == null) throw new NullPointerException();
        this.vars = vars;
        this.elementCursor = elementCursor;
    }

    /**
     * Get the variables that should be visible during the transformation.
     *
     * @return a Map of variable name to variable value.  May be empty but never null.
     */
    public Map getVars() {
        return vars;
    }

    /**
     * Get an ElementCursor view of the input document.  This may be either a Tarari or DOM element cursor.
     *
     * @return an ElementCursor instance.  Never null.
     * @throws IOException if there is a problem reading the source document
     * @throws SAXException if there is a problem parsing the source document
     */
    public ElementCursor getElementCursor() throws SAXException, IOException {
        return elementCursor;
    }
}
