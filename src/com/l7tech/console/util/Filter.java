package com.l7tech.console.util;

/**
 * The Filter interface. Implementations provide custom 
 * filtering logic.
 */
public interface Filter {
  /**
   * @param o  the <code>Object</code> to examine
   * @return  true if filter accepts the object, false otherwise
   */
  boolean accept(Object o);
}
