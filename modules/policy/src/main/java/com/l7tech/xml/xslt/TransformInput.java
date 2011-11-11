package com.l7tech.xml.xslt;

import com.l7tech.message.XmlKnob;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;

/**
 * Represents an input document for an XSL transformation using {@link CompiledStylesheet}.
 */
public class TransformInput implements Closeable {
    private final Functions.Unary<Object, String> variableGetter;
    private final XmlKnob xmlKnob;
    private final @Nullable Closeable closeable;

    /**
     * Create a TransformInput that uses the specified ElementCursor and variables array.
     *
     * @param xmlKnob XmlKnob for the document to be transformed.  Required.
     * @param resourceToClose optional Closeable to close after the transformation is finished, or null.
     * @param variableGetter getter for variables to make visible to the template code, or null to offer none.
     */
    public TransformInput(XmlKnob xmlKnob, @Nullable Closeable resourceToClose, Functions.Unary<Object, String> variableGetter) {
        if (xmlKnob == null) throw new NullPointerException();
        this.variableGetter = variableGetter;
        this.xmlKnob = xmlKnob;
        this.closeable = resourceToClose;
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
     * Get an XmlKnob view of the input document.
     *
     * @return an XmlKnob.  Never null.
     */
    public XmlKnob getXmlKnob() {
        return xmlKnob;
    }

    @Override
    public void close() throws IOException {
        if (closeable != null) closeable.close();
    }
}
