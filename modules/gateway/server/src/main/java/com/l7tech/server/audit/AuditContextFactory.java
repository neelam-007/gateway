package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.server.DefaultKey;
import com.l7tech.util.Config;
import com.l7tech.util.Functions;
import com.l7tech.util.RunnableCallable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bean that manages thread-local audit contexts.
 */
public class AuditContextFactory {
    protected static final ThreadLocal<AuditContext> currentAuditContext = new ThreadLocal<AuditContext>();
    private static final AtomicReference<AuditLogListener> globalAuditLogListener = new AtomicReference<AuditLogListener>();

    private final AuditLogListener listener;
    private Config config;
    private SimpleAuditRecordManager auditRecordManager;
    private String nodeId;
    private AuditPolicyEvaluator auditPolicyEvaluator;
    private AuditFilterPolicyManager auditFilterPolicyManager;
    private DefaultKey keystore;

    private boolean readyToCreateActiveContexts = false;

    public AuditContextFactory(AuditLogListener listener) {
        this.listener = listener;
        if (listener != null) {
            if (!globalAuditLogListener.compareAndSet(null, listener))
                throw new IllegalStateException("At least one AuditContextFactory has already had a global AuditLogListener set");
        }
    }

    /*
     * Called to activate the server auditing subsystem once all its dependencies are ready to use.
     * Until then, details records are (immediately) sent to the log sinks, and no records can be saved to the database.
     */
    void activateServerAuditing(Config config,
                                SimpleAuditRecordManager auditRecordManager,
                                String nodeId,
                                AuditPolicyEvaluator auditPolicyEvaluator,
                                AuditFilterPolicyManager auditFilterPolicyManager,
                                DefaultKey keystore)
    {
        this.config = config;
        this.auditRecordManager = auditRecordManager;
        this.nodeId = nodeId;
        this.auditPolicyEvaluator = auditPolicyEvaluator;
        this.auditFilterPolicyManager = auditFilterPolicyManager;
        this.keystore = keystore;
        this.readyToCreateActiveContexts = true;
    }

    /**
     * @return the currently-active AuditContext for this thread. Never null, but may be a no-op AuditContext if nobody is currently listening.
     */
    public static AuditContext getCurrent() {
        AuditContext context = currentAuditContext.get();
        if (context == null) {
            context = new LogOnlyAuditContext(globalAuditLogListener.get());
        }
        return context;
    }

    /**
     * Runs the specified code with the specified audit context as the active context.
     * <p/>
     * The caller is responsible for ensuring that any needed flushing of the context is done after this
     * method returns.
     *
     * @param context a custom audit context to have in effect for the duration of the callable code.  Required.
     * @param callable code to execute with the custom audit context in effect.  Required.
     * @param <T> the return type of the callable
     * @return whatever is returned by the callable.
     * @throws Exception if the callable throws an exception.
     */
    public static <T> T doWithCustomAuditContext(@NotNull AuditContext context, @NotNull Callable<T> callable) throws Exception {
        final AuditContext prev = currentAuditContext.get();
        currentAuditContext.set(context);
        try {
            return callable.call();
        } finally {
            currentAuditContext.set(prev);
        }
    }

    /**
     * Emit the specified audit record immediately using a new audit context, with no audit details, without affecting the current
     * audit context (if any).
     *
     * @param auditRecord the record to immediately emit.
     */
    public void emitAuditRecord(AuditRecord auditRecord) {
        emitAuditRecord(auditRecord, false);
    }

    /**
     * Emit the specified audit record immediately using a new audit context, with no audit details, without affecting the current
     * audit context (if any).
     *
     * @param auditRecord the record to immediately emit.
     * @param update true to mark the record as an update to an existing record.
     */
    public void emitAuditRecord(AuditRecord auditRecord, boolean update) {
        emitAuditRecordWithDetails(auditRecord, update, null, null);
    }

    /**
     * Emit the specified audit record immediately using a new audit context, with the specified audit details, without affecting the current
     * audit context (if any).
     *
     * @param auditRecord the record to immediately emit.
     * @param update true to mark the record as an update to an existing record.
     * @param source source for details, or null if no details.
     * @param details details to include in audit record, or null for none.
     */
    public void emitAuditRecordWithDetails(AuditRecord auditRecord, boolean update, @Nullable Object source, @Nullable Collection<AuditDetail> details) {
        AuditContextImpl context = (AuditContextImpl) newContext();
        context.setCurrentRecord(auditRecord);
        if (update) context.setUpdate(true);

        if (details != null && !details.isEmpty()) {
            if (source == null)
                source = this;
            for (AuditDetail detail : details) {
                context.addDetail(detail, source);
            }
        }

        context.flush();
    }

