package com.l7tech.server.message;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.gateway.common.RequestId;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.policy.PolicyMetadata;
import com.l7tech.server.policy.assertion.RoutingResultListener;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.Pair;
import com.l7tech.xml.SoapFaultLevel;
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
 * Provides an implementation of the PolicyEnforcementContext interface that can be subclassed for adaption.
 *
 * <p>This class implements the Wrapper or Decorator pattern. Methods default
 * to calling through to the wrapped request object.</p>
 */
public class PolicyEnforcementContextWrapper implements PolicyEnforcementContext {

    //- PUBLIC
    
    @Override
    public Message getRequest() {
        return delegate.getRequest();
    }

    @Override
    public Message getResponse() {
        return delegate.getResponse();
    }

    @Override
    public AuthenticationContext getDefaultAuthenticationContext() {
        return delegate.getDefaultAuthenticationContext();
    }

    @Override
    public AuthenticationContext getAuthenticationContext( final Message message ) {
        return delegate.getAuthenticationContext( message );
    }

    @Override
    public RoutingStatus getRoutingStatus() {
        return delegate.getRoutingStatus();
    }

    @Override
    public void setRoutingStatus( final RoutingStatus routingStatus ) {
        delegate.setRoutingStatus( routingStatus );
    }

    @Override
    public boolean isPostRouting() {
        return delegate.isPostRouting();
    }

    @Override
    public boolean isReplyExpected() {
        return delegate.isReplyExpected();
    }

    @Override
    public Level getAuditLevel() {
        return delegate.getAuditLevel();
    }

    @Override
    public void setAuditLevel( final Level auditLevel ) {
        delegate.setAuditLevel( auditLevel );
    }

    @Override
    public RequestId getRequestId() {
        return delegate.getRequestId();
    }

    @Override
    public void addSeenAssertionStatus( final AssertionStatus assertionStatus ) {
        delegate.addSeenAssertionStatus( assertionStatus );
    }

    @Override
    public Set<AssertionStatus> getSeenAssertionStatus() {
        return delegate.getSeenAssertionStatus();
    }

    @Override
    public boolean isAuditSaveRequest() {
        return delegate.isAuditSaveRequest();
    }

    @Override
    public void setAuditSaveRequest( final boolean auditSaveRequest ) {
        delegate.setAuditSaveRequest( auditSaveRequest );
    }

    @Override
    public boolean isAuditSaveResponse() {
        return delegate.isAuditSaveResponse();
    }

    @Override
    public void setAuditSaveResponse( final boolean auditSaveResponse ) {
        delegate.setAuditSaveResponse( auditSaveResponse );
    }

    /**
     * @return an empty list (deferred assertions not supported)
     */
    @Override
    @Deprecated
    public Collection<ServerAssertion> getDeferredAssertions() {
        return Collections.emptyList();
    }

    /**
     * @throws UnsupportedOperationException On any use
     */
    @Override
    @Deprecated
    public void addDeferredAssertion( final ServerAssertion owner, final ServerAssertion decoration ) {
        throw new UnsupportedOperationException();
    }

    /**
     * This implementation does nothing (deferred assertions not supported)
     */
    @Override
    @Deprecated
    public void removeDeferredAssertion( final ServerAssertion owner ) {
    }

    @Override
    public RoutingResultListener getRoutingResultListener() {
        return delegate.getRoutingResultListener();
    }

    @Override
    public void addRoutingResultListener( final RoutingResultListener listener ) {
        delegate.addRoutingResultListener( listener );
    }

    @Override
    public void removeRoutingResultListener( final RoutingResultListener listener ) {
        delegate.removeRoutingResultListener( listener );
    }

    @Override
    public void setAuthenticationMissing() {
        delegate.setAuthenticationMissing();
    }

    @Override
    public boolean isRequestPolicyViolated() {
        return delegate.isRequestPolicyViolated();
    }

    @Override
    public void setRequestPolicyViolated() {
        delegate.setRequestPolicyViolated();
    }

    @Override
    public void setRequestClaimingWrongPolicyVersion() {
        delegate.setRequestClaimingWrongPolicyVersion();
    }

    @Override
    public boolean isRequestClaimingWrongPolicyVersion() {
        return delegate.isRequestClaimingWrongPolicyVersion();
    }

    @Override
    public PublishedService getService() {
        return delegate.getService();
    }

    @Override
    public void setService( final PublishedService service ) {
        delegate.setService( service );
    }

