/*
 * $Header$
 */
package com.l7tech.console.panels;

/**
 * An abstract adapter class for receiving Panel events. The methods
 * in this class are empty. This class exists as convenience for creating
 * PanelListener objects.
 * 
 * Extend this class to create a Panel listener and override the methods
 * for the events of interest.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.1
 * @see PanelListener
 */
abstract public class PanelListenerAdapter implements PanelListener {
  /**
   * invoked after insert
   *
   * @param object an arbitrary object set by the Panel
   */
  public void onInsert(Object object) {}
  /**
   * invoked on error
   *
   * @param object an arbitrary object set by the Panel
   */
  public void onError(Object object) {}
  /**
   * invoked after update
   *
   * @param object an arbitrary object set by the Panel
   */
  public void onUpdate(Object object) {}

  /**
   * invoked on object delete
   *
   * @param object an arbitrary object set by the Panel
   */
  public void onDelete(Object object) {}
}
