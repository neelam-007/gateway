package com.l7tech.console.text;

import javax.swing.text.AttributeSet;
import javax.swing.text.PlainDocument;
import javax.swing.text.BadLocationException;


/**
 * <CODE>MaxLengthDocument</CODE> is the implementation of
 * <CODE>PlainDocument</CODE> accepting fields up to the
 * length defined by the 'max' parameter passed in constructor.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 * @see javax.swing.text.Document
 * @see javax.swing.JTextField
 */
public class MaxLengthDocument extends PlainDocument {
  private int max;

  public MaxLengthDocument (int max) {
    this.max = max;
  }

  /**
   * Inserts some content into the document.
   *
   * @param offs the starting offset >= 0
   * @param str the string to insert; does nothing with null/empty strings
   * @param a the attributes for the inserted content
   * @exception BadLocationException  the given insert position is not a valid
   *   position within the document
   * @see javax.swing.text.Document#insertString
   */
  public void insertString(int offs, String str, AttributeSet a)
  throws BadLocationException
  {
    if (str != null) {
      if (getLength() + str.length() <= max) {
        super.insertString(offs, str, a);
      }
    }
  }
}

