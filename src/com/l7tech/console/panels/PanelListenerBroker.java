/*
 * $Header$
 */
package com.l7tech.console.panels;

/**
 * A broker class for receiving and distributing editor panel 
 * events. 
 * The class register itself with panels, and when the event
 * is received it dispatches the events to the listeners that
 * registered with the broker.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.1
 * @see PanelListener
 */
public class PanelListenerBroker implements PanelListener {
  /**
   * invoked after insert
   *
   * @param object an arbitrary object set by the Panel
   */
  public void onInsert(Object object) {
    for (int i = listeners.length-1; i>=0; --i) {
      listeners[i].onInsert(object);
    }
  }

  /**
   * invoked on error
   *
   * @param object an arbitrary object set by the Panel
   */
  public void onError(Object object) {
    for (int i = listeners.length-1; i>=0; --i) {
      listeners[i].onError(object);
    }
  }

  /**
   * invoked after update
   *
   * @param object an arbitrary object set by the Panel
   */
  public void onUpdate(Object object) {
    for (int i = listeners.length-1; i>=0; --i) {
      listeners[i].onUpdate(object);
    }
  }

  /**
   * invoked after delete
   *
   * @param object an arbitrary object set by the Panel
   */
  public void onDelete(Object object) {
    for (int i = listeners.length-1; i>=0; --i) {
      listeners[i].onDelete(object);
    }
  }

  /**
   * registers new <CODE>PanelListener</CODE> with the broker
   * 
   * @param l      the new panel listener
   */
  public void addPanelListener(PanelListener l) {
    int i = listeners.length;
    PanelListener[] tmp = new PanelListener[i+1];
    System.arraycopy(listeners, 0, tmp, 0, i);
    tmp[i] = l;
    listeners = tmp;
  }
  /**
   * de registers the <CODE>PanelListener</CODE>
   * 
   * @param l      the PanelListener instance
   */
  public void removePanelListener(PanelListener l) {
    // Is l on the list?
    int index = -1;
    for (int i = listeners.length-1; i>=0; --i) {
      if (listeners[i].equals(l)) {
        index = i;
        break;
      }
    }

    // If so,  remove it
    if (index != -1) {
      PanelListener[] tmp = 
      new PanelListener[listeners.length-1];
      // Copy the list up to index
      System.arraycopy(listeners, 0, tmp, 0, index);
      // Copy from one past the index, up to
      // the end of tmp (which is one element shorter than the old list)
      if (index < tmp.length)
        System.arraycopy(listeners, index+1, tmp, index, tmp.length - index);
      // set the listener array to the new array or null
      listeners = (tmp.length == 0) ? EMPTY_ARRAY : tmp;
    }

  }

  private 
  PanelListener[] listeners = EMPTY_ARRAY;

  private static final 
  PanelListener[] EMPTY_ARRAY = new PanelListener[0];
}
