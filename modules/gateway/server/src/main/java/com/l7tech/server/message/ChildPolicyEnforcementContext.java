package com.l7tech.server.message;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionMetrics;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.server.message.metrics.GatewayMetricsPublisher;
import com.l7tech.server.message.metrics.GatewayMetricsSupport;
import com.l7tech.server.message.metrics.GatewayMetricsUtils;
import com.l7tech.server.policy.PolicyMetadata;
import com.l7tech.server.policy.assertion.RoutingResultListener;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.stepdebug.DebugContext;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXException;

import javax.wsdl.Binding;
import javax.wsdl.Operation;
import javax.wsdl.WSDLException;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * A PEC that is split between parent/child contexts.
 *
 * <p>Messages, authentication and auditing/fault (configuration) are handled
 * by the parent PEC. The child PEC is responsible for variables, routing and
 * other instance specific duties.</p>
 */
class ChildPolicyEnforcementContext extends PolicyEnforcementContextWrapper implements HasOriginalContext, ShadowsParentVariables, HasOutputVariables, GatewayMetricsSupport {

    //- PUBLIC

    @Override
    public void addSeenAssertionStatus( final AssertionStatus assertionStatus ) {
        context.addSeenAssertionStatus( assertionStatus );
    }

    @Override
    public Set<AssertionStatus> getSeenAssertionStatus() {
        return context.getSeenAssertionStatus();
    }

    @Override
    public PublishedService getService() {
        PublishedService service = context.getService();
        if ( service == null ) {
            service = super.getService();
        }
        return service;
    }

    @Override
    public void setService( final PublishedService service ) {
        context.setService( service );
    }

    @Override
    public Pair<Binding, Operation> getBindingAndOperation() throws IOException, SAXException, WSDLException, InvalidDocumentFormatException {
        return context.getBindingAndOperation();
    }

    @Override
    public List<String> getIncrementedCounters() {
        return context.getIncrementedCounters();
    }

    @Override
    public Message getTargetMessage( final MessageTargetable targetable ) throws NoSuchVariableException {
        switch(targetable.getTarget()) {
            case REQUEST:
                return getRequest();
            case RESPONSE:
                return getResponse();
            default:
                final String varName = targetable.getOtherTargetMessageVariable();
                if (varName != null && isParentVariable(varName)) {
                    return super.getTargetMessage(targetable);
                } else {
                    return context.getTargetMessage( targetable );
                }
        }
    }

    @Override
    public Message getTargetMessage( final MessageTargetable targetable, final boolean allowNonMessageVar ) throws NoSuchVariableException {
        switch(targetable.getTarget()) {
            case REQUEST:
                return getRequest();
            case RESPONSE:
                return getResponse();
            default:
                final String varName = targetable.getOtherTargetMessageVariable();
                if (varName != null && isParentVariable(varName)) {
                    return super.getTargetMessage(targetable, allowNonMessageVar);
                } else {
                    return context.getTargetMessage( targetable, allowNonMessageVar );
                }
        }
    }

    @Override
    public Message getOrCreateTargetMessage( final MessageTargetable targetable, final boolean allowNonMessagevar ) throws NoSuchVariableException, VariableNotSettableException {
        switch(targetable.getTarget()) {
            case REQUEST:
                return getRequest();
            case RESPONSE:
                return getResponse();
            default:
                final String varName = targetable.getOtherTargetMessageVariable();
                if (varName != null && isParentVariable(varName)) {
                    return super.getOrCreateTargetMessage(targetable, allowNonMessagevar);
                } else {
                    return context.getOrCreateTargetMessage( targetable, allowNonMessagevar );
                }
        }
    }

    @Override
    public void setVariable( final String name, final Object value ) throws VariableNotSettableException {
        if (isParentVariable(name)) {
            super.setVariable(name, value);
        } else {
            context.setVariable( name, value );
        }
    }

    @Override
    public Object getVariable( final String name ) throws NoSuchVariableException {
        if (isParentVariable(name)) {
            return super.getVariable(name);
        } else {
            return context.getVariable(name);
        }
    }

    @Override
    public Map<String, Object> getAllVariables() {
        return context.getAllVariables();
    }