    /**
     * Perform operations with a new audit context.
     * A new audit context will be created and set as the current context for this thread while the operation
     * is performed.  When the operation finished (either by returning normally or throwing an exception)
     * the new audit context will be flushed, and the previous audit context will be restored.
     * <p/>
     * This method is the same as {@link #doWithNewAuditContext(java.util.concurrent.Callable, com.l7tech.util.Functions.Nullary)}
     * except that it takes a Runnable rather than a Callable and so neither returns any values nor throws any checked
     * exceptions.
     *
     * @param auditRecord audit record that will be set just before the context is flushed.  The caller may modify the fields of this record at any time
     *                    up until the context is flushed with the following caveats:  the record may not be replaced with a different instance (so there is no way
     *                    to, for example, change a SystemAuditRecord with a MessageSummaryAuditRecord midway through); and, any detail records added to the main record
     *                    by any mechanism aside from adding them to the audit context will be overwritten when the audit context is flushed.
     * @param runnable operation to invoke.  Required.
     */
    public void doWithNewAuditContext(@NotNull final AuditRecord auditRecord, @NotNull Runnable runnable) {
        try {
            doWithNewAuditContext(auditRecord, (Callable<Void>)new RunnableCallable<Void>(runnable));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Perform operations with a new audit context.
     * A new audit context will be created and set as the current context for this thread while the operation
     * is performed.  When the operation finished (either by returning normally or throwing an exception)
     * the new audit context will be flushed, and the previous audit context will be restored.
     *
     *
     * @param auditRecord audit record that will be set just before the context is flushed.  The caller may modify the fields of this record at any time
     *                    up until the context is flushed with the following caveats:  the record may not be replaced with a different instance (so there is no way
     *                    to, for example, change a SystemAuditRecord with a MessageSummaryAuditRecord midway through); and, any detail records added to the main record
     *                    by any mechanism aside from adding them to the audit context will be overwritten when the audit context is flushed.
     * @param callable operation to invoke.  Required.
     * @return the value returned by the operation, if any.
     * @throws Exception
     */
    public <T> T doWithNewAuditContext(@NotNull final AuditRecord auditRecord, @NotNull Callable<T> callable) throws Exception {
        return doWithNewAuditContext(callable, new Functions.Nullary<com.l7tech.gateway.common.audit.AuditRecord>() {
            @Override
            public AuditRecord call() {
                return auditRecord;
            }
        });
    }

    /**
     * Perform operations with a new audit context.
     * A new audit context will be created and set as the current context for this thread while the operation
     * is performed.  When the operation finished (either by returning normally or throwing an exception)
     * the new audit context will be flushed, and the previous audit context will be restored.
     *
     * @param callable operation to invoke.  Required.
     * @param recordFactory factory that will be invoked exactly once, right before the context is flushed, to produce an AuditRecord.  Required.
     * @param <T> return type of operation.
     * @return the value returned by the operation, if any.
     * @throws Exception
     */
    public <T> T doWithNewAuditContext(@NotNull Callable<T> callable, @NotNull Functions.Nullary<AuditRecord> recordFactory) throws Exception {
        final AuditContext prev = currentAuditContext.get();
        final AuditContextImpl current = (AuditContextImpl)newContext();
        currentAuditContext.set(current);
        try {
            return callable.call();
        } finally {
            try {
                current.setCurrentRecord(recordFactory.call());
                current.flush();
            } finally {
                currentAuditContext.set(prev);
            }
        }
    }

    /**
     * If a subclass overrides this method, it must also override doWithNewAuditContext and emitAuditRecord.
     *
     * @return an AuditContext that will work with {@link #doWithNewAuditContext(java.util.concurrent.Callable, com.l7tech.util.Functions.Nullary)}.
     * @throws IllegalStateException if it is too early in startup to create an audit context
     */
    protected AuditContext newContext() {
        if (!readyToCreateActiveContexts)
            return new LogOnlyAuditContext(globalAuditLogListener.get());
        return new AuditContextImpl(config, auditRecordManager, auditPolicyEvaluator, auditFilterPolicyManager, nodeId, keystore, listener);
    }
}
