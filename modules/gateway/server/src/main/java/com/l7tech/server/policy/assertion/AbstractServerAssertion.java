package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.audit.AuditHaver;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.util.Injector;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
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

    /**
     * Create a server assertion with the given values.
     *
     * <p>Use this constructor if the server assertion subclass audits from its
     * constructor.</p>
     *
     * @param assertion The assertion bean
     * @param auditFactory The factory to use for Audit creation
     */
    public AbstractServerAssertion( @NotNull  final AT assertion,
                                    @Nullable final AuditFactory auditFactory ) {
        this.assertion = assertion;
        this.auditFactory = auditFactory;
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
        return getAudit( true );
    }

    /**
     * Get an AuditHaver that gets the Audit for this assertion.
     *
     * @return The AuditHaver
     */
    @NotNull
    protected final AuditHaver getAuditHaver() {
        return new AuditHaver() {
            @Override
            public Audit getAuditor() {
                return getAudit();
            }
        };
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

    @Inject
    protected final void setInjector( final Injector injector ) {
        if ( this.injector == null ) {
            this.injector = injector;
            injectDependencies();
        }
    }

    /**
     * Inject dependencies for the target object.
     *
     * <p>This method may only be called during or after the
     * "injectDependencies" callback.</p>
     *
     * @param target The bean to inject
     */
    protected final void inject( @NotNull final Object target ) {
        injector.inject( target );
    }

    /**
     * Inject dependencies for the target objects.
     *
     * <p>This method may only be called during or after the
     * "injectDependencies" callback.</p>
     *
     * @param targets The beans to inject
     */
    protected final void injectAll( @NotNull final Iterable<?> targets ) {
        CollectionUtils.foreach( targets, false, new Functions.UnaryVoid<Object>() {
            @Override
            public void call( final Object target ) {
                inject( target );
            }
        } );
    }

    /**
     * Dependency injection callback.
     *
     * <p>Use injection callback methods to inject dependencies for children.</p>
     *
     * @see #inject
     * @see #injectAll(Iterable)
     */
    protected void injectDependencies() {
    }

    //- PRIVATE

    @Inject
    private AuditFactory auditFactory;
    private Injector injector;
    private final AtomicReference<Audit> auditReference = new AtomicReference<Audit>();

    private Audit getAudit( boolean allowLazy ) {
        Audit audit = auditReference.get();
        if ( audit == null ) {
            if ( auditFactory == null ) {
                if ( allowLazy ) {
                    // create a lazy auditor
                    audit = new Audit() {
                        @Override
                        public void logAndAudit( @NotNull final AuditDetailMessage msg,
                                                 final String[] params,
                                                 final Throwable e ) {
                            getAudit( false ).logAndAudit( msg, params, e );
                        }

                        @Override
                        public void logAndAudit( @NotNull final AuditDetailMessage msg,
                                                 final String... params ) {
                            getAudit( false ).logAndAudit( msg, params );
                        }

                        @Override
                        public void logAndAudit( @NotNull final AuditDetailMessage msg ) {
                            getAudit( false ).logAndAudit( msg );
                        }
                    };
                } else {
                    // fallback to logging only, this will only
                    // be used until the factory is available
                    return new LoggingAudit( logger );
                }
            } else {
                audit = auditFactory.newInstance( this, logger );
                auditReference.compareAndSet( null, audit );
            }
        }
        return audit;

    }
}
