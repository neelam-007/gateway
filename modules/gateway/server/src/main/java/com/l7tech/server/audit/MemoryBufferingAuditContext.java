package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditDetailEvent;
import com.l7tech.gateway.common.audit.AuditRecord;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * An audit context that buffers all audit events in memory (sorted into streams based on the thread ID)
 * and can reply them on demand via a specified audit context.
 * <p/>
 * At some point during Gateway application context construction, when the auditing subsystem is fully ready to go,
 * this context is no longer used and the regular thread local audit context is used instead.
 * <p/>
 * It is safe for multiple threads to use an instance of this class simultaneously.
 */
public class MemoryBufferingAuditContext implements AuditContext {
    private static final Logger logger = Logger.getLogger(MemoryBufferingAuditContext.class.getName());

    @Override
    public void setCurrentRecord(AuditRecord record) {
        assertNotShutdown();
        if (record == null) 
            throw new NullPointerException();
        final long uniqueThreadId = uniqueThreadId();
        RecordedAuditRecord currentRecord = currentRecordsByThread.get(uniqueThreadId);
        if (currentRecord != null) {
            throw new IllegalStateException("Only one audit record can be active at one time (existing is '"+currentRecord.auditRecord.getMessage()+"', new is '"+record.getMessage()+"')");
        }
        currentRecordsByThread.put(uniqueThreadId, new RecordedAuditRecord(record));
    }

    @Override
    public void addDetail(AuditDetail detail, Object source) {
        addDetail(new AuditDetailEvent.AuditDetailWithInfo(source, detail, null, null));
    }

    @Override
    public void addDetail(AuditDetailEvent.AuditDetailWithInfo auditDetailInfo) {
        assertNotShutdown();
        currentRecord().details.add(auditDetailInfo);
    }

    @Override
    public boolean isUpdate() {
        return false;
    }

    @Override
    public void setUpdate(boolean update) {
        if (update)
            throw new IllegalStateException("Audit record updates (audit purge event?) not supported until message processing auditing is initialized");
    }

    @Override
    public Set getHints() {
        throw new IllegalStateException("Hints not supported until message processing auditing is initialized");
    }

    @Override
    public void flush() {
        assertNotShutdown();
        RecordedAuditRecord current = currentRecordsByThread.remove(uniqueThreadId());
        if (current != null)
            recordedAuditRecords.add(current);
    }

    @Override
    public void clear() {
        assertNotShutdown();
        currentRecordsByThread.remove(uniqueThreadId());                
    }

    @Override
    public Map<Object, List<AuditDetail>> getDetails() {
        Map<Object,List<AuditDetail>> ads = new HashMap<Object,List<AuditDetail>>();

        List<AuditDetailEvent.AuditDetailWithInfo> infos = currentRecord().details;
        for (AuditDetailEvent.AuditDetailWithInfo info : infos) {
            final Object source = info.getSource();
            List<AuditDetail> ds = ads.get(source);
            if (ds == null) {
                ds = new ArrayList<AuditDetail>();
                ads.put(source, ds);
            }            
            ds.add(info.getDetail());
        }

        return Collections.unmodifiableMap(ads);
    }

    @Override
    public String[] getContextVariablesUsed() {
        return AuditLogFormatter.getContextVariablesUsed();
    }
        
    @Override
    public void setContextVariables(Map<String, Object> variables) {
        throw new IllegalStateException("setContextVariables not supported until message processing auditing is initialized");
    }

    /**
     * Turn off any further buffering of audit information for this buffering audit context.
     * <p/>
     * After this method is called, any calls to methods such as setCurrentRecord, addDetail, flush, or clear will
     * trigger IllegalStateException.
     * <p/>
     * It is still safe to retrieve buffered records using methods such as replayAllBufferedRecords after this
     * instance has been shut down.
     */
    public void shutdownBuffering() {
        if (shutdown.getAndSet(true))
            assertNotShutdown(); // force failure if was already shut down
    }

    /**
     * Check if any unflushed audit records are present for any thread.
     *
     * @return true if any records have been set on this context (by any thread) that have not been flushed or cleared.
     */
    public boolean isAnyUnflushedAuditRecords() {
        return !currentRecordsByThread.isEmpty();
    }

    /**
     * Replays all buffered audit records into the specified target audit context, removing them from the
     * buffer as we go, and flushing the target context after each record.
     * <p/>
     * Each buffered record will be replayed exactly once, even if multiple threads call this method simultaneously.
     * <p/>
     * Any currently open record will not be replayed to the target context.
     * <p/>
     * It is safe to continue using this MemoryBufferingAuditContext during and after this replay operation.
     * <p/>
     * It is safe to call this method after buffering has been shut down.
     * 
     * @param target the audit context to receive all buffered records.
     */
    public void replayAllBufferedRecords(AuditContext target) {
        int count = 0;
        RecordedAuditRecord record;
        while (null != (record = recordedAuditRecords.poll())) {
            replayBufferedRecord(record, target);
            count++;
        }
        logger.info("Processed " + count + " audit records");
    }

    private void replayBufferedRecord(RecordedAuditRecord record, AuditContext target) {
        target.setCurrentRecord(record.auditRecord);        
        List<AuditDetailEvent.AuditDetailWithInfo> details = record.details;
        for (AuditDetailEvent.AuditDetailWithInfo detail : details) {
            target.addDetail(detail);
        }
        target.flush();
    }
    
    /**
     * @return the current record for the current thread.  never null
     * @throws IllegalStateException if there is no current record
     */
    private RecordedAuditRecord currentRecord() {
        assertNotShutdown();
        RecordedAuditRecord currentRecord = currentRecordsByThread.get(uniqueThreadId());
        if (currentRecord == null)
            throw new IllegalStateException("No audit record has been set yet");
        return currentRecord;
    }

    private void assertNotShutdown() {
        if (shutdown.get())
            throw new IllegalStateException("This MemoryBufferingAuditContext has been shut down and should no longer be in use");
    }

    private static class RecordedAuditRecord {
        private final AuditRecord auditRecord;
        private final List<AuditDetailEvent.AuditDetailWithInfo> details = new ArrayList<AuditDetailEvent.AuditDetailWithInfo>();

        /**
         * @param auditRecord the audit record to use for events for this thread
         */
        private RecordedAuditRecord(AuditRecord auditRecord) {
            this.auditRecord = auditRecord;
        }
    }

    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final Queue<RecordedAuditRecord> recordedAuditRecords = new ConcurrentLinkedQueue<RecordedAuditRecord>();
    private final ConcurrentMap<Long,RecordedAuditRecord> currentRecordsByThread = new ConcurrentHashMap<Long, RecordedAuditRecord>();
    
    private static final AtomicLong nextThreadId = new AtomicLong(0);
    private static final ThreadLocal<Long> threadId = new ThreadLocal<Long>() {
        @Override
        protected Long initialValue() {
            return nextThreadId.getAndIncrement();
        }
    };

    /**
     * @return a thread ID that is unique for the lifetime of this process: it is (unlike Thread.getId()) guaranteed to never be reused even if 
     *         its original thread dies off and the OS-level ID is reused by a new thread
     */
    private static long uniqueThreadId() {
        return threadId.get();
    }

}
