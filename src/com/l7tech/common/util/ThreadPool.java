package com.l7tech.common.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A ThreadPool contains a collection of re-usable threads. Since
 * there is a performance overhead in creating new threads, ThreadPool
 * can improve performance in threaded applications.
 * Pooled threads operate on Runnable targets and return back to the
 * pool when the Runnable.run method exits.
 *
 * The pool operates in two distinct modes:
 * timeout mode - is when positive timeout and idletimoeut values in ms
 * have been given. Pool uses timeout value as a wait timeout for an
 * available thread when maximum number of threads has been reached.
 * The idletimoeut value is used by individual threads when checking
 * if there has been a new Runnable assigned (after the initial Runnable
 * has run that created the Thread). If no Runnable has been assigned
 * during the idletimout period the thread exits and removes itself
 * from the pool.
 *
 * no timeout (blocking mode) - is when negative timeout values in ms
 * have been given. Pool waits indefinitely for an available thread when
 * maximum number of pooled threads has been reached.
 *
 * Additionally pool supports <CODE>ThreadPoolListener</CODE> registrations
 * that receives pool events. The listener methods are invoked on the
 * thread that generated the event.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1 $
 *
 * @see ThreadPoolListener
 */
public class ThreadPool extends ThreadGroup {
  private static int cThreadID;

  private synchronized static int nextThreadID() {
    return cThreadID++;
  }

  // pool timeoout, idletimoeut values
  private long mTimeout = -1;
  private long mIdleTimeout = -1;

  // pool listeners
  private Collection mListeners = new LinkedList();

  private LinkedList mPool = new LinkedList();

  private int mMax;
  private volatile int mActive;
  private boolean mDaemon;
  private int mPriority;
  private volatile boolean mClosed;

  /**
   * Create a ThreadPool of daemon threads.
   *
   * @param name Name of ThreadPool
   * @param max The maximum allowed number of threads
   *
   * @throws IllegalArgumentException
   */
  public ThreadPool(String name, int max)
    throws IllegalArgumentException {

    this(name, max, true);
  }

  /**
   * Create a ThreadPool of daemon threads.
   *
   * @param parent Parent ThreadGroup
   * @param name Name of ThreadPool
   * @param max The maximum allowed number of threads
   *
   * @throws IllegalArgumentException
   */
  public ThreadPool(ThreadGroup parent, String name, int max)
    throws IllegalArgumentException {

    this(parent, name, max, true);
  }

  /**
   * Create a ThreadPool.
   *
   * @param name Name of ThreadPool
   * @param max The maximum allowed number of threads
   * @param daemon Set to true to create ThreadPool of daemon threads
   *
   * @throws IllegalArgumentException
   */
  public ThreadPool(String name, int max, boolean daemon)
    throws IllegalArgumentException {

    super(name);

    init(max, daemon);
  }

  /**
   * Create a ThreadPool (full constructor)
   *
   * @param parent Parent ThreadGroup
   * @param name   Name of ThreadPool
   * @param max    The maximum allowed number of threads
   * @param daemon Set to true to create ThreadPool of daemon threads
   * @exception IllegalArgumentException
   *                   thrown on illegal parameters
   *                   (max number of threads negative)
   */
  public ThreadPool(ThreadGroup parent, String name, int max, boolean daemon)
    throws IllegalArgumentException {

    super(parent, name);

    init(max, daemon);
  }

  private void init(int max, boolean daemon)
    throws IllegalArgumentException {

    if (max <= 0) {
      throw new IllegalArgumentException
        ("Maximum number of threads must be greater than zero: " +
         max);
    }

    mMax = max;

    mDaemon = daemon;
    mPriority = Thread.currentThread().getPriority();
    mClosed = false;
  }

  /**
   * Sets the timeout (in milliseconds) for getting threads from the pool
   * or for closing the pool. A negative value specifies an infinite timeout.
   * Calling the start method that accepts a timeout value will override
   * this setting.
   */
  public synchronized void setTimeout(long timeout) {
    mTimeout = timeout;
  }

  /**
   * Returns the timeout (in milliseconds) for getting threads from the pool.
   * The default value is negative, which indicates an infinite wait.
   */
  public synchronized long getTimeout() {
    return mTimeout;
  }

  /**
   * Sets the timeout (in milliseconds) for idle threads to exit. A negative
   * value specifies that an idle thread never exits.
   */
  public synchronized void setIdleTimeout(long timeout) {
    mIdleTimeout = timeout;
  }

