package com.l7tech.gateway.common.audit;

import com.l7tech.util.Functions;
import com.l7tech.util.Triple;

import java.util.ArrayList;
import java.util.List;

/**
 * And audit implementation for use in tests
 */
public class TestAudit implements Audit {

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

    public boolean isAuditPresent( final AuditDetailMessage message ) {
        return Functions.reduce( audits, Boolean.FALSE, new Functions.Binary<Boolean, Boolean, Triple<AuditDetailMessage, String[], Throwable>>() {
            @Override
            public Boolean call( final Boolean aBoolean, final Triple<AuditDetailMessage, String[], Throwable> auditDetailMessageThrowableTriple ) {
                return aBoolean || auditDetailMessageThrowableTriple.left.getId() == message.getId();
            }
        } );
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

}
