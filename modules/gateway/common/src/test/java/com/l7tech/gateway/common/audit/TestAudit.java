package com.l7tech.gateway.common.audit;

import com.l7tech.util.Functions;
import com.l7tech.util.Triple;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import static com.l7tech.util.Functions.forall;
import static com.l7tech.util.Functions.negate;
import static com.l7tech.util.Functions.reduce;

/**
 * And audit implementation for use in tests
 */
public class TestAudit implements Audit, AuditHaver, Iterable<String> {

    //- PUBLIC

    public TestAudit() {
    }

    @Override
    public final void logAndAudit( @NotNull final AuditDetailMessage msg, final String[] params, final Throwable e ) {
        if ( notifyAudit( msg, params, e ) ) {
            storeAudit( msg, params, e );
        }
    }

    @Override
    public final void logAndAudit( @NotNull final AuditDetailMessage msg, final String... params ) {
        logAndAudit( msg, params, null );
    }

    @Override
    public final void logAndAudit( @NotNull final AuditDetailMessage msg ) {
        logAndAudit( msg, new String[0], null );
    }

    @Override
    public Audit getAuditor() {
        return this;
    }

    public boolean isAllOfAuditsPresent( final Iterable<AuditDetailMessage> messages ) {
        return forall(messages, isAuditPresentPredicate());
    }

    public boolean isNoneOfAuditsPresent( final Iterable<AuditDetailMessage> messages ) {
        return forall(messages, negate(isAuditPresentPredicate()));
    }

    public boolean isAuditPresent( final AuditDetailMessage message ) {
        return isAuditPresent(message, false);
    }

    public boolean isAuditPresent( final AuditDetailMessage message, final boolean mustIncludeStack ) {
        return matchAudit(new Functions.Unary<Boolean, Triple<AuditDetailMessage, String[], Throwable>>() {
            @Override
            public Boolean call(final Triple<AuditDetailMessage, String[], Throwable> auditDetailMessageThrowableTriple) {
                return auditDetailMessageThrowableTriple.left.getId() == message.getId() &&
                        (!mustIncludeStack || auditDetailMessageThrowableTriple.right != null);
            }
        });
    }

    public boolean isAuditPresentWithParameters(final AuditDetailMessage message, final String... parameters) {
        return matchAudit(new Functions.Unary<Boolean, Triple<AuditDetailMessage, String[], Throwable>>() {
            @Override
            public Boolean call( final Triple<AuditDetailMessage, String[], Throwable> auditDetailMessageThrowableTriple ) {
                return auditDetailMessageThrowableTriple.left.getId() == message.getId() &&
                        Arrays.equals(auditDetailMessageThrowableTriple.middle, parameters);
            }
        });
    }

    public boolean isAuditPresentContaining( final String text ) {
        return matchAudit( new Functions.Unary<Boolean, Triple<AuditDetailMessage, String[], Throwable>>() {
            @Override
            public Boolean call( final Triple<AuditDetailMessage, String[], Throwable> auditDetailMessageThrowableTriple ) {
                return MessageFormat.format( auditDetailMessageThrowableTriple.left.getMessage(), auditDetailMessageThrowableTriple.middle ).contains( text );
            }
        } );
    }

    public boolean isAnyAuditPresent() {
        return iterator().hasNext();
    }

    public int getAuditCount() {
        return audits.size();
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
        return reduce(audits, Boolean.FALSE, new Functions.Binary<Boolean, Boolean, Triple<AuditDetailMessage, String[], Throwable>>() {
            @Override
            public Boolean call(final Boolean aBoolean, final Triple<AuditDetailMessage, String[], Throwable> auditDetailMessageThrowableTriple) {
                return aBoolean || matcher.call(auditDetailMessageThrowableTriple);
            }
        });
    }

    private <Msg extends AuditDetailMessage> Functions.Unary<Boolean, ? super Msg> isAuditPresentPredicate() {
        return new Functions.Unary<Boolean, Msg>() {
            @Override
            public Boolean call(Msg msg) {
                return isAuditPresent(msg);
            }
        };
    }

    @Override
    public Iterator<String> iterator() {
        return Functions.map(audits, new Functions.Unary<String, Triple<AuditDetailMessage, String[], Throwable>>() {
            @Override
            public String call(Triple<AuditDetailMessage, String[], Throwable> auditDetailMessageThrowableTriple) {
                return MessageFormat.format( auditDetailMessageThrowableTriple.left.getMessage(), auditDetailMessageThrowableTriple.middle );
            }
        }).iterator();
    }
}