    @Override
    public PolicyMetadata getServicePolicyMetadata() {
        return delegate.getServicePolicyMetadata();
    }

    @Override
    public void setServicePolicyMetadata( final PolicyMetadata policyMetadata ) {
        delegate.setServicePolicyMetadata( policyMetadata );
    }

    @Override
    public PolicyMetadata getCurrentPolicyMetadata() {
        return delegate.getCurrentPolicyMetadata();
    }

    @Override
    public void setCurrentPolicyMetadata(PolicyMetadata policyMetadata) {
        delegate.setCurrentPolicyMetadata( policyMetadata );
    }

    @Override
    public PolicyContextCache getCache() {
        return delegate.getCache();
    }

    @Override
    public void setCache( final PolicyContextCache cache ) {
        delegate.setCache( cache );
    }

    @Override
    public Set<HttpCookie> getCookies() {
        return delegate.getCookies();
    }

    @Override
    public void addCookie( final HttpCookie cookie ) {
        delegate.addCookie( cookie );
    }

    @Override
    public void deleteCookie(HttpCookie cookie) {
        delegate.deleteCookie(cookie);
    }

    @Override
    public ArrayList<String> getIncrementedCounters() {
        return delegate.getIncrementedCounters();
    }

    @Override
    public Object getVariable(String name) throws NoSuchVariableException {
        final Object value;

        if ( isBuiltinVariable(name) ) {
            value = getBuiltinVariable(name);
        } else {
            value = delegate.getVariable(name);
        }

        return value;
    }

    @Override
    public void setVariable(String name, Object value) throws VariableNotSettableException {
        if (name == null) return;
        if (isBuiltinVariable(name)) {
            try {
                setBuiltinVariable(name, value);
            } catch (NoSuchVariableException e) {
                throw new RuntimeException("Variable '" + name + "' is supposedly supported, but doesn't exist", e);
            }
        } else {
            delegate.setVariable( name, value );
        }
    }

    @Override
    public Map<String, Object> getVariableMap(String[] names, Audit auditor) {
        final Map<String,Object> vars = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        final List<String> variableNames = new ArrayList<String>();

        for (String name : names) {
            if ( isBuiltinVariable(name) ) {
                try {
                    Object value = getBuiltinVariable(name);
                    vars.put( name, value );
                } catch (NoSuchVariableException e) {
                    auditor.logAndAudit( AssertionMessages.NO_SUCH_VARIABLE, name);
                }
            } else {
                variableNames.add( name );
            }
        }

        vars.putAll( delegate.getVariableMap( variableNames.toArray( new String[variableNames.size()] ), auditor ) );

        return vars;
    }

    @Override
    public void routingStarted() {
        delegate.routingStarted();
    }

    @Override
    public void routingFinished() {
        delegate.routingFinished();
    }

    @Override
    public long getRoutingStartTime() {
        return delegate.getRoutingStartTime();
    }

    @Override
    public long getRoutingEndTime() {
        return delegate.getRoutingEndTime();
    }

    @Override
    public long getRoutingTotalTime() {
        return delegate.getRoutingTotalTime();
    }

    @Override
    public Pair<Binding, Operation> getBindingAndOperation() throws IOException, SAXException, WSDLException, InvalidDocumentFormatException {
        return delegate.getBindingAndOperation();
    }

    @Override
    public boolean isStealthResponseMode() {
        return delegate.isStealthResponseMode();
    }

    @Override
    public long getStartTime() {
        return delegate.getStartTime();
    }

    @Override
    public long getAssertionLatencyNanos() {
        return delegate.getAssertionLatencyNanos();
    }

    @Override
    public void setAssertionLatencyNanos(long latency) {
        delegate.setAssertionLatencyNanos(latency);
    }

    @Override
    public URL getRoutedServiceUrl() {
        return delegate.getRoutedServiceUrl();
    }

    @Override
    public void setRoutedServiceUrl( final URL routedServiceUrl ) {
        delegate.setRoutedServiceUrl( routedServiceUrl );
    }

    @Override
    public long getEndTime() {
        return delegate.getEndTime();
    }

    @Override
    public void setEndTime() {
        delegate.setEndTime();
    }

    @Override
    public SoapFaultLevel getFaultlevel() {
        return delegate.getFaultlevel();
    }

    @Override
    public void setFaultlevel( final SoapFaultLevel faultlevel ) {
        delegate.setFaultlevel( faultlevel );
    }

    @Override
    public void assertionStarting( final ServerAssertion assertion ) {
        delegate.assertionStarting( assertion );
    }

