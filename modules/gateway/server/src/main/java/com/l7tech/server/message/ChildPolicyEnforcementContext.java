package com.l7tech.server.message;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.server.policy.assertion.RoutingResultListener;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.util.InvalidDocumentFormatException;

import javax.wsdl.Operation;
import javax.wsdl.WSDLException;
import java.util.Set;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.net.URL;
import java.io.IOException;

import org.xml.sax.SAXException;

/**
 * A PEC that is split between parent/child contexts.
 *
 * <p>Messages, authentication and auditing/fault (configuration) are handled
 * by the parent PEC. The child PEC is responsible for variables, routing and
 * other instance specfic duties.</p>
 */
class ChildPolicyEnforcementContext extends PolicyEnforcementContextWrapper {

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
    public Operation getOperation() throws IOException, SAXException, WSDLException, InvalidDocumentFormatException {
        return context.getOperation();
    }

    @Override
    public ArrayList<String> getIncrementedCounters() {
        return context.getIncrementedCounters();
    }

    @Override
    public void setVariable( final String name, final Object value ) throws VariableNotSettableException {
        context.setVariable( name, value );
    }

    @Override
    public Object getVariable( final String name ) throws NoSuchVariableException {
        return context.getVariable( name );
    }

    @Override
    public Map<String, Object> getVariableMap( final String[] names, final Audit auditor ) {
        return context.getVariableMap( names, auditor );
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
    public void assertionFinished( final ServerAssertion assertion, final AssertionStatus status ) {
        context.assertionFinished( assertion, status );
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
    public void runOnClose( final Runnable runMe ) {
        context.runOnClose( runMe );
    }

    @Override
    public void close() {
        // Do not close super, the parent context is not owned by the child
        context.close();
    }

    //- PACKAGE

    ChildPolicyEnforcementContext( final PolicyEnforcementContext parent,
                                   final PolicyEnforcementContext context ) {
        super( parent );
        this.context = context;
    }

    //- PRIVATE

    private final PolicyEnforcementContext context;
}
