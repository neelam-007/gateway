package com.l7tech.skunkworks;

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;
import EDU.oswego.cs.dl.util.concurrent.Rendezvous;
import com.l7tech.common.util.ThreadPool;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>BenchmarkRunner</code> is simple bechmark tool, that accepts
 * the <code>Runnable</code> and executes it specified number of times.
 * The <code>Runnable</code> executions are distributed over the number
 * of threads up to {@link BenchmarkRunner.REQUESTS_PER_THREAD}.
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

    private Rendezvous rendezvous;
    private int threadCount;
    private Set runnerResults = new HashSet();
    private final ThreadPool threadPool = new ThreadPool("test", MAX_THREAD_COUNT);
    private Runnable runnable;
    private int runCount;
    private boolean running;

    /**
     * Test constructor
     */
    public BenchmarkRunner(Runnable r, int times, String name) {
        this.runnable = r;
        this.runCount = times;
        this.name = name;

        // block indefinitely for an available thread
        // and threads never exit
        threadPool.setTimeout(-1);
        threadPool.setIdleTimeout(-1);
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
            throw new IllegalStateException();
        }
        this.threadCount = threadCount;
    }

    /**
     * crete the test, and invoke the rendezvous (Barrier) that will
     * release waiting threads.
     * 
     * @throws InterruptedException
     */
    public final void run() throws InterruptedException {
        running = true;

        try {
            prepareRunnables();
            // last rendezvois signals the release
            rendezvous.rendezvous(new Object());
            synchronized (this) {
                try {
                    this.wait();
                    threadPool.close(10000);
                } catch (InterruptedException e) {
                    // swallow
                }
            }
        } finally {
            running = false;
        }
    }

    /**
     * prepare the agents authenticaiton test. Load the agents, and
     * distribute the agents auth request to threads.
     * 
     * @throws InterruptedException
     */
    private void prepareRunnables() throws InterruptedException {
        if (runCount <= 0) {
            log.warning( name +": Invalid runnable count "+runCount);
            return;
        }

        int runnableCount = runCount;
        int adjustedThreads = threadCount;
        // if not set
        if (threadCount == 0) {
            threadCount = estimateThreads(runnableCount);
            // adjust if reminder
            int r = (runnableCount % threadCount) == 0 ? 0 : 1;
            adjustedThreads = threadCount + r;
        }

        log.info( name + ": Runnables = " + runnableCount + " threads to use = " + adjustedThreads);

        rendezvous = new Rendezvous(adjustedThreads + 1);

        ThreadRunner arun = null;

        for (int i = 0; i < runnableCount; i++) {
            if ((i % (runnableCount / threadCount)) == 0) {
                arun = new ThreadRunner(rendezvous);
                threadPool.start(arun);
            }
            arun.addRunnable(runnable);
        }
    }


    /**
     * Estimate the number of requests per thread.
     * Start with MAX_THREAD_COUNT threads and decrease the number of threads
     * until there are less then REQUESTS_PER_THREAD clients per thread.
     *
     * @param requests the number of agents to process
     * @return the estimated number of threads that will process the
     *         auth request.
     */
    private int estimateThreads(int requests) {
        if (requests <= 0) {
            throw new IllegalArgumentException("number of agents " + requests);
        }
        int nThreads = MAX_THREAD_COUNT;
        while ((requests / nThreads) < REQUESTS_PER_THREAD &&
          nThreads > 0) {
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
            log.info( name + ": all results collected.");

            long start = Long.MAX_VALUE - 1;
            long end = 0;

            Iterator it = runnerResults.iterator();
            while (it.hasNext()) {
                ThreadRunner.Result res = (ThreadRunner.Result)it.next();
                //log.info("thread = " + res.thread + " time " + (res.end - res.start));
                if (end < res.end) end = res.end;
                if (start > res.start) start = res.start;
            }
            log.info( name + ": total time = " + (end - start) + "ms");
            // notify done
            synchronized (this) {
                this.notify();
            }
        }
    }


    /**
     * ThreadRunner, the class runs several test runnables
     */
    class ThreadRunner implements Runnable {
        /**
         * the class contains the results for one ThreadRunner
         */
        class Result {
            private Thread thread;
            long start;
            long end;

            public Result(long start, long end, Thread th) {
                this.start = start;
                this.end = end;
                this.thread = th;
            }
        }

        /** barrier */
        private Rendezvous rzvs;
        /** the request queue */
        private List runnables = new ArrayList();

        private int requestsSize = 0;
        private volatile int completed = 0;
        private long start;
        private long end;
        private Thread th; // thread on which requests are executing

        /**
         * constructor
         * 
         * @param rzvs rendezvous 'Barrier' synchronization variation
         */
        public ThreadRunner(Rendezvous rzvs) {
            this.rzvs = rzvs;
        }

        /**
         * add the runnable to the runner
         * 
         * @param r the runnable
         */
        public void addRunnable(Runnable r) {
            runnables.add(r);
        }

        /**
         * runs the queued requests
         */
        public void run() {
            try {
                th = Thread.currentThread();

                rzvs.rendezvous(new Object()); // join barrier

                requestsSize = runnables.size();
                start = System.currentTimeMillis();

                Iterator it = runnables.iterator();
                while (it.hasNext()) {
                    Runnable r = (Runnable)it.next();
                    r.run();
                    completed++;
                }
                submitResults();
            } catch (InterruptedException e) {
                log.severe( name + ": thread interrupted");
                Thread.currentThread().interrupt();
            } catch (BrokenBarrierException e) {
                log.log(Level.SEVERE, name + ": barrier synchronziation exception", e);
            }
        }

        /**
         * submit the results for this AuthLogonCompletionHandlerImpl
         */
        private void submitResults() {
            end = System.currentTimeMillis();
            collectResults(new Result(start, end, th));
        }
    }

    private String name;
}

