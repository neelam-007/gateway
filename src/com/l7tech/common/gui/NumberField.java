package com.l7tech.common.gui;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;


/**
 * <CODE>NumberField</CODE> is the implementation of
 * <CODE>PlainDocument</CODE> accepting only digits, and only up to a given maximum length.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 * @see javax.swing.text.Document
 * @see javax.swing.JTextField
 */
public class NumberField extends PlainDocument {
    private int maxDigits;

    public NumberField() {
        maxDigits = String.valueOf(Integer.MAX_VALUE).length();
    }

    public NumberField(int maxDigits) {
        this.maxDigits = maxDigits;
    }

    /**
     * Inserts some content into the document.
     *
     * @param offs the starting offset >= 0
     * @param str the string to insert; does nothing with null/empty strings
     * @param a the attributes for the inserted content
     * @exception javax.swing.text.BadLocationException  the given insert position is not a valid
     *   position within the document
     * @see javax.swing.text.Document#insertString
     */
    public void insertString(int offs, String str, AttributeSet a)
            throws BadLocationException
    {
        if (str == null) return;

        char[] cArr = str.toCharArray();
        for (int i=0;i< cArr.length;i++) {
            if (!Character.isDigit(cArr[i])) {
                return;
            }
        }

        int newLength = getText(0, getLength()).length() + str.length();
        if (newLength > maxDigits)
            return;
        super.insertString(offs, str, a);
    }
}

