package com.l7tech.external.assertions.concall.server;

import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.server.audit.AuditContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * A thread-safe audit context that will record details on different threads.
 * <p/>
 * This only implements enough of the AuditContext interface to be useful as a mock for ServerConcurrentAllAssertion.
 * It is not (currently) expected to be useful for other tests.
 */
public class ConcurrentDetailCollectingAuditContext implements AuditContext {
    private static final Logger logger = Logger.getLogger(ConcurrentDetailCollectingAuditContext.class.getName());

    boolean logDetails = false;

    ConcurrentMap<Long, List<AuditDetail>> detailsByThreadId = new ConcurrentHashMap<Long, List<AuditDetail>>();

    @Override
    public void setCurrentRecord(AuditRecord record) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDetail(AuditDetail detail, Object source) {
        addDetail(detail, source, null, null);
    }

    @Override
    public void addDetail(AuditDetail detail, Object source, Throwable thrown, String loggerName) {
        List<AuditDetail> list = getDetailListForThread();
        list.add(detail);

        if (logDetails) {

            logger.info("Detail: " + detailToString(detail));
        }
    }

    private List<AuditDetail> getDetailListForThread() {
        long tid = Thread.currentThread().getId();
        List<AuditDetail> list = detailsByThreadId.get(tid);
        if (list == null) {
            list = new ArrayList<AuditDetail>();
            detailsByThreadId.put(tid, list);
        }
        return list;
    }

    private String detailToString(AuditDetail detail) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            detail.serializeSignableProperties(baos);
            return baos.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isUpdate() {
        throw new UnsupportedOperationException("no mock for isUpdate()");
    }

    @Override
    public void setUpdate(boolean update) {
        throw new UnsupportedOperationException("no mock for setUpdate()");
    }

    @Override
    public Set getHints() {
        throw new UnsupportedOperationException("no mock for getHints()");
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException("no mock for flush()");
    }

    @Override
    public void clear() {
        detailsByThreadId.remove(Thread.currentThread().getId());
    }

    public void clearAll() {
        detailsByThreadId.clear();
    }

    public List<AuditDetail> getOtherThreadDetails() {
        List<AuditDetail> ret = new ArrayList<AuditDetail>();
        for (Map.Entry<Long, List<AuditDetail>> entry : detailsByThreadId.entrySet()) {
            if (Thread.currentThread().getId() != entry.getKey())
                ret.addAll(entry.getValue());
        }
        return ret;
    }

    public List<AuditDetail> getCurrentThreadDetails() {
        List<AuditDetail> ret = new ArrayList<AuditDetail>();
        for (Map.Entry<Long, List<AuditDetail>> entry : detailsByThreadId.entrySet()) {
            if (Thread.currentThread().getId() == entry.getKey())
                ret.addAll(entry.getValue());
        }
        return ret;
    }

    @Override
    public Map<Object, List<AuditDetail>> getDetails() {
        List<AuditDetail> list = getDetailListForThread();
        Map<Object, List<AuditDetail>> ret = new HashMap<Object, List<AuditDetail>>();
        ret.put(this, list);
        return ret;
    }

    @Override
    public String[] getContextVariablesUsed() {
        throw new UnsupportedOperationException("no mock for getContextVariablesUsed()");
    }

    @Override
    public void setContextVariables(Map<String, Object> variables) {
        throw new UnsupportedOperationException("no mock for setContextVariables()");
    }
}
