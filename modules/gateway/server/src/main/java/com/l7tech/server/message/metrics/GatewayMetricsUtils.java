package com.l7tech.server.message.metrics;

import com.l7tech.gateway.common.RequestId;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.event.metrics.AssertionFinished;
import com.l7tech.server.event.metrics.GatewayMetricsEvent;
import com.l7tech.server.message.AssertionTraceListener;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyContextCache;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyMetadata;
import com.l7tech.server.policy.assertion.RoutingResultListener;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.stepdebug.DebugContext;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.Pair;
import com.l7tech.xml.SoapFaultLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXException;

import javax.wsdl.Binding;
import javax.wsdl.Operation;
import javax.wsdl.WSDLException;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;

/**
 * Utility class containing methods to set the {@link GatewayMetricsPublisher} and to pass the publisher from the parent PEC to kid PEC
 */
public final class GatewayMetricsUtils {

    /**
     * Set the {@link GatewayMetricsPublisher} if the PEC supports the {@link GatewayMetricsSupport}.
     * Set to null if the publisher has no subscribers.
     *
     * @param context the context of which the publisher is set to. Cannot be {@code null}.
     * @param publisher the publisher to be set to. The publisher will be {@code null} in the case there are no subscribers {@link GatewayMetricsListener}
     */
    public static void setPublisher(@NotNull final PolicyEnforcementContext context, @Nullable final GatewayMetricsPublisher publisher) {
        if (context instanceof GatewayMetricsSupport) {
            ((GatewayMetricsSupport) context).setGatewayMetricsEventsPublisher(publisher != null && publisher.hasSubscribers() ? publisher : null);
        }
    }

    /**
     * passes the publisher from the parent PEC to the kid PEC if the PEC supports the {@link GatewayMetricsSupport}.
     *
     * @param parent the parent PEC containing the publisher {@link GatewayMetricsPublisher}. Cannot be {@code null}
     * @param child the kid PEC to pass the publisher to {@link GatewayMetricsPublisher}. Cannot be {@code null}.
     */
    public static void setPublisher(@NotNull final PolicyEnforcementContext parent, @NotNull final PolicyEnforcementContext child) {
        // pass GatewayMetricsPublisher from parent to child PEC
        if (parent instanceof GatewayMetricsSupport && child instanceof GatewayMetricsSupport) {
            ((GatewayMetricsSupport) child).setGatewayMetricsEventsPublisher(((GatewayMetricsSupport) parent).getGatewayMetricsEventsPublisher());
        }
    }

    private static class GatewayMetricsEventImpl implements GatewayMetricsEvent {
        private final ReadOnlyPolicyEnforcementContext context;

        private GatewayMetricsEventImpl(@NotNull final PolicyEnforcementContext context) {
            this.context = new ReadOnlyPolicyEnforcementContext(context);
        }

        @NotNull
        @Override
        public PolicyEnforcementContext getContext() {
            return context;
        }

        /**
         * Creates a read only copy of the {@link PolicyEnforcementContext}. Any method that modifies the PEC will throw a {@link UnsupportedOperationException}.
         */
        private static class ReadOnlyPolicyEnforcementContext implements PolicyEnforcementContext {
            /**
             * Thsi is our delegate instance
             */
            @NotNull
            private final PolicyEnforcementContext delegate;

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

            private ReadOnlyPolicyEnforcementContext(@NotNull final PolicyEnforcementContext delegate) {
                this.delegate = delegate;

                // set this thread as the owner of this instance
                ownedByThisThread.set(true);
            }

            /**
             * Check whether the calling thread is the owner of this object.
             *
             * @throws IllegalStateException when the calling thread is not the owner (i.e the one created this object) of this object
             */
            private void checkOwnerThread() throws IllegalStateException {
                if (!ownedByThisThread.get()) {
                    throw new IllegalStateException("PEC is not owned by this thread");
                }
            }

            @Override
            public Message getRequest() {
                checkOwnerThread();
                return delegate.getRequest();
            }

            @Override
            public Message getResponse() {
                checkOwnerThread();
                return delegate.getResponse();
            }

