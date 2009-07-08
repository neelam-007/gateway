package com.l7tech.test;

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;
import EDU.oswego.cs.dl.util.concurrent.Rendezvous;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>BenchmarkRunner</code> is simple bechmark tool, that accepts
 * the <code>Runnable</code> and executes it specified number of times.
 * The <code>Runnable</code> executions are distributed over the number
 * of threads up to {@link BenchmarkRunner#REQUESTS_PER_THREAD}.
 * <p/>
 * The 'Barrier' synchronization model to ensure all the test threads
 * are started at the same time.
 * <p/>
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class BenchmarkRunner {
    Logger log = Logger.getLogger(BenchmarkRunner.class.getName());

    /** A maximum number of threads that the test uses */
    public static final int MAX_THREAD_COUNT = 500;
    /** an arbitray number of requests per thread that the test uses */
    public static final int REQUESTS_PER_THREAD = 15;

    private final Set runnerResults = new HashSet();

    private Rendezvous rendezvous;
    private int threadCount;
    private Thread[] threads;
    private Runnable runnable;
    private int runCount;
    private boolean running;

    /** @noinspection FieldCanBeLocal silly idea bugs */
    private volatile boolean allThreadsCompletedNormally = false;

    /**
     * Test constructor
     */
    public BenchmarkRunner(Runnable r, int times, String name) {
        this(r, times, 0, name);
    }

    /**
     * Test constructor
     */
    public BenchmarkRunner(Runnable r, int times, int threadCount, String name) {
        this.runnable = r;
        this.runCount = times;
        this.name = name;
        this.threadCount = threadCount;
    }

    public BenchmarkRunner(Runnable r, int times) {
        this(r,times,r.toString());
    }

    /**
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * @return the number of threads that the test will use
     */
    public int getThreadCount() {
        return threadCount;
    }

    /**
     * Set the number of threads the test will use
     * @param threadCount
     */
    public void setThreadCount(int threadCount) {
        if (threadCount <=0 || threadCount > MAX_THREAD_COUNT) {
            throw new IllegalArgumentException();
        }
        if (running) {
            throw new IllegalStateException("already running");
        }
        this.threadCount = threadCount;
    }

    private void killAllThreads() {
        if (threads == null || threads.length < 1) return;
        for (int i = 0; i < threads.length; i++) {
            Thread thread = threads[i];
            if (thread != null) {
                threads[i] = null;
                thread.interrupt();
            }
        }
    }

    public void stopAll() {
        killAllThreads();
        synchronized (this) {
            this.notifyAll();
        }
    }

    /**
     * crete the test, and invoke the rendezvous (Barrier) that will
     * release waiting threads.
     *
     * @throws InterruptedException
     */
    public final void run() throws InterruptedException {
        // Make sure any old threads get killed
        killAllThreads();
        allThreadsCompletedNormally = false;
        running = true;
        synchronized (runnerResults) {
            runnerResults.clear();
        }

        try {
            prepareRunnables();
            // last rendezvois signals the release
            rendezvous.rendezvous(new Object());

            // Wait for all test threads to die
            for (int i = 0; i < threads.length; i++) {
                Thread thread = threads[i];
                thread.join();
            }

        } finally {
            killAllThreads();
            running = false;
        }

        if (allThreadsCompletedNormally) {
            log.info( name + ": all results collected.");
        } else {
            log.severe( name = ": WARNING: at least one thread died before finishing: results are incomplete");
        }
        printResults(allThreadsCompletedNormally);

    }

    /**
     * prepare the test.  Instantiates and starts every thread, waiting only for run to unblock them.
     */
    private void prepareRunnables() {
        if (runCount <= 0) {
            log.warning( name +": Invalid runnable count "+runCount);
            return;
        }

        int runnableCount = runCount;
        // if not set
        if (threadCount == 0) {
            threadCount = estimateThreads(runnableCount);
            if (threadCount < 1) threadCount = 1;
        }

        if (threadCount < 1) throw new IllegalStateException("Invalid thread count: " + threadCount);
        int perThread = runCount / threadCount;
        int remainder = runCount % threadCount;

        log.info( name + ": Runnables = " + runnableCount + " threads to use = " + threadCount + "  (about " + perThread + " iterations per thread)");

        rendezvous = new Rendezvous(threadCount + 1);
        // Make sure any old threads get killed
        killAllThreads();
        this.threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; ++i) {
            int num = perThread;
            if (remainder > 0) {
                // Distribute any remainder among the first few threads
                num++;
                remainder--;
            }
            ThreadRunner arun = new ThreadRunner(rendezvous, num, runnable);
            threads[i] = new Thread(arun);
            threads[i].start();
        }
    }


    /**
     * Estimate the number of requests per thread.
     * Start with MAX_THREAD_COUNT threads and decrease the number of threads
     * until there are less then REQUESTS_PER_THREAD clients per thread.
     *
     * @param requests the number of runnables to process
     * @return the estimated number of threads that will be used
     */
    private int estimateThreads(int requests) {
        if (requests <= 0) {
            throw new IllegalArgumentException("number of runnables must be positive " + requests);
        }
        int nThreads = MAX_THREAD_COUNT;
        while (nThreads > 1 && (requests / nThreads) < REQUESTS_PER_THREAD) {
            nThreads--;
        }
        return nThreads;
    }


    /**
     * collect authentication results for the current thread.
     *
     * @param result the results that were produced by the thread
     *               reporting the results.
     */
    private void collectResults(ThreadRunner.Result result) {
        int currentSize;
        synchronized (runnerResults) {
            runnerResults.add(result);
            currentSize = runnerResults.size();
        }
        if (currentSize == threadCount) {
            synchronized (this) {
                allThreadsCompletedNormally = true;
                this.notifyAll();
            }
        }
    }

    private void printResults(boolean allResultsReady) {
        if (allResultsReady)
            log.info( name + ": all results collected.");
        else
            log.severe( name + ": SEVERE: at least one thread died before finishing: results are probably incomplete");

        long start = Long.MAX_VALUE - 1;
        long end = 0;

        Iterator it = runnerResults.iterator();
        while (it.hasNext()) {
            ThreadRunner.Result res = (ThreadRunner.Result)it.next();
            //log.info("thread = " + res.thread + " time " + (res.end - res.start));
            if (end < res.end) end = res.end;
            if (start > res.start) start = res.start;
        }
        long t = (end - start);
        if (t == 0) t = 1;
        log.info( name + ": total time = " + t + "ms (" + runCount / (t / 1000f) + "/s)" );
    }


    /**
     * ThreadRunner, the class runs several test runnables
     */
    class ThreadRunner implements Runnable {
        /**
         * the class contains the results for one ThreadRunner
         */
        class Result {
            long start;
            long end;

            public Result(long start, long end) {
                this.start = start;
                this.end = end;
            }
        }

        /** barrier */
        private final Rendezvous rzvs;
        private final Runnable runnable;
        private final int iterations;

        /**
         * constructor
         *
         * @param rzvs rendezvous 'Barrier' synchronization variation
         * @param iterations number of times to repeat the work
         * @param runnable  the work to do
         */
        public ThreadRunner(Rendezvous rzvs, int iterations, Runnable runnable) {
            this.rzvs = rzvs;
            this.iterations = iterations;
            this.runnable = runnable;
        }

        /**
         * runs the queued requests
         */
        public void run() {
            try {
                rzvs.rendezvous(new Object()); // join barrier

                final long start = System.currentTimeMillis();
                for (int i = 0; i < iterations; ++i)
                    runnable.run();
                final long end = System.currentTimeMillis();

                collectResults(new Result(start, end));

            } catch (InterruptedException e) {
                log.severe( name + ": thread interrupted");
                Thread.currentThread().interrupt();
            } catch (BrokenBarrierException e) {
                log.log(Level.SEVERE, name + ": barrier synchronziation exception", e);
            } catch (Throwable t) {
                log.log(Level.SEVERE, name + ": SEVERE: uncaught exception", t);
            }
        }

    }

    private String name;
}

