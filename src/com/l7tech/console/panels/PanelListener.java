package com.l7tech.console.panels;

/**
 * The listener interface for receiving console Panel events. The class 
 * that is interested in processing an event implements this interface, 
 * and the object created with that class is registered with a Panel.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public interface PanelListener {
  /**
   * invoked after update
   * 
   * @param object an arbitrary object set by the Panel
   */
  void onUpdate(Object object);
  /**
   * invoked after insert
   * 
   * @param object an arbitrary object set by the Panel
   */
  void onInsert(Object object);
  /**
   * invoked on error
   * 
   * @param object an arbitrary object set by the Panel
   */
  void onError(Object object);
  /**
   * invoked on delete
   * 
   * @param object an arbitrary object set by the Panel
   */
  void onDelete(Object object);
}