  /**
   * Returns the idle timeout (in milliseconds) for threads to exit. The
   * default value is negative, which indicates that idle threads never exit.
   */
  public synchronized long getIdleTimeout() {
    return mIdleTimeout;
  }

  /**
   * adds and ThreadPoolListener to the pool.
   *
   * @param listener the ThreadPoolListener to be added.
   */
  public void addThreadPoolListener(ThreadPoolListener listener) {
    synchronized (mListeners) {
      mListeners.add(listener);
    }
  }

  /**
   * removes the given ThreadPoolListener from the pool listeners.
   *
   * @param listener the ThreadPoolListener to be removed.
   */
  public void removeThreadPoolListener(ThreadPoolListener listener) {
    synchronized (mListeners) {
      mListeners.remove(listener);
    }
  }

  /**
   * Returns the initial priority given to each thread in the pool. The
   * default value is that of the thread that created the ThreadPool.
   */
  public int getPriority() {
    synchronized (mPool) {
      return mPriority;
    }
  }

  /**
   * Sets the priority given to each thread in the pool.
   *
   * @throws IllegalArgumentException if priority is out of range
   */
  public void setPriority(int priority) throws IllegalArgumentException {
    if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
      throw new IllegalArgumentException
        ("Priority out of range: " + priority);
    }