    @Override
    public Map<String, Object> getVariableMap( final String[] names, final Audit auditor ) {
        List<String> forChild = new ArrayList<>();
        List<String> forParent = new ArrayList<>();

        for (String name : names) {
            if (isParentVariable(name)) {
                forParent.add(name);
            } else {
                forChild.add(name);
            }
        }

        Map<String, Object> vars = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        vars.putAll(context.getVariableMap(forChild.toArray(new String[forChild.size()]), auditor));
        vars.putAll(super.getVariableMap(forParent.toArray(new String[forParent.size()]), auditor));
        return vars;
    }

    @Override
    public RoutingResultListener getRoutingResultListener() {
        return context.getRoutingResultListener();
    }

    @Override
    public boolean isResponseWss11() {
        return routingMetricsPEC.isResponseWss11();
    }

    @Override
    public void setResponseWss11(boolean wss11) {
        routingMetricsPEC.setResponseWss11(wss11);
    }

    @Override
    public RoutingStatus getRoutingStatus() {
        return routingMetricsPEC.getRoutingStatus();
    }

    @Override
    public void setRoutingStatus( final RoutingStatus routingStatus ) {
        routingMetricsPEC.setRoutingStatus(routingStatus);
    }

    @Override
    public boolean isPostRouting() {
        return routingMetricsPEC.isPostRouting();
    }

    @Override
    public void routingStarted() {
        routingMetricsPEC.routingStarted();
    }

    @Override
    public void routingFinished() {
        routingMetricsPEC.routingFinished();
    }

    @Override
    public long getStartTime() {
        return routingMetricsPEC.getStartTime();
    }

    @Override
    public long getRoutingStartTime() {
        return routingMetricsPEC.getRoutingStartTime();
    }

    @Override
    public long getRoutingEndTime() {
        return routingMetricsPEC.getRoutingEndTime();
    }

    @Override
    public long getRoutingTotalTime() {
        return routingMetricsPEC.getRoutingTotalTime();
    }

    @Override
    public boolean isReplyExpected() {
        return routingMetricsPEC.isReplyExpected();
    }

    @Override
    public boolean isRequestWasCompressed() {
        return routingMetricsPEC.isRequestWasCompressed();
    }

    @Override
    public void setRequestWasCompressed(boolean wasCompressed) {
        routingMetricsPEC.setRequestWasCompressed(wasCompressed);
    }

    @Override
    public URL getRoutedServiceUrl() {
        return routingMetricsPEC.getRoutedServiceUrl();
    }

    @Override
    public void setRoutedServiceUrl( final URL routedServiceUrl ) {
        routingMetricsPEC.setRoutedServiceUrl( routedServiceUrl );
    }

    @Override
    public long getEndTime() {
        return routingMetricsPEC.getEndTime();
    }

    @Override
    public void setEndTime() {
        routingMetricsPEC.setEndTime();
    }

    @Override
    public boolean isOverwriteResponseCookiePath() {
        return routingMetricsPEC.isOverwriteResponseCookiePath();
    }

    @Override
    public void setOverwriteResponseCookiePath(final boolean overwriteResponseCookiePath) {
        routingMetricsPEC.setOverwriteResponseCookiePath(overwriteResponseCookiePath);
    }

    @Override
    public boolean isOverwriteResponseCookieDomain() {
        return routingMetricsPEC.isOverwriteResponseCookieDomain();
    }

    @Override
    public void setOverwriteResponseCookieDomain(final boolean overwriteResponseCookieDomain) {
        routingMetricsPEC.setOverwriteResponseCookieDomain(overwriteResponseCookieDomain);
    }

    public void addRoutingResultListener(RoutingResultListener listener) {
        context.addRoutingResultListener(listener);
    }

    public void removeRoutingResultListener(RoutingResultListener listener) {
        context.removeRoutingResultListener(listener);
    }

    @Override
    public void assertionStarting( final ServerAssertion assertion ) {
        context.assertionStarting( assertion );

        if (debugContext != null) {
            debugContext.onStartAssertion(this, assertion);
        }
    }

