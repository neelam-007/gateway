package com.l7tech.util;

import java.util.EventObject;

/**
 * An event that contains information from a {@link ThreadPool}.
 * ThreadPoolEvents can be received by implementing a
 * {@link ThreadPoolListener}.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a> 
 */
public class ThreadPoolEvent extends EventObject {
  private Thread mThread;

  public ThreadPoolEvent(ThreadPool source, Thread thread) {
    super(source);
    mThread = thread;
  }

  public ThreadPool getThreadPool() {
    return (ThreadPool) getSource();
  }

  public Thread getThread() {
    return mThread;
  }
}
