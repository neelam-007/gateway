package com.l7tech.console.text;

import java.awt.Toolkit;

import javax.swing.text.AttributeSet;
import javax.swing.text.PlainDocument;
import javax.swing.text.BadLocationException;


/**
 * <CODE>IntegerField</CODE> is the implementation of
 * <CODE>PlainDocument</CODE> accepting integers fields only.
 * 
 * The instance may be creted by specifiyng the min and max
 * values, thus limiting the integer range.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 * @see javax.swing.text.Document
 * @see javax.swing.JTextField
 */
public class IntegerField extends PlainDocument {
  private int minInt;
  private int maxInt;

  public IntegerField() {
    minInt = Integer.MIN_VALUE;
    maxInt = Integer.MAX_VALUE;
  }

  public IntegerField(int min, int max) {
    // sanity check
    if (!(min < max)) {
      throw new IllegalArgumentException("min < max");
    }
    minInt = min;
    maxInt = max;
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
    if (str == null) return;

    char[] cArr = str.toCharArray();
    for (int i=0;i< cArr.length;i++) {
      if (!Character.isDigit(cArr[i])) {
        return;
      }
    }
    try {
      String s = getText(0, getLength())+str;
      int newInt = Integer.parseInt(s);
      if (newInt >=minInt && newInt <= maxInt) {
        super.insertString(offs, str, a);
      } else {
      }
    } catch(NumberFormatException e) {
    }
  }
}