    @Override
    public void assertionFinished( final ServerAssertion assertion, final AssertionStatus status, @Nullable final AssertionMetrics assertionMetrics ) {
        context.assertionFinished( assertion, status, assertionMetrics);

        if (debugContext != null) {
            debugContext.onFinishAssertion(this);
        }

        if (assertionMetrics != null) {
            GatewayMetricsUtils.publishAssertionFinish(this, assertion, assertionMetrics);
        }
    }

    @Override
    public List<AssertionResult> getAssertionResults() {
        return context.getAssertionResults();
    }

    @Override
    public boolean isPolicyExecutionAttempted() {
        return context.isPolicyExecutionAttempted();
    }

    @Override
    public void setPolicyExecutionAttempted( final boolean attempted ) {
        context.setPolicyExecutionAttempted( attempted );
    }

    @Override
    public DebugContext getDebugContext() {
        return debugContext;
    }

    @Override
    public void setDebugContext(DebugContext debugContext) {
        this.debugContext = debugContext;
    }

    @Override
    public void pushAssertionOrdinal( final int ordinal ) {
        context.pushAssertionOrdinal( ordinal );
    }

    @Override
    public Collection<Integer> getAssertionOrdinalPath() {
        return context.getAssertionOrdinalPath();
    }

    @Override
    public void passDownAssertionOrdinal(@NotNull final PolicyEnforcementContext child) {
        context.passDownAssertionOrdinal(child);
    }

    @Override
    public Collection<Integer> getAssertionNumber() {
        return context.getAssertionNumber();
    }

    @Override
    public int popAssertionOrdinal() throws NoSuchElementException {
        return context.popAssertionOrdinal();
    }

    @Override
    public PolicyMetadata getCurrentPolicyMetadata() {
        return context.getCurrentPolicyMetadata();
    }

    @Override
    public void setCurrentPolicyMetadata( final PolicyMetadata policyMetadata ) {
        context.setCurrentPolicyMetadata( policyMetadata );
    }

    @Override
    public void setTraceListener( final AssertionTraceListener traceListener ) {
        context.setTraceListener( traceListener );
    }

    @Override
    public boolean hasTraceListener() {
        return context.hasTraceListener();
    }

    @Override
    public AssertionTraceListener getTraceListener() {
        return context.getTraceListener();
    }

    @Override
    public void close() {
        // Do not close super, the parent context is not owned by the child
        context.close();
    }

    @Override
    public long getAssertionLatencyNanos() {
        return context.getAssertionLatencyNanos();
    }

    @Override
    public void setAssertionLatencyNanos(long latency) {
        context.setAssertionLatencyNanos(latency);
    }

    @Override
    public Message getOriginalRequest() {
        return parentContext.getRequest();
    }

    @Override
    public Message getOriginalResponse() {
        return parentContext.getResponse();
    }

    @Override
    public Object getOriginalContextVariable(String name) throws NoSuchVariableException {
        return parentContext.getVariable(name);
    }

    @Override
    public PolicyEnforcementContext getOriginalContext() {
        return parentContext;
    }


    @Override
    public void putParentVariable(@NotNull String variableName, boolean prefixed) {
        final String lowerVar = variableName.toLowerCase();
        passthroughVariables.add(lowerVar);
        if (prefixed) {
            passthroughPrefixes.add(lowerVar + ".");
        }
    }

    @NotNull
    @Override
    public Set<String> getOutputVariableNames() {
        return Collections.unmodifiableSet(outputVariables);
    }

    @Override
    public void addOutputVariableName(@NotNull String variableName) {
        outputVariables.add(variableName);
    }

    @Override
    public void setGatewayMetricsEventsPublisher(@Nullable final GatewayMetricsPublisher publisher) {
        this.gatewayMetricsEventsPublisher = publisher;
    }

    @Nullable
    @Override
    public GatewayMetricsPublisher getGatewayMetricsEventsPublisher() {
        return this.gatewayMetricsEventsPublisher;
    }

    //- PACKAGE

