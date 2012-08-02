package com.l7tech.gui.util;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the 3rd version of SwingWorker (also known as
 * SwingWorker 3), an abstract class that you subclass to
 * perform GUI-related work in a dedicated thread.  For
 * instructions on using this class, see:
 * 
 * http://java.sun.com/docs/books/tutorial/uiswing/misc/threads.html
 *
 * Note that the API changed slightly in the 3rd version:
 * You must now invoke start() on the SwingWorker after
 * creating it.
 */
public abstract class SwingWorker {
  private static final Logger logger = Logger.getLogger(SwingWorker.class.getName());

  private static final int POOL_CORE_SIZE = SyspropUtil.getInteger("com.l7tech.gui.util.SwingWorker.threadPool.coreSize", 5);
  private static final int POOL_MAX_SIZE = SyspropUtil.getInteger("com.l7tech.gui.util.SwingWorker.threadPool.maxSize",50);
  private static final int POOL_KEEP_ALIVE_MILLIS = SyspropUtil.getInteger("com.l7tech.gui.util.SwingWorker.threadPool.keepAliveMillis",1000);

  private static final BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(100);
  private static final ThreadFactory threadFactory = new ThreadFactory() {
      final ThreadFactory dflt = Executors.defaultThreadFactory();

      @Override
      public Thread newThread(Runnable r) {
          Thread t = dflt.newThread(r);
          t.setName("SwingWorker-" + t.getName());
          return t;
      }
  };
  private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(POOL_CORE_SIZE, POOL_MAX_SIZE, POOL_KEEP_ALIVE_MILLIS, TimeUnit.MILLISECONDS, workQueue, threadFactory, new ThreadPoolExecutor.CallerRunsPolicy());

  private final AtomicReference<Object> value = new AtomicReference<Object>();

  private final AtomicReference<Future<Object>> valueFuture = new AtomicReference<Future<Object>>();

  private final Runnable doFinished = new Runnable() {
    public void run() {
      finished();
    }
  };

  private final Runnable doConstruct;

    /**
   * Get the value produced by the worker thread, or null if it 
   * hasn't been constructed yet.
   */
  protected Object getValue() {
    return value.get();
  }

  /**
   * Set the value produced by worker thread
   */
  private void setValue(Object x) {
    value.set(x);
  }

  /** 
   * Compute the value to be returned by the <code>get</code> method. 
   */
  public abstract Object construct();

  /**
   * Called on the event dispatching thread (not on the worker thread)
   * after the <code>construct</code> method has returned.
   */
  public void finished() {
  }

  /**
   * A new method that interrupts the worker thread.  Call this method
   * to force the worker to stop what it's doing.
   */
  public void interrupt() {
    Future<Object> future = valueFuture.getAndSet(null);
    if (future != null) {
      future.cancel(true);
    }
  }

  /**
   * Return the value created by the <code>construct</code> method.  
   * Returns null if either the constructing thread or the current
   * thread was interrupted before a value was produced.
   * 
   * @return the value created by the <code>construct</code> method
   */
  public Object get() {

    while (true) {
      Future<Object> future = valueFuture.get();
      if (future == null) {
        return getValue();
      }
      try {
        future.get();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt(); // propagate
        return null;
      } catch (ExecutionException e) {
        logger.log(Level.FINE, "Exception in SwingWorker task: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        return null;
      }
    }
  }


  /**
   * Start a thread that will call the <code>construct</code> method
   * and then exit.
   */
  public SwingWorker() {
      doConstruct = new Runnable() {
          public void run() {
              try {
                  setValue(construct());
              } catch(RuntimeException exception) {
                  String handlerClass = SyspropUtil.getProperty("sun.awt.exception.handler");
                  if (handlerClass != null) {
                      handleException(handlerClass, exception);
                  }
                  else {
                      throw exception;
                  }
              } finally {
                  valueFuture.set(null);
              }

              SwingUtilities.invokeLater(doFinished);
          }
      };

  }

  /**
   * Start the worker thread.
   */
  public void start() {
      valueFuture.set((Future<Object>)executor.submit(doConstruct));
  }

  protected boolean isAlive() {
    return valueFuture.get() != null;
  }

    private void handleException(String handerClassName, RuntimeException exception) {
        Object handler = null;
        Method method = null;
        try {
            handler = Class.forName(handerClassName).newInstance();
            method = handler.getClass().getMethod("handle", new Class[]{Throwable.class});
        }
        catch(Exception e) {
            logger.log(Level.WARNING, "Error getting error handler", e);
            throw exception;
        }

        if (method != null) {
            try {
                method.invoke(handler, new Object[]{exception});
            }
            catch (IllegalAccessException iae) {
                logger.log(Level.WARNING, "Error calling error handler", iae);
                throw exception;
            }
            catch (InvocationTargetException ite) {
                logger.log(Level.WARNING, "Error calling error handler", ite);
                // we invoked the handler so don't throw exception here
            }
        }
    }
}