    @Override
    public void assertionFinished( final ServerAssertion assertion, final AssertionStatus status ) {
        delegate.assertionFinished( assertion, status );
    }

    @Override
    public List<AssertionResult> getAssertionResults() {
        return delegate.getAssertionResults();
    }

    @Override
    public Collection<Integer> getAssertionNumber() {
        return delegate.getAssertionNumber();
    }

    @Override
    public Message getTargetMessage( final MessageTargetable targetable ) throws NoSuchVariableException {
        return delegate.getTargetMessage( targetable );
    }

    @Override
    public Message getOrCreateTargetMessage( final MessageTargetable targetable, final boolean allowNonMessagevar ) throws NoSuchVariableException, VariableNotSettableException {
        return delegate.getOrCreateTargetMessage( targetable, allowNonMessagevar );
    }

    @Override
    public Message getTargetMessage( final MessageTargetable targetable, final boolean allowNonMessageVar ) throws NoSuchVariableException {
        return delegate.getTargetMessage( targetable, allowNonMessageVar );
    }

    @Override
    public boolean isResponseWss11() {
        return delegate.isResponseWss11();
    }

    @Override
    public void setResponseWss11( final boolean wss11 ) {
        delegate.setResponseWss11( wss11 );
    }

    @Override
    public void setMalformedRequest() {
        delegate.setMalformedRequest();
    }

    @Override
    public boolean isMalformedRequest() {
        return delegate.isMalformedRequest();
    }

    @Override
    public void setSavedRequestL7aMessageId(String messageId) {
        delegate.setSavedRequestL7aMessageId(messageId);
    }

    @Override
    public String getSavedRequestL7aMessageId() {
        return delegate.getSavedRequestL7aMessageId();
    }

    @Override
    public List<MessageContextMapping> getMappings() {
        return delegate.getMappings();
    }

    @Override
    public void setMappings( final List<MessageContextMapping> mappings ) {
        delegate.setMappings( mappings );
    }

    @Override
    public Collection<Integer> getAssertionOrdinalPath() {
        return delegate.getAssertionOrdinalPath();
    }

    @Override
    public void pushAssertionOrdinal(int ordinal) {
        delegate.pushAssertionOrdinal(ordinal);
    }

    @Override
    public int popAssertionOrdinal() throws NoSuchElementException {
        return delegate.popAssertionOrdinal();
    }

    @Override
    public void setTraceListener(AssertionTraceListener traceListener) {
        delegate.setTraceListener(traceListener);
    }

    @Override
    public boolean isRequestWasCompressed() {
        return delegate.isRequestWasCompressed();
    }

    @Override
    public void setRequestWasCompressed( final boolean requestWasCompressed ) {
        delegate.setRequestWasCompressed( requestWasCompressed );
    }

    @Override
    public AssertionStatus getPolicyResult() {
        return delegate.getPolicyResult();
    }

    @Override
    public void setPolicyResult( final AssertionStatus policyResult ) {
        delegate.setPolicyResult( policyResult );
    }

    @Override
    public boolean isPolicyExecutionAttempted() {
        return delegate.isPolicyExecutionAttempted();
    }

    @Override
    public void setPolicyExecutionAttempted( final boolean attempted ) {
        delegate.setPolicyExecutionAttempted( attempted );
    }

    @Override
    public void setAuditContext(@Nullable AuditContext auditContext) {
        delegate.setAuditContext(auditContext);
    }

    @Override
    public AuditContext getAuditContext() {
        return delegate.getAuditContext();
    }

    @Override
    public void runOnClose( final Runnable runMe ) {
        delegate.runOnClose( runMe );
    }

    @Override
    public void runOnCloseFirst(Runnable runMe) {
        delegate.runOnCloseFirst( runMe );
    }

    @Override
    public void close() {
        delegate.close();
    }

    //- PROTECTED

    protected PolicyEnforcementContextWrapper( final PolicyEnforcementContext delegate ) {
        this.delegate = delegate;            
    }

    //- PRIVATE

    private PolicyEnforcementContext delegate;
        
    private boolean isBuiltinVariable(String name) {
        return BuiltinVariables.isSupported(name) && ServerVariables.isValidForContext(name, this);
    }

    private void setBuiltinVariable(String name, Object value) throws NoSuchVariableException {
        ServerVariables.set(name, value, this);
    }

    private Object getBuiltinVariable(String name) throws NoSuchVariableException {
        return ServerVariables.get(name, this);
    }

}
