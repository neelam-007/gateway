package com.l7tech.server.event.metrics;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.policy.assertion.AssertionMetrics;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.message.AssertionTraceListener;
import com.l7tech.server.message.PolicyContextCache;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextWrapper;
import com.l7tech.server.policy.PolicyMetadata;
import com.l7tech.server.policy.assertion.RoutingResultListener;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.stepdebug.DebugContext;
import com.l7tech.xml.SoapFaultLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.*;
import java.util.logging.Level;

/**
 * TODO: add javadoc
 */
abstract class GatewayMetricsEvent {
    private final PolicyEnforcementContext context;

    /**
     * Thread local to protect against passing instances of this class to to another thread.<br/>
     * The thread who creates the instance (calls the constructor) owns it and only that thread can access its content.
     */
    private ThreadLocal<Boolean> ownedByThisThread = new ThreadLocal<Boolean>(){
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    protected GatewayMetricsEvent(@NotNull final PolicyEnforcementContext context) {
        this.context = context;

        // set this thread as the owner of this instance
        ownedByThisThread.set(true);
    }

    /**
     * Check whether the calling thread is the owner of this object.
     *
     * @throws IllegalStateException when the calling thread is not the owner (i.e the one created this object) of this object
     */
    protected void checkOwnerThread() throws IllegalStateException {
        if (!ownedByThisThread.get()) {
            throw new IllegalStateException("Event is not owned by this thread");
        }
    }

    /**
     * TODO: add javadoc
     */
    private static class ReadOnlyPolicyEnforcementContext extends PolicyEnforcementContextWrapper {

        ReadOnlyPolicyEnforcementContext(final PolicyEnforcementContext delegate) {
            super(delegate);
        }

        @Override
        public void setRoutingStatus(final RoutingStatus routingStatus) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setAuditLevel(final Level auditLevel) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addSeenAssertionStatus(final AssertionStatus assertionStatus) {
            throw new UnsupportedOperationException();
        }

        /**
         * Implementation is already returning read-only set
         * uncomment this if logic changes
         */
//        @Override
//        public Set<AssertionStatus> getSeenAssertionStatus() {
//            return Collections.unmodifiableSet(super.getSeenAssertionStatus());
//        }

        @Override
        public void setAuditSaveRequest(final boolean auditSaveRequest) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setAuditSaveResponse(final boolean auditSaveResponse) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<ServerAssertion> getDeferredAssertions() {
            return Collections.unmodifiableCollection(super.getDeferredAssertions());
        }

        @Override
        public void addDeferredAssertion(final ServerAssertion owner, final ServerAssertion decoration) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeDeferredAssertion(final ServerAssertion owner) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addRoutingResultListener(final RoutingResultListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeRoutingResultListener(final RoutingResultListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setAuthenticationMissing() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setRequestPolicyViolated() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setRequestClaimingWrongPolicyVersion() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setService(final PublishedService service) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setServicePolicyMetadata(final PolicyMetadata policyMetadata) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setCurrentPolicyMetadata(final PolicyMetadata policyMetadata) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setCache(final PolicyContextCache cache) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> getIncrementedCounters() {
            return Collections.unmodifiableList(super.getIncrementedCounters());
        }

        @Override
        public void setVariable(@Nullable final String name, @Nullable final Object value) throws VariableNotSettableException {
            throw new UnsupportedOperationException();
        }

        /**
         * Implementation is already returning read-only set
         * uncomment this if logic changes
         */
//        @Override
//        public Map<String, Object> getAllVariables() {
//            return Collections.unmodifiableMap(super.getAllVariables());
//        }

        @Override
        public Map<String, Object> getVariableMap(final String[] names, final Audit auditor) {
            return Collections.unmodifiableMap(super.getVariableMap(names, auditor));
        }

        @Override
        public void routingStarted() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void routingFinished() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setAssertionLatencyNanos(final long latency) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setRoutedServiceUrl(final URL routedServiceUrl) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setEndTime() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setFaultlevel(final SoapFaultLevel faultlevel) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void assertionStarting(final ServerAssertion assertion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void assertionFinished(final ServerAssertion assertion, final AssertionStatus status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void assertionFinished(final ServerAssertion assertion, final AssertionStatus status, @Nullable final AssertionMetrics assertionMetrics) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<AssertionResult> getAssertionResults() {
            return Collections.unmodifiableList(super.getAssertionResults());
        }

        @Override
        public Collection<Integer> getAssertionNumber() {
            return Collections.unmodifiableCollection(super.getAssertionNumber());
        }

        @Override
        public void setResponseWss11(final boolean wss11) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setMalformedRequest() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSavedRequestL7aMessageId(final String messageId) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public List<MessageContextMapping> getMappings() {
            return Collections.unmodifiableList(super.getMappings());
        }

        @Override
        public void setMappings(final List<MessageContextMapping> mappings) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<Integer> getAssertionOrdinalPath() {
            return Collections.unmodifiableCollection(super.getAssertionOrdinalPath());
        }

        @Override
        public void pushAssertionOrdinal(final int ordinal) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int popAssertionOrdinal() throws NoSuchElementException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setTraceListener(final AssertionTraceListener traceListener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setRequestWasCompressed(final boolean requestWasCompressed) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setPolicyResult(final AssertionStatus policyResult) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setPolicyExecutionAttempted(final boolean attempted) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setDebugContext(@Nullable final DebugContext debugContext) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setAuditContext(@Nullable final AuditContext auditContext) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void runOnClose(final Runnable runMe) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void runOnCloseFirst(final Runnable runMe) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setOverwriteResponseCookiePath(final boolean overwriteResponseCookiePath) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setOverwriteResponseCookieDomain(final boolean overwriteResponseCookieDomain) {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    public final PolicyEnforcementContext getContext() {
        // make sure the PEC is accessed only by the owner thread
        checkOwnerThread();
        return new ReadOnlyPolicyEnforcementContext(context);
    }
}
