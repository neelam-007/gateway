package com.l7tech.common.xml.xslt;

import com.l7tech.common.util.Functions;
import com.l7tech.common.xml.ElementCursor;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Represents an input document for an XSL transformation using {@link CompiledStylesheet}.
 */
public class TransformInput {
    private final Functions.Unary<Object, String> variableGetter;
    private final ElementCursor elementCursor;

    /**
     * Create a TransformInput that uses the specified ElementCursor and variables array.
     *
     * @param elementCursor ElementCursor open against the document to be transformed.  Required.
     *                      This can be either a DOM or Tarari ElementCursor.
     * @param variableGetter getter for variables to make visible to the template code, or null to offer none.
     */
    public TransformInput(ElementCursor elementCursor, Functions.Unary<Object, String> variableGetter) {
        if (elementCursor == null) throw new NullPointerException();
        this.variableGetter = variableGetter;
        this.elementCursor = elementCursor;
    }

    /**
     * Get the value of the given named variable.
     *
     * @param variableName  the variable to look up.
     * @return the result of getting this variable using our variableGetter, or null.
     */
    public Object getVariableValue(String variableName) {
        return variableGetter == null ? null : variableGetter.call(variableName);
    }

    /**
     * Get a variable getter that will look up the values of named variables as needed by this TransformInput.
     *
     * @return a variable getter, or null if none is available.
     */
    public Functions.Unary<Object, String> getVariableGetter() {
        return variableGetter;
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
