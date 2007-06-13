package com.l7tech.common;

import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.util.HexUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.rmi.RemoteException;

/**
 * Methods implemented by remote admin beans that support async method execution.
 */
public interface AsyncAdminMethods {
    /**
     * Check the status of a remote job.  The return value is a status String with three fields, arranged
     * from general to specific and joined by a colon.
     * <p>
     * The first field is always present and nonempty and is either "active" or "inactive".
     * The value "active" means no result is yet ready, so
     * a call to {@link #getJobResult} would block or return immediate failure (depending on the argument).
     * The value "inactive" means a JobResult is available for pickup from {@link #getJobResult}.
     * <p/>
     * The second field is always present and nonempty and is one of "rejected", "pending", "running", "paused",
     * "canceled", "completed", or "failed".
     * <p/>
     * The third field is always present but may be empty.  It contains additional information about the disposition
     * of the job.
     * <p/>
     * <pre>
     *     inactive:rejected:REASON    job was not admitted into work queue (ie, "inactive:rejected:Queue is full")
     *     active:pending:             job is enqueued and is waiting to be executed
     *     active:running:             job is currently executing
     *     active:paused:REASON        job is paused but may eventually finish
     *     inactive:canceled:REASON    job was canceled
     *     inactive:completed:         job completed successfully
     *     inactive:failed:REASON      job threw an exception (ie, "inactive:failed:java.io.FileNotFoundException")
     * </pre>
     * <p/>
     * Most callers can just test if the status string is null, starts with "i", or starts with "a".
     *
     * @param jobId the JobId of the job whose status to check.  Required.
     * @return a job status string in the above format, or null if the specified jobId is not recognized.
     * @throws java.rmi.RemoteException on remote communication error
     */
    <OUT extends Serializable> String getJobStatus(JobId<OUT> jobId) throws RemoteException;

    /**
     * Exception thrown if a JobId is unrecognized.
     */
    public static final class UnknownJobException extends Exception {
        private static final long serialVersionUID = 1234742529058273838L;
        public UnknownJobException() {}
        public UnknownJobException(String message) { super(message); }
        public UnknownJobException(String message, Throwable cause) { super(message, cause); }
        public UnknownJobException(Throwable cause) { super(cause); }
    }

    /**
     * Exception thrown if an attempt is made to pick up the result of a job that is still active.
     */
    public static final class JobStillActiveException extends Exception {
        private static final long serialVersionUID = 2462587258125876234L;
        public JobStillActiveException() {}
        public JobStillActiveException(String message) { super(message); }
        public JobStillActiveException(String message, Throwable cause) { super(message, cause); }
        public JobStillActiveException(Throwable cause) { super(cause); }
    }

    /**
     * Pick up the result of an asynchronous job.  As soon as a job's results are picked up the job's information
     * is discarded.
     *
     * @param jobId the ID of the job whose result to pick up.  Required.
     * @return the result of this inactive job.  Never null.
     * @throws java.rmi.RemoteException on remote communication error
     * @throws UnknownJobException if the specified jobId is unknown or has already been picked up.
     * @throws JobStillActiveException if the specified job is not inactive.
     */
    <OUT extends Serializable> JobResult<OUT> getJobResult(JobId<OUT> jobId) throws RemoteException, UnknownJobException, JobStillActiveException;

    /**
     * An asynchronous completion token representing a long-running job being performed on the server.
     */
    public static final class JobId<OUT extends Serializable> implements Serializable {
        private static final long serialVersionUID = 8292671249057242870L;
        private final byte[] bytes;
        private final String resultClassname;

        public JobId(byte[] bytes, Class<? extends OUT> resultClass) {
            if (bytes == null) throw new NullPointerException();
            this.bytes = bytes;
            this.resultClassname = resultClass.getName();
        }

        /** @return the classname of the result expected by this job. */
        public String getResultClassname() {
            return resultClassname;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TrustedCertAdmin.JobId jobId = (TrustedCertAdmin.JobId)o;
            return Arrays.equals(bytes, jobId.bytes);
        }

        public int hashCode() {
            return Arrays.hashCode(bytes);
        }

        public String toString() {
            return "JobId<" + resultClassname + ">:" + HexUtils.hexDump(bytes);
        }
    }

    /**
     * Holds the result of an inactive asynchronous job.
     * If throwableClassname is non-null, the job failed.  Otherwise it succeeded, and result holds the return
     * value from the job (which may be null).
     */
    public static final class JobResult<OUT extends Serializable> implements Serializable {
        private static final long serialVersionUID = 6452392466733202138L;

        /** Final status string, in same format as {@link com.l7tech.common.security.TrustedCertAdmin#getJobStatus}.  Always starts with "inactive:". */
        public final String status;

        /** If the job was successful, this holds the result (which may be null); otherwise it is always null. */
        public final OUT result;

        /** If the job was successful, this is always null.  Otherwise, this is the classname of the Throwable the job threw. */
        public final String throwableClassname;

        /** If throwableClassname is non-null, this is the message from the Throwable.  May be null. */
        public final String throwableMessage;

        public JobResult(String status, OUT result, String throwableClassname, String throwableMessage) {
            this.status = status;
            this.result = result;
            this.throwableClassname = throwableClassname;
            this.throwableMessage = throwableMessage;
        }
    }
}
