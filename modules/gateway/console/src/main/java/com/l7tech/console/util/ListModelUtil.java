package com.l7tech.console.util;

import java.util.Arrays;
import java.util.List;
import javax.swing.ListModel;

/**
 * The class represents a bag of utilities that deal with 
 * <CODE>ListModel</CODE> instances.
 * The intent was to keep the <CODE>ListModel</CODE> interface
 * clean, and use this class to perform additional operation
 * that are not part of the original interface
 * @version 1.0
 */
public final class ListModelUtil {
  /**
   * private cosntructor, this class cannot 
   * be instantiated
   */
  private ListModelUtil() {
  }

  /**
   * Returns an array containing all of the elements in the
   * ListModel in the correct order.
   * 
   * @param model  the ListModel
   * @return the Object array continaing elements from ListModel
   * @exception NullPointerException
   *                   thrown if null ListModel passed
   */
  public static Object[] toArray(ListModel model) 
    throws NullPointerException {
    if (model == null) {
      throw new NullPointerException("model == null");
    }
    Object[] oa = new Object[model.getSize()];
    int size = model.getSize();
    for (int i=0; i< size; i++) {
      oa[i] = model.getElementAt(i);
    }
    return oa;
  }

  /**
   * Returns an List containing all of the elements in the
   * ListModel in the correct order.
   * 
   * @param model  the ListModel
   * @return the <code>List</code> continaing elements from 
   * ListModel
   * @exception NullPointerException
   *                   thrown if null ListModel passed
   */
  public static List asList(ListModel model) {
    return Arrays.asList(toArray(model));
  }
}
