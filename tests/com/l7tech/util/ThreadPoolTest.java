package com.l7tech.util;

import junit.framework.*;

import java.util.logging.Logger;


/**
 * Thread pool tests.
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a> 
 */
public class ThreadPoolTest extends TestCase {
  private static ThreadPool pool = null;
  static Logger logger = Logger.getLogger(ThreadPoolTest.class.getName());

  private static final ThreadPoolListener
    tl = new ThreadPoolListener() {
      /**
       * Called when a new thread is started.
       */
      public void threadStarted(ThreadPoolEvent e) {
        // logger.debug("threadStarted() "+e.getThread());
      }

      /**
       * Called before a thread exits, usually because the idle timeout expired.
       */
      public void threadExiting(ThreadPoolEvent e) {
        // logger.debug("threadExiting() "+e.getThread());
      }
    };

  public ThreadPoolTest(String name) {
    super(name);
  }

  public static Test suite() {
    return new TestSuite(ThreadPoolTest.class);
  }

  /**
   * Test Pool that has been set to block indefinitely
   * when new thread is requested.
   *
   * @exception Exception
   */
  public void testSimpleBlockIndefinitely() throws Exception {
    pool = new ThreadPool("ThreadPool", 10);
    pool.addThreadPoolListener(tl);

    Runnable r =
      new Runnable() {
        public void run() {
          logger.info("Runnable " + Thread.currentThread());
        }
      };

    for (int i = 0; i < 10; i++) {
      pool.start(r);
    }

    pool.close();
    assertTrue("getAvailableCount() returns non zero on closed pool",
               pool.getAvailableCount() == 0);
    assertTrue("getPooledCount() returns non zero on closed pool",
               pool.getPooledCount() == 0);

    pool = new ThreadPool("ThreadPool", 3);
    pool.addThreadPoolListener(tl);
    for (int i = 0; i < 10; i++) {
      pool.start(r);
    }
    pool.close();
    assertTrue("getAvailableCount() returns non zero on closed pool",
               pool.getAvailableCount() == 0);
    assertTrue("getPooledCount() returns non zero on closed pool",
               pool.getPooledCount() == 0);

  }

  /**
   * test the blocking pool performance
   *
   * @exception Exception
   */
  public void testBlockingPoolPerformance() throws Exception {
    final Runnable r =
      new Runnable() {
        public void run() {
          ; // empty runnable
        }
      };

    ThreadPool pool = null;

    try {
      pool = new ThreadPool("ThreadPool", 5);
      StartStop ss = new StartStop();
      for (int i = 0; i < 1000; i++) {
        pool.start(r);
      }
      ss.print("1000 Runnable invocations on 5 thread queue");
      pool.close();

      pool = new ThreadPool("ThreadPool", 10);
      ss.reset();
      for (int i = 0; i < 1000; i++) {
        pool.start(r);
      }

      ss.print("1000 Runnable invocations on 10 thread queue");
      pool.close();

      pool = new ThreadPool("ThreadPool", 100);
      ss.reset();
      for (int i = 0; i < 1000; i++) {
        pool.start(r);
      }

      ss.print("1000 Runnable invocations on 100 thread queue");
      pool.close();

    } finally {
      if (pool != null) {
        pool.close();
      }
    }
  }

  /**
   * @exception Exception
   */
  public void testPoolWithTimeout() throws Exception {
    pool = new ThreadPool("ThreadPool", 6);
    pool.addThreadPoolListener(tl);
    pool.setTimeout(5000);

    Runnable r =
      new Runnable() {
        public void run() {
          try {
            synchronized (this) {
              this.wait(3000);
            }
          } catch (InterruptedException e) {
          }
        }
      };
    for (int i = 0; i < 10; i++) {
      pool.start(r);
    }
    pool.close();
    assertTrue("getAvailableCount() returns non zero on closed pool",
               pool.getAvailableCount() == 0);
    assertTrue("getPooledCount() returns non zero on closed pool",
               pool.getPooledCount() == 0);
  }

  /**
   * simple lap start/stop class
   */
  private static class StartStop {
    long timestart = System.currentTimeMillis();

    void print(String str) {
      logger.info((System.currentTimeMillis() - timestart) + "ms " + str);
    }

    void reset() {
      timestart = System.currentTimeMillis();
    }
  }


  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }

}