            @Override
            public AuthenticationContext getDefaultAuthenticationContext() {
                checkOwnerThread();
                return delegate.getDefaultAuthenticationContext();
            }

            @Override
            public AuthenticationContext getAuthenticationContext(final Message message) {
                checkOwnerThread();
                return delegate.getAuthenticationContext(message);
            }

            @Override
            public RoutingStatus getRoutingStatus() {
                checkOwnerThread();
                return delegate.getRoutingStatus();
            }

            @Override
            public void setRoutingStatus(final RoutingStatus routingStatus) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isPostRouting() {
                checkOwnerThread();
                return delegate.isPostRouting();
            }

            @Override
            public boolean isReplyExpected() {
                checkOwnerThread();
                return delegate.isReplyExpected();
            }

            @Override
            public Level getAuditLevel() {
                checkOwnerThread();
                return delegate.getAuditLevel();
            }

            @Override
            public void setAuditLevel(final Level auditLevel) {
                throw new UnsupportedOperationException();
            }

            @Override
            public RequestId getRequestId() {
                checkOwnerThread();
                return delegate.getRequestId();
            }

            @Override
            public void addSeenAssertionStatus(final AssertionStatus assertionStatus) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<AssertionStatus> getSeenAssertionStatus() {
                checkOwnerThread();
                return delegate.getSeenAssertionStatus();
            }

            @Override
            public boolean isAuditSaveRequest() {
                checkOwnerThread();
                return delegate.isAuditSaveRequest();
            }

            @Override
            public void setAuditSaveRequest(final boolean auditSaveRequest) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isAuditSaveResponse() {
                checkOwnerThread();
                return delegate.isAuditSaveResponse();
            }

