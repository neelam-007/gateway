package com.l7tech.util;

import java.util.EventObject;

/**
 * An event that contains information from a {@link ThreadPool}.
 * ThreadPoolEvents can be received by implementing a
 * {@link ThreadPoolListener}.
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
