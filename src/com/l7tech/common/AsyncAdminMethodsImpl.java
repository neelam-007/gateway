package com.l7tech.common;

import com.l7tech.common.util.Background;
import com.l7tech.common.util.Closeable;
import com.l7tech.common.util.ExceptionUtils;

import java.rmi.RemoteException;
import java.io.Serializable;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.SecureRandom;

/**
 * A utility implementation of {@link AsyncAdminMethods}, useful either as a base class or as a delegate.
 * This implementation is threadsafe (fully synchronized).
 */
public class AsyncAdminMethodsImpl implements AsyncAdminMethods, Closeable {
    protected static final Logger logger = Logger.getLogger(AsyncAdminMethodsImpl.class.getName());

    private final long staleJobMillis;

    private static class JobEntry<OUT extends Serializable> {
        private final JobId id;
        private final Future<OUT> future;
        private boolean done = false;
        private OUT result;
        private Throwable resultThrowable;
        private long timeCompleted;

        public JobEntry(JobId id, Future<OUT> future) {
            this.id = id;
            this.future = future;
        }
    }

    private final Random random = new SecureRandom();
    private Map<JobId, JobEntry> jobs = new HashMap<JobId, JobEntry>();
    private TimerTask cleanupTask;

    /**
     * Create an AsyncAdminMethodsImpl that will discard unclaimed inactive jobs after staleJobMillis milliseconds.
     *
     * @param staleJobSeconds number of seconds to hold unclaimed results of an inactive job before discarding
     *                       them.  Must be at least 30.
     */
    public AsyncAdminMethodsImpl(int staleJobSeconds) {
        if (staleJobSeconds < 30) throw new IllegalArgumentException("staleJobSeconds must be at least 30");
        this.staleJobMillis = staleJobSeconds * 1000L;
        cleanupTask = new TimerTask() {
            public void run() {
                synchronized (AsyncAdminMethodsImpl.this) {
                    long now = System.currentTimeMillis();
                    Collection<JobEntry> entries = jobs.values();
                    for (JobEntry entry : entries) {
                        long finished = entry.timeCompleted;
                        if (entry.done && (now - finished) > staleJobMillis) {
                            logger.log(Level.WARNING, "Discarding unclaimed asynchronous job result: " + entry.id);
                            jobs.remove(entry.id);
                        }
                    }
                }
            }
        };
        Background.scheduleRepeated(cleanupTask, 6521, 7919);
    }

    /**
     * Register a job entry to track the result of the specified Future.
     *
     * @param future  a Future result to track with an async JobId.  Required.
     * @param resultClass  the result type the Future will produce.  Required.
     * @return a new JobId that can be used to track the job.  Never null.
     */
    public synchronized <OUT extends Serializable> JobId<OUT> registerJob(Future<OUT> future, Class<OUT> resultClass) {
        mustNotBeClosed();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        JobId<OUT> jobId = new JobId<OUT>(bytes, resultClass);
        JobEntry<OUT> jobEntry = new JobEntry<OUT>(jobId, future);
        jobs.put(jobId, jobEntry);
        return jobId;
    }

    public synchronized <OUT extends Serializable> String getJobStatus(JobId<OUT> jobId) throws RemoteException
    {
        mustNotBeClosed();
        JobEntry entry = jobs.get(jobId);
        if (entry == null)
            return null;

        //noinspection unchecked
        Future<OUT> future = entry.future;
        if (future.isDone()) {
            entry.done = true;
            if (future.isCancelled())
                return "inactive:cancelled:Future.isCancelled()";
            entry.timeCompleted = System.currentTimeMillis();
            try {
                entry.result = future.get();
                return "inactive:completed:";
            } catch (InterruptedException e) {
                throw new RemoteException("Interrupted while trying to get job status", e); // shouldn't happen
            } catch (ExecutionException e) {
                Throwable t = e.getCause();
                if (t == null) t = e;
                entry.resultThrowable = t;
                return "inactive:failed:" + t.getClass().getName();
            }
        }

        return "active:pending:";
    }

    public synchronized <OUT extends Serializable> JobResult<OUT> getJobResult(JobId<OUT> jobId)
            throws RemoteException, UnknownJobException, JobStillActiveException {
        mustNotBeClosed();
        JobEntry entry = jobs.get(jobId);
        if (entry == null)
            throw new UnknownJobException("The specified job ID is unknown or its result has already been claimed");

        String status = getJobStatus(jobId);
        if (!entry.done)
            throw new JobStillActiveException("The specified job is still active");

        final OUT result;
        final Throwable t;
        if (jobId.getResultClassname().equals(entry.id.getResultClassname())) {
            //noinspection unchecked
            result = (OUT)entry.result;
            t = entry.resultThrowable;
            if (t != null && logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING, "Asynchronous job " + jobId + " threw an exception: " + ExceptionUtils.getMessage(t), t);
        } else {
            // Can't happen
            result = null;
            t = new ClassCastException("expected " + jobId.getResultClassname() + " found " + entry.id.getResultClassname());
        }

        String tclass = t == null ? null : t.getClass().getName();
        String tmess = t == null ? null : t.getMessage();
        return new JobResult<OUT>(status, result, tclass, tmess);
    }

    private void mustNotBeClosed() {
        if (jobs == null)
            throw new IllegalStateException(getClass().getName() + " has been closed");
    }

    /**
     * Free any resources used by this instance.
     * In particular, this cancels the cleanup timer task.
     * This instance may not be used once close() has been called.
     */
    public synchronized void close() {
        mustNotBeClosed();
        Background.cancel(cleanupTask);
        cleanupTask = null;
        jobs = null;
    }
}
