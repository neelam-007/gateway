/*
 * $Header$
 */
package com.l7tech.console.text;

import javax.swing.text.AttributeSet;
import javax.swing.text.PlainDocument;
import javax.swing.text.BadLocationException;


/**
 * <CODE>FilterDocument</CODE> is the implementation of
 * <CODE>PlainDocument</CODE> accepting fields up to the
 * length defined by the 'max' parameter passed in constructor.
 * with optional filtering support.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version $Revision$, $Date$
 * @see javax.swing.text.Document
 */
public class FilterDocument extends PlainDocument {
  private int max;
  private Filter filter;

  /**
   * Constructor accepting the maximum document length and
   * the optional filter.
   * 
   * @param max    int specifying the maximum length of the content
   * @param filter optional user supplied filter for content
   */
  public FilterDocument(int max, Filter filter) {
    this.max = max;
    this.filter = filter;
  }

  /**
   * Inserts some content into the document. The max length
   * is verified and then it is run through the filter to 
   * verify wheter the content is accepted.
   * 
   * @param offs   the starting offset >= 0
   * @param str    the string to insert; does nothing with null/empty strings
   * @param a      the attributes for the inserted content
   * @exception BadLocationException
   *                   the given insert position is not a valid
   *                   position within the document
   * @see Document#insertString
   */
  public void insertString(int offs, String str, AttributeSet a)
  throws BadLocationException
  {
    if (str == null) return;
    if (getLength() + str.length() > max) return;
    if (null !=filter && !filter.accept(str)) return;

    super.insertString(offs, str, a);
  }

  /**
   * Filter, once implemented, can be set on a 
   * FilterDocument to keep unwanted chars from the 
   * underlying <CODE>Document</CODE>.
   */
  public static interface Filter {
    boolean accept(String s);
  }
}

