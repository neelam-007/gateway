package com.l7tech.server.message;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.server.policy.PolicyMetadata;
import com.l7tech.server.policy.assertion.RoutingResultListener;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.stepdebug.DebugContext;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
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
class ChildPolicyEnforcementContext extends PolicyEnforcementContextWrapper implements HasOriginalContext, ShadowsParentVariables, HasOutputVariables {

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
    public ArrayList<String> getIncrementedCounters() {
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
    public void addRoutingResultListener( final RoutingResultListener listener ) {
        context.addRoutingResultListener( listener );
    }

    @Override
    public void removeRoutingResultListener( final RoutingResultListener listener ) {
        context.removeRoutingResultListener( listener );
    }

    @Override
    public RoutingStatus getRoutingStatus() {
        return context.getRoutingStatus();
    }

    @Override
    public void setRoutingStatus( final RoutingStatus routingStatus ) {
        context.setRoutingStatus( routingStatus );
    }

    @Override
    public boolean isPostRouting() {
        return context.isPostRouting();
    }

    @Override
    public void routingStarted() {
        context.routingStarted();
    }

    @Override
    public void routingFinished() {
        context.routingFinished();
    }

    @Override
    public long getRoutingStartTime() {
        return context.getRoutingStartTime();
    }

    @Override
    public long getRoutingEndTime() {
        return context.getRoutingEndTime();
    }

    @Override
    public long getRoutingTotalTime() {
        return context.getRoutingTotalTime();
    }

    @Override
    public URL getRoutedServiceUrl() {
        return context.getRoutedServiceUrl();
    }

    @Override
    public void setRoutedServiceUrl( final URL routedServiceUrl ) {
        context.setRoutedServiceUrl( routedServiceUrl );
    }

    @Override
    public long getEndTime() {
        return context.getEndTime();
    }

    @Override
    public void setEndTime() {
        context.setEndTime();
    }

    @Override
    public void assertionStarting( final ServerAssertion assertion ) {
        context.assertionStarting( assertion );

        if (debugContext != null) {
            debugContext.onStartAssertion(this, assertion);
        }
    }

    @Override
    public void assertionFinished( final ServerAssertion assertion, final AssertionStatus status ) {
        context.assertionFinished( assertion, status );

        if (debugContext != null) {
            debugContext.onFinishAssertion(this);
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

    //- PACKAGE

    ChildPolicyEnforcementContext( final PolicyEnforcementContext parent,
                                   final PolicyEnforcementContext context ) {
        super( parent );
        this.context = context;
        this.parentContext = parent;
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

    private final PolicyEnforcementContext context;
    private final PolicyEnforcementContext parentContext;
    private final Set<String> passthroughVariables = new HashSet<>();
    private final TreeSet<String> passthroughPrefixes = new TreeSet<>();
    private final TreeSet<String> outputVariables = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private DebugContext debugContext = null;
}
