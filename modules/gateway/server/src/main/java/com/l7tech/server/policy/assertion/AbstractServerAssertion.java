package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.audit.Auditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Abstract implementation for the getAssertion
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 11, 2006<br/>
 */
public abstract class AbstractServerAssertion<AT extends Assertion> implements ServerAssertion<AT> {

    //- PUBLIC

    public AbstractServerAssertion( @NotNull final AT assertion ) {
        this.assertion = assertion;
        this.logger = Logger.getLogger( getClass().getName() );
    }

    @NotNull
    @Override
    public AT getAssertion() {
        return assertion;
    }

    /**
     * Override to cleanup any resources allocated in the ServerAssertion.
     * Caller is responsible for ensuring that no requests are currently using -- or will ever
     * again use -- this ServerAssertion after close() is called.
     */
    @Override
    public void close() {
    }

    //- PROTECTED

    @NotNull protected final Logger logger;
    @NotNull protected final AT assertion;

    /**
     * Get the Audit for this assertion.
     *
     * <p>This method should be used when an Audit instance is required. For
     * auditing from a server assertion use the <code>logAndAudit</code>
     * methods.</p>
     *
     * @return The Audit
     * @see #logAndAudit(AuditDetailMessage)
     * @see #logAndAudit(AuditDetailMessage,String...)
     * @see #logAndAudit(AuditDetailMessage,String[],Throwable)
     */
    @NotNull
    protected final Audit getAudit() {
        Audit audit = auditReference.get();
        if ( audit == null ) {
            audit = auditorFactory.newInstance( this, logger );
            auditReference.compareAndSet( null, audit );
        }
        return audit;
    }

    /**
     * Log and/or audit a message using this assertions Audit instance.
     */
    protected final void logAndAudit( @NotNull  final AuditDetailMessage msg,
                                      @Nullable final String[] params,
                                      @Nullable final Throwable e ) {
        getAudit().logAndAudit( msg, params, e );
    }

    /**
     * Log and/or audit a message using this assertions Audit instance.
     */
    protected final void logAndAudit( @NotNull final AuditDetailMessage msg,
                                      @NotNull final String... params ) {
        getAudit().logAndAudit( msg, params );
    }

    /**
     * Log and/or audit a message using this assertions Audit instance.
     */
    protected final void logAndAudit( @NotNull final AuditDetailMessage msg ) {
        getAudit().logAndAudit( msg );
    }

    //- PRIVATE

    @Inject
    private Auditor.AuditorFactory auditorFactory;
    private final AtomicReference<Audit> auditReference = new AtomicReference<Audit>();

}