            @Override
            public void setAuditSaveResponse(final boolean auditSaveResponse) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Collection<ServerAssertion> getDeferredAssertions() {
                checkOwnerThread();
                return Collections.unmodifiableCollection(delegate.getDeferredAssertions());
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
            public RoutingResultListener getRoutingResultListener() {
                checkOwnerThread();
                return delegate.getRoutingResultListener();
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
            public boolean isRequestPolicyViolated() {
                checkOwnerThread();
                return delegate.isRequestPolicyViolated();
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
            public boolean isRequestClaimingWrongPolicyVersion() {
                checkOwnerThread();
                return delegate.isRequestClaimingWrongPolicyVersion();
            }

            @Override
            public PublishedService getService() {
                checkOwnerThread();
                return delegate.getService();
            }

            @Override
            public void setService(final PublishedService service) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PolicyMetadata getServicePolicyMetadata() {
                checkOwnerThread();
                return delegate.getServicePolicyMetadata();
            }

            @Override
            public void setServicePolicyMetadata(final PolicyMetadata policyMetadata) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PolicyMetadata getCurrentPolicyMetadata() {
                checkOwnerThread();
                return delegate.getCurrentPolicyMetadata();
            }

            @Override
            public void setCurrentPolicyMetadata(final PolicyMetadata policyMetadata) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PolicyContextCache getCache() {
                checkOwnerThread();
                return delegate.getCache();
            }

            @Override
            public void setCache(final PolicyContextCache cache) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<String> getIncrementedCounters() {
                checkOwnerThread();
                return Collections.unmodifiableList(delegate.getIncrementedCounters());
            }

            @Override
            public void setVariable(@Nullable final String name, @Nullable final Object value) throws VariableNotSettableException {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object getVariable(final String name) throws NoSuchVariableException {
                checkOwnerThread();
                return delegate.getVariable(name);
            }

            @Override
            public Map<String, Object> getAllVariables() {
                checkOwnerThread();
                return Collections.unmodifiableMap(delegate.getAllVariables());
            }

            @Override
            public Map<String, Object> getVariableMap(final String[] names, final Audit auditor) {
                checkOwnerThread();
                return Collections.unmodifiableMap(delegate.getVariableMap(names, auditor));
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
            public long getRoutingStartTime() {
                checkOwnerThread();
                return delegate.getRoutingStartTime();
            }

            @Override
            public long getRoutingEndTime() {
                checkOwnerThread();
                return delegate.getRoutingEndTime();
            }

            @Override
            public long getRoutingTotalTime() {
                checkOwnerThread();
                return delegate.getRoutingTotalTime();
            }

            @Override
            public Pair<Binding, Operation> getBindingAndOperation() throws IOException, SAXException, WSDLException, InvalidDocumentFormatException {
                checkOwnerThread();
                return delegate.getBindingAndOperation();
            }

            @Override
            public boolean isStealthResponseMode() {
                checkOwnerThread();
                return delegate.isStealthResponseMode();
            }

            @Override
            public long getStartTime() {
                checkOwnerThread();
                return delegate.getStartTime();
            }

            @Override
            public long getAssertionLatencyNanos() {
                checkOwnerThread();
                return delegate.getAssertionLatencyNanos();
            }

            @Override
            public void setAssertionLatencyNanos(final long latency) {
                throw new UnsupportedOperationException();
            }

            @Override
            public URL getRoutedServiceUrl() {
                checkOwnerThread();
                return delegate.getRoutedServiceUrl();
            }

            @Override
            public void setRoutedServiceUrl(final URL routedServiceUrl) {
                throw new UnsupportedOperationException();
            }

            @Override
            public long getEndTime() {
                checkOwnerThread();
                return delegate.getEndTime();
            }

            @Override
            public void setEndTime() {
                throw new UnsupportedOperationException();
            }

            @Override
            public SoapFaultLevel getFaultlevel() {
                checkOwnerThread();
                return delegate.getFaultlevel();
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
            public void assertionFinished(final ServerAssertion assertion, final AssertionStatus status, @Nullable final AssertionMetrics assertionMetrics) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<AssertionResult> getAssertionResults() {
                checkOwnerThread();
                return Collections.unmodifiableList(delegate.getAssertionResults());
            }

            @Override
            public Collection<Integer> getAssertionNumber() {
                checkOwnerThread();
                return Collections.unmodifiableCollection(delegate.getAssertionNumber());
            }

            @Override
            public Message getTargetMessage(final MessageTargetable targetable) throws NoSuchVariableException {
                checkOwnerThread();
                return delegate.getTargetMessage(targetable);
            }

            @Override
            public Message getOrCreateTargetMessage(final MessageTargetable targetable, final boolean allowNonMessagevar) throws NoSuchVariableException, VariableNotSettableException {
                checkOwnerThread();
                return delegate.getOrCreateTargetMessage(targetable, allowNonMessagevar);
            }

            @Override
            public Message getTargetMessage(final MessageTargetable targetable, final boolean allowNonMessageVar) throws NoSuchVariableException {
                checkOwnerThread();
                return delegate.getTargetMessage(targetable, allowNonMessageVar);
            }

            @Override
            public boolean isResponseWss11() {
                checkOwnerThread();
                return delegate.isResponseWss11();
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
            public boolean isMalformedRequest() {
                checkOwnerThread();
                return delegate.isMalformedRequest();
            }

            @Override
            public void setSavedRequestL7aMessageId(final String messageId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getSavedRequestL7aMessageId() {
                checkOwnerThread();
                return delegate.getSavedRequestL7aMessageId();
            }

            @NotNull
            @Override
            public List<MessageContextMapping> getMappings() {
                checkOwnerThread();
                return Collections.unmodifiableList(delegate.getMappings());
            }

            @Override
            public void setMappings(final List<MessageContextMapping> mappings) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Collection<Integer> getAssertionOrdinalPath() {
                checkOwnerThread();
                return Collections.unmodifiableCollection(delegate.getAssertionOrdinalPath());
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
            public boolean hasTraceListener() {
                checkOwnerThread();
                return delegate.hasTraceListener();
            }

            @Override
            public AssertionTraceListener getTraceListener() {
                checkOwnerThread();
                return delegate.getTraceListener();
            }

            @Override
            public boolean isRequestWasCompressed() {
                checkOwnerThread();
                return delegate.isRequestWasCompressed();
            }

            @Override
            public void setRequestWasCompressed(final boolean requestWasCompressed) {
                throw new UnsupportedOperationException();
            }

            @Override
            public AssertionStatus getPolicyResult() {
                checkOwnerThread();
                return delegate.getPolicyResult();
            }

            @Override
            public void setPolicyResult(final AssertionStatus policyResult) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isPolicyExecutionAttempted() {
                checkOwnerThread();
                return delegate.isPolicyExecutionAttempted();
            }

            @Override
            public void setPolicyExecutionAttempted(final boolean attempted) {
                throw new UnsupportedOperationException();
            }

            @Nullable
            @Override
            public DebugContext getDebugContext() {
                checkOwnerThread();
                return delegate.getDebugContext();
            }

            @Override
            public void setDebugContext(@Nullable final DebugContext debugContext) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void setAuditContext(@Nullable final AuditContext auditContext) {
                throw new UnsupportedOperationException();
            }

            @Nullable
            @Override
            public AuditContext getAuditContext() {
                checkOwnerThread();
                return delegate.getAuditContext();
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
            public boolean isOverwriteResponseCookiePath() {
                checkOwnerThread();
                return delegate.isOverwriteResponseCookiePath();
            }

            @Override
            public void setOverwriteResponseCookiePath(final boolean overwriteResponseCookiePath) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isOverwriteResponseCookieDomain() {
                checkOwnerThread();
                return delegate.isOverwriteResponseCookieDomain();
            }

            @Override
            public void setOverwriteResponseCookieDomain(final boolean overwriteResponseCookieDomain) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isAuthorizedRequest() {
                checkOwnerThread();
                return delegate.isAuthorizedRequest();
            }

            @Override
            public boolean isCompletedRequest() {
                checkOwnerThread();
                return delegate.isCompletedRequest();
            }
        }
    }

    /**
     * {@link AssertionFinished} implementation.
     */
    private static class AssertionFinishedImpl extends GatewayMetricsEventImpl implements AssertionFinished {
        private final Assertion assertion;
        private final AssertionMetrics assertionMetrics;

        private AssertionFinishedImpl(
                @NotNull final PolicyEnforcementContext context,
                @NotNull final Assertion assertion,
                @NotNull final AssertionMetrics assertionMetrics
        ) {
            super(context);
            this.assertion = assertion;
            this.assertionMetrics = assertionMetrics;
        }

        @NotNull
        @Override
        public AssertionMetrics getAssertionMetrics() {
            return assertionMetrics;
        }

        @NotNull
        @Override
        public Assertion getAssertion() {
            return assertion;
        }
    }

    /**
     * Publish {@link AssertionFinished} event if the specified {@code context} is instance of {@link GatewayMetricsSupport}.
     *
     * @param context             The executing PEC. Cannot be {@code null}
     * @param assertion           Assertion that finished executing. Cannot be {@code null}
     * @param assertionMetrics    Assertion metrics info. Cannot be {@code null}
     */
    public static void publishAssertionFinish(
            @NotNull final PolicyEnforcementContext context,
            @NotNull final ServerAssertion assertion,
            @NotNull final AssertionMetrics assertionMetrics
    ) {
        if (context instanceof GatewayMetricsSupport) {
            final GatewayMetricsPublisher publisher = ((GatewayMetricsSupport) context).getGatewayMetricsEventsPublisher();
            if (publisher != null) {
                publisher.publishEvent(createAssertionFinishedEvent(context, assertion.getAssertion(), assertionMetrics));
            }
        }
    }

    @NotNull
    static AssertionFinished createAssertionFinishedEvent(
            @NotNull final PolicyEnforcementContext context,
            @NotNull final Assertion assertion,
            @NotNull final AssertionMetrics assertionMetrics
    ) {
        return new AssertionFinishedImpl(context, assertion, assertionMetrics);
    }

    /**
     * Used only for unit testing
     */
    @NotNull
    static Class<? extends PolicyEnforcementContext> getPecClass() {
        return GatewayMetricsEventImpl.ReadOnlyPolicyEnforcementContext.class;
    }
}
