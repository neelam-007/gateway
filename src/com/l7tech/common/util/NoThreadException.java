package com.l7tech.common.util;

/**
 * This exception is thrown by a {@link ThreadPool} when no thread is
 * available.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * 
 */
public class NoThreadException extends InterruptedException {
  private boolean mIsClosed;

  public NoThreadException(String message) {
    super(message);
  }

  public NoThreadException(String message, boolean isClosed) {
    super(message);
    mIsClosed = isClosed;
  }

  public boolean isThreadPoolClosed() {
    return mIsClosed;
  }
}
