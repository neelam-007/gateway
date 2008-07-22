package com.l7tech.gui.util;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * DocumentFilter that enforces a size limit.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class DocumentSizeFilter extends DocumentFilter {

    //- PUBLIC

    /**
     * Create a new document size filter with the given maximum length.
     *
     * @param maxLength The maximum length
     * @throws IllegalArgumentException if maxLength is less than zero
     */
    public DocumentSizeFilter(final int maxLength) {
        if (maxLength < 0) throw new IllegalArgumentException("maxLength must be 0 or more.");
        this.maxLength = maxLength;
    }

    public void insertString(FilterBypass fb, int offs,
                             String str, AttributeSet a)
        throws BadLocationException {
        //This vetos the insertion if it would exceed the length
        if ((fb.getDocument().getLength() + str.length()) <= maxLength)
            super.insertString(fb, offs, str, a);
    }

    public void replace(FilterBypass fb, int offs,
                        int length,
                        String str, AttributeSet a)
        throws BadLocationException {
        //This vetos the replacement if it would exceed the length
        if ((fb.getDocument().getLength() + str.length() - length) <= maxLength)
            super.replace(fb, offs, length, str, a);
    }

    //- PRIVATE

    private final int maxLength;
}
