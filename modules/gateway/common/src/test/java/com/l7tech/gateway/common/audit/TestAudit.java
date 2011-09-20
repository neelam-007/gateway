package com.l7tech.gateway.common.audit;

import com.l7tech.util.Functions;
import com.l7tech.util.Triple;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * And audit implementation for use in tests
 */
public class TestAudit implements Audit, AuditHaver {

    //- PUBLIC

    public TestAudit() {
    }

    @Override
    public final void logAndAudit( final AuditDetailMessage msg, final String[] params, final Throwable e ) {
        if ( notifyAudit( msg, params, e ) ) {
            storeAudit( msg, params, e );
        }
    }

    @Override
    public final void logAndAudit( final AuditDetailMessage msg, final String... params ) {
        logAndAudit( msg, params, null );
    }

    @Override
    public final void logAndAudit( final AuditDetailMessage msg ) {
        logAndAudit( msg, new String[0], null );
    }

    @Override
    public Audit getAuditor() {
        return this;
    }

    public boolean isAuditPresent( final AuditDetailMessage message ) {
        return matchAudit( new Functions.Unary<Boolean, Triple<AuditDetailMessage, String[], Throwable>>() {
            @Override
            public Boolean call( final Triple<AuditDetailMessage, String[], Throwable> auditDetailMessageThrowableTriple ) {
                return auditDetailMessageThrowableTriple.left.getId() == message.getId();
            }
        } );
    }

    public boolean isAuditPresentContaining( final String text ) {
        return matchAudit( new Functions.Unary<Boolean, Triple<AuditDetailMessage, String[], Throwable>>() {
            @Override
            public Boolean call( final Triple<AuditDetailMessage, String[], Throwable> auditDetailMessageThrowableTriple ) {
                return MessageFormat.format( auditDetailMessageThrowableTriple.left.getMessage(), auditDetailMessageThrowableTriple.middle ).contains( text );
            }
        } );
    }

    /**
     * Get an AuditFactory that always returns this Audit instance.
     *
     * @return The AuditFactory
     */
    public AuditFactory factory() {
        return new AuditFactory() {
            @Override
            public Audit newInstance( final Object source, final Logger logger ) {
                return TestAudit.this;
            }
        };
    }

    /**
     * Reset to the original (empty) state
     */
    public void reset() {
        audits.clear();
    }

    //- PROTECTED

    protected boolean notifyAudit( final AuditDetailMessage msg, final String[] params, final Throwable e ) {
        return true;
    }

    //- PRIVATE

    private final List<Triple<AuditDetailMessage, String[], Throwable>> audits = new ArrayList<Triple<AuditDetailMessage, String[], Throwable>>();

    private void storeAudit( final AuditDetailMessage msg, final String[] params, final Throwable e ) {
        audits.add( new Triple<AuditDetailMessage, String[], Throwable>( msg, params, e ) );
    }

    private boolean matchAudit( final Functions.Unary<Boolean,Triple<AuditDetailMessage, String[], Throwable>> matcher ) {
        return Functions.reduce( audits, Boolean.FALSE, new Functions.Binary<Boolean, Boolean, Triple<AuditDetailMessage, String[], Throwable>>() {
            @Override
            public Boolean call( final Boolean aBoolean, final Triple<AuditDetailMessage, String[], Throwable> auditDetailMessageThrowableTriple ) {
                return aBoolean || matcher.call( auditDetailMessageThrowableTriple );
            }
        } );
    }

}