    /**
     * <p>Constructor.</p>
     * <p>
     *     Expects two {@link com.l7tech.server.message.PolicyEnforcementContext PEC}s: one to be the parent,
     *     one to be the child.
     *     You should think of the child context as 'this'.
     *     Most method invocations on this object will simply be forwarded to the child context,
     *     but some will go to the parent:
     *     <ol>
     *        <li>{@link #getOriginalContext()}</li>
     *        <li>{@link #getOriginalContextVariable(String)}</li>
     *        <li>{@link #getOriginalRequest()}</li>
     *        <li>{@link #getOriginalResponse()}</li>
     *     </ol>
     * </p>
     * <p>Additionally, a routing assertion-like behaviour is supported: when set to <code>true</code>, argument
     * <code>passRoutingMetricsToParent</code> will forward the following method calls to the parent context:
     * <ol>
     *     <li>{@link #isResponseWss11()}</li>
     *     <li>{@link #setResponseWss11(boolean)}</li>
     *     <li>{@link #addRoutingResultListener(com.l7tech.server.policy.assertion.RoutingResultListener)}</li>
     *     <li>{@link #removeRoutingResultListener(com.l7tech.server.policy.assertion.RoutingResultListener)}</li>
     *     <li>{@link #getRoutingStatus()}</li>
     *     <li>{@link #setRoutingStatus(com.l7tech.policy.assertion.RoutingStatus)}</li>
     *     <li>{@link #isPostRouting()}</li>
     *     <li>{@link #routingStarted()}</li>
     *     <li>{@link #routingFinished()}</li>
     *     <li>{@link #getStartTime()}</li>
     *     <li>{@link #getRoutingStartTime()}</li>
     *     <li>{@link #getRoutingEndTime()}</li>
     *     <li>{@link #getRoutingTotalTime()}</li>
     *     <li>{@link #isReplyExpected()}</li>
     * </ol>
     * </p>
     * @param parent parent context
     * @param context child (this) context
     * @param passRoutingMetricsToParent whether to pass routing metrics to parent context
     */
    ChildPolicyEnforcementContext( final PolicyEnforcementContext parent,
                                   final PolicyEnforcementContext context,
                                   final boolean passRoutingMetricsToParent) {
        super( parent );
        this.context = context;
        this.parentContext = parent;
        routingMetricsPEC = passRoutingMetricsToParent ? parentContext : context;
        GatewayMetricsUtils.setPublisher(parent, this);
    }

    /**
     * Constructor. Creates an instance that does NOT forward routing metrics to the parent context.
     * Equivalent to calling {@link #ChildPolicyEnforcementContext(PolicyEnforcementContext, PolicyEnforcementContext, boolean)}
     * with a <code>false</code> value in the third argument.
     * @param parent parent context
     * @param context child (this) context
     */
    ChildPolicyEnforcementContext( final PolicyEnforcementContext parent,
                                   final PolicyEnforcementContext context) {
        this(parent, context, false);
    }


    //- PRIVATE

    private boolean isParentVariable(String name) {
        if (name == null)
            return false;

        // TODO move this hardcoded config somewhere more appropriate (get from variable metadata, perhaps)
        final String lcname = name.toLowerCase();
        return passthroughVariables.contains(lcname) ||
                matchesPassthroughPrefix(lcname) ||
                "request".equals(lcname) ||
                "response".equals(lcname) ||
                "service".equals(lcname) ||
                lcname.startsWith("request.") ||
                lcname.startsWith("response.") ||
                lcname.startsWith("service.");
    }

    private boolean matchesPassthroughPrefix(String name) {
        // TODO this algorithm is terrible, replace with a faster one using a smarter data structure
        for (String prefix : passthroughPrefixes) {
            if (name.startsWith(prefix))
                return true;
        }
        return false;
    }

    /** The child (this) context */
    private final PolicyEnforcementContext context;

    /** The parent context */
    private final PolicyEnforcementContext parentContext;

    private final Set<String> passthroughVariables = new HashSet<>();
    private final TreeSet<String> passthroughPrefixes = new TreeSet<>();

    /**
     * Points to the PEC that will receive routing metrics-related calls.
     * If this instance was called using {@link #ChildPolicyEnforcementContext(PolicyEnforcementContext, PolicyEnforcementContext, boolean)}
     * with the third argument set to <code>true</code>, it points to {@link #parentContext}.
     * If not, it points to {@link #context}.
     */
    private final PolicyEnforcementContext routingMetricsPEC;

    private final TreeSet<String> outputVariables = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private DebugContext debugContext = null;

    private @Nullable
    GatewayMetricsPublisher gatewayMetricsEventsPublisher;
}
