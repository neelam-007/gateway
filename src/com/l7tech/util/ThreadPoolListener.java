package com.l7tech.util;

import java.util.EventListener;

/**
 * Interface used to receive events from a {@link ThreadPool}.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a> 
 * 
 */
public interface ThreadPoolListener extends EventListener {
  /**
   * Called when a new thread is started.
   *
   * @param e      TreadPoolEvent instance with event details
   */
  public void threadStarted(ThreadPoolEvent e);

  /**
   * Called before a thread exits, usually because the idle
   * timeout expired, or pool closing.
   *
   * @param e      TreadPoolEvent instance with event details
   */
  public void threadExiting(ThreadPoolEvent e);
}