    synchronized (mPool) {
      mPriority = priority;
    }
  }

  /**
   * @return The maximum allowed number of threads.
   */
  public int getMaximumAllowed() {
    synchronized (mPool) {
      return mMax;
    }
  }

  /**
   * @return The number of currently available threads in the pool.
   */
  public int getAvailableCount() {
    synchronized (mPool) {
      return mPool.size();
    }
  }

  /**
   * @return The total number of threads in the pool that are either
   * available or in use.
   */
  public int getPooledCount() {
    synchronized (mPool) {
      return mActive;
    }
  }

  /**
   *
   * @return The estimated total number of threads in the ThreadGroup.
   * @see ThreadGroup#activeCount()
   */
  public int getThreadCount() {
    return activeCount();
  }

  /**
   * @return threads active in the entire ThreadGroup.
   */
  public Thread[] getAllThreads() {
    int count = activeCount();
    Thread[] threads = new Thread[count];
    count = enumerate(threads);
    if (count >= threads.length) {
      // return sort(threads);
      return threads;
    } else {
      Thread[] newThreads = new Thread[count];
      System.arraycopy(threads, 0, newThreads, 0, count);
      return newThreads;
    }
  }

  /**
   * Waits for a Thread to become available and starts a Runnable in it.
   * If there are no available threads and the number of active threads is
   * less than the maximum allowed, then a newly created thread is returned.
   *
   * @param target The Runnable instance that gets started by the returned
   * thread.
   * @exception com.l7tech.common.util.NoThreadException If no thread could be obtained.
   * @exception InterruptedException If interrupted while waiting for a
   * thread to become available.
   * @return A Thread that has been started on the given Runnable.
   */
  public Thread start(Runnable target)
    throws NoThreadException, InterruptedException {
    return start0(target, getTimeout(), null);
  }

  /**
   * Waits for a Thread to become available and starts a Runnable in it.
   * If there are no available threads and the number of active threads is
   * less than the maximum allowed, then a newly created thread is returned.
   *
   * @param target  The Runnable instance that gets started by the returned
   *                thread.
   * @param timeout Milliseconds to wait for a thread to become
   *                available. If zero, don't wait at all. If negative, wait forever.
   * @return A Thread that has been started on the given Runnable.
   * @exception NoThreadException
   *                   If no thread could be obtained.
   * @exception InterruptedException
   *                   If interrupted while waiting for a
   *                   thread to become available.
   */
  public Thread start(Runnable target, long timeout)
    throws NoThreadException, InterruptedException {
    return start0(target, timeout, null);
  }


  /**
   * Waits for a Thread to become available and starts a Runnable in it.
   * If there are no available threads and the number of active threads is
   * less than the maximum allowed, then a newly created thread is returned.
   *
   * @param target The Runnable instance that gets started by the returned
   *               thread.
   * @param name   The name to give the thread.
   * @return A Thread that has been started on the given Runnable.
   * @exception NoThreadException
   *                   If no thread could be obtained.
   * @exception InterruptedException
   *                   If interrupted while waiting for a
   *                   thread to become available.
   */
  public Thread start(Runnable target, String name)
    throws NoThreadException, InterruptedException {
    return start0(target, getTimeout(), name);
  }

  /**
   * Waits for a Thread to become available and starts a Runnable in it.
   * If there are no available threads and the number of active threads is
   * less than the maximum allowed, then a newly created thread is returned.
   *
   * @param target  The Runnable instance that gets started by the returned
   *                thread.
   * @param timeout Milliseconds to wait for a thread to become available.
   *                If zero, don't wait at all. If negative, wait forever.
   *
   * @param name    The name to give the thread.
   * @return A Thread that has been started on the given Runnable.
   * @exception NoThreadException
   *                   If no thread could be obtained.
   * @exception InterruptedException
   *                   If interrupted while waiting for a
   *                   thread to become available.
   */
  public Thread start(Runnable target, long timeout, String name)
    throws NoThreadException, InterruptedException {
    return start0(target, timeout, name);
  }

  private Thread start0(Runnable target, long timeout, String name)
    throws NoThreadException, InterruptedException {
    PooledThread thread;

    while (true) {
      synchronized (mPool) {
        closeCheck();

        // Obtain a thread from the pool if non-empty.
        if (mPool.size() > 0) {
          thread = (PooledThread) mPool.removeLast();
        } else {
          // Create a new thread if the number of active threads
          // is less than the maximum allowed.
          if (mActive < mMax) {
            return startThread(target, name);
          } else {
            break;
          }
        }
      }

      if (name != null) {
        thread.setName(name);
      }

      if (thread.setTarget(target)) {
        return thread;
      }

      // Couldn't set the target because the pooled thread is exiting.
      // Wait for it to exit to ensure that the active count is less
      // than the maximum and try to obtain another thread.
      thread.join();
    }

    if (timeout == 0) {
      throw new NoThreadException("No thread available from " + this);
    }

    // Wait for a thread to become available in the pool.
    synchronized (mPool) {
      closeCheck();

      if (timeout < 0) {
        while (mPool.size() <= 0) {
          mPool.wait(0);
          closeCheck();
        }
      } else {
        long expireTime = System.currentTimeMillis() + timeout;
        while (mPool.size() <= 0) {
          mPool.wait(timeout);
          closeCheck();

          // Thread could have been notified, but another thread may
          // have stolen the thread away.
          if (mPool.size() <= 0 &&
            System.currentTimeMillis() > expireTime) {

            throw new NoThreadException
              ("No thread available after waiting " +
               timeout + " milliseconds: " + this);
          }
        }
      }

      thread = (PooledThread) mPool.removeLast();
      if (name != null) {
        thread.setName(name);
      }

      if (thread.setTarget(target)) {
        return thread;
      }
    }

    // Couldn't set the target because the pooled thread is exiting.
    // Wait for it to exit to ensure that the active count is less
    // than the maximum and create a new thread.
    thread.join();
    return startThread(target, name);
  }

  public boolean isClosed() {
    return mClosed;
  }

  /**
   * Will close down all the threads in the pool as they become
   * available. This method may block forever if any threads are
   * never returned to the thread pool.
   */
  public void close() throws InterruptedException {
    close(getTimeout());
  }

  /**
   * Will close down all the threads in the pool as they become
   * available. If all the threads cannot become available within
   * the specified timeout, any active threads not yet returned
   * to the thread pool are interrupted.
   * Special note for timeout negative (wait indefinitely)
   * If thread never becomes available, it is never returned to a
   * pool and this call blocks <B>indefinitely</B>.
   *
   * @param timeout Milliseconds to wait before unavailable threads
   *                are interrupted. If zero, don't wait at all. If negative,
   *                wait forever.
   */
  public void close(long timeout) {
    synchronized (mPool) {
      mClosed = true;
      mPool.notifyAll();

      if (timeout != 0) {
        if (timeout < 0) {
          while (mActive > 0) {
            // to allow available threads to exit gracefully
            // submit noop (null) Runnable to thos threads
            PooledThread[] threads =
              (PooledThread[]) mPool.toArray(new PooledThread[]{});
            for (int i = 0; i < threads.length; i++) {
              threads[i].setTarget(null);
            }
            try {
              mPool.wait(0);
            } catch (InterruptedException e) {
            }
          }
        } else {
          long expireTime = System.currentTimeMillis() + timeout;
          while (mActive > 0) {
            PooledThread[] threads =
              (PooledThread[]) mPool.toArray(new PooledThread[]{});
            for (int i = 0; i < threads.length; i++) {
              threads[i].setTarget(null);
            }
            try {
              mPool.wait(timeout);
            } catch (InterruptedException e) {
            }
            if (System.currentTimeMillis() > expireTime) {
              break;
            }
          }
        }
      }
    }
    interrupt();
  }

  /**
   * start a Runnable target on the thread, optionally assigning a
   * name to a thread.
   *
   * @param target the Runnable target
   * @param name   optional thread name, if not null
   * @return PooledThread instance that the Runnable will be invoked on
   */
  private PooledThread startThread(Runnable target, String name) {
    PooledThread thread;

    synchronized (mPool) {
      mActive++;

      thread = new PooledThread(getName() + ' ' + nextThreadID());
      thread.setPriority(mPriority);
      thread.setDaemon(mDaemon);

      if (name != null) {
        thread.setName(name);
      }

      thread.setTarget(target);
      thread.start();
    }

    ThreadPoolEvent event = new ThreadPoolEvent(this, thread);
    synchronized (mListeners) {
      for (Iterator it = mListeners.iterator(); it.hasNext();) {
        ((ThreadPoolListener) it.next()).threadStarted(event);
      }
    }

    return thread;
  }

  private void closeCheck() throws NoThreadException {
    if (mClosed) {
      throw new NoThreadException("Thread pool is closed", true);
    }
  }

  /**
   * make the thread avaialbe in the pool. This is usually invoked
   * after the thread finsihed executing the assigned Runnable.
   *
   * @param thread PooledThread instance to return to the pool
   */
  void threadAvailable(PooledThread thread) {
    synchronized (mPool) {
      if (thread.getPriority() != mPriority) {
        thread.setPriority(mPriority);
      }
      mPool.addLast(thread);
      mPool.notify();
    }
  }

  /**
   * invoked when thread is exiting from the pool. Not that the
   * thread may have never been available in the pool when this
   * method is invoked. This happens when the first Runnable
   * throws the uncaught exception, or the pool has been closed
   * before joining the pool.
   *
   * @param thread Thread instance that is exiting from the pool
   */
  void threadExiting(PooledThread thread) {
    synchronized (mPool) {
      mActive--;
      mPool.remove(thread);
      ThreadPoolEvent event = new ThreadPoolEvent(this, thread);
      synchronized (mListeners) {
        for (Iterator it = mListeners.iterator(); it.hasNext();) {
          ((ThreadPoolListener) it.next()).threadExiting(event);
        }
      }
      mPool.notify();
    }
  }


  private void submitNoopToPooledThreads() {
    synchronized (mPool) {
      PooledThread[] threads =
        (PooledThread[]) mPool.toArray(new PooledThread[]{});
      for (int i = 0; i < threads.length; i++) {
        threads[i].setTarget(null);
      }
    }
  }

  /**
   * The <CODE>PooledThread</CODE> class is a class that represents
   * the thread in the pool. <code>Runnable</CODE> instances run on
   * the PooledThread.
   *
   * @see ThreadPool#start(Runnable)
   */
  private class PooledThread extends Thread {
    private String mOriginalName;
    private Runnable mTarget;
    private boolean mExiting;

    public PooledThread(String name) {
      super(ThreadPool.this, name);
      mOriginalName = name;
    }

    synchronized boolean setTarget(Runnable target) {
      if (mTarget != null) {
        throw new IllegalStateException
          ("Target runnable in pooled thread is already set");
      }

      if (mExiting) {
        return false;
      } else {
        mTarget = target;
        this.notify();
        return true;
      }
    }

    private synchronized Runnable waitForTarget() {
      Runnable target;
      long idle = getIdleTimeout();
      if ((target = mTarget) == null) {
        if (idle != 0) {
          try {
            if (idle < 0) {
              this.wait(0);
            } else {
              this.wait(idle);
            }
          } catch (InterruptedException e) {
          }
        }

        if ((target = mTarget) == null) {
          mExiting = true;
        }
      }
      return target;
    }

    public void run() {
      try {
        while (!isClosed()) {
          if (Thread.interrupted()) {
            continue;
          }

          Runnable target;

          if ((target = waitForTarget()) == null) {
            break;
          }

          try {
            target.run();
          } catch (ThreadDeath e) {
            throw e;
          } catch (Throwable e) {
            uncaughtException(Thread.currentThread(), e);
            e = null;
          }
          target = null;
          mTarget = null;
          setName(mOriginalName);
          threadAvailable(this);
        }
      } finally {
        threadExiting(this);
      }
    }
  }
}
