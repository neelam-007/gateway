package com.l7tech.server.message;

import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.RoutingResultListener;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.gateway.common.RequestId;
import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.SoapFaultLevel;
import com.l7tech.message.Message;

import javax.wsdl.Operation;
import javax.wsdl.WSDLException;
import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.Set;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.net.URL;

import org.xml.sax.SAXException;

/**
 * Holds message processing state needed by policy enforcement server (SSG) message processor and policy assertions.
 */
public interface PolicyEnforcementContext extends Closeable {

    Message getRequest();

    Message getResponse();

    AuthenticationContext getDefaultAuthenticationContext();

    AuthenticationContext getAuthenticationContext( Message message );

    RoutingStatus getRoutingStatus();

    void setRoutingStatus(RoutingStatus routingStatus);

    /**
     * Check if this context's routing status is ROUTED or ATTEMPTED.
     *
     * @return true iff. this context routing status is ROUTED or ATTEMPTED.
     */
    boolean isPostRouting();

    boolean isReplyExpected();

    Level getAuditLevel();

    void setAuditLevel(Level auditLevel);

    RequestId getRequestId();

    void addSeenAssertionStatus( AssertionStatus assertionStatus);

    Set<AssertionStatus> getSeenAssertionStatus();

    boolean isAuditSaveRequest();

    void setAuditSaveRequest(boolean auditSaveRequest);

    boolean isAuditSaveResponse();

    void setAuditSaveResponse(boolean auditSaveResponse);

    @Deprecated
    /**
     * @deprecated
     */
    Collection<ServerAssertion> getDeferredAssertions();

    @Deprecated
    /**
     * @deprecated
     */
    void addDeferredAssertion(ServerAssertion owner, ServerAssertion decoration);

    @Deprecated
    /**
     * @deprecated
     */
    void removeDeferredAssertion(ServerAssertion owner);

    RoutingResultListener getRoutingResultListener();

    void addRoutingResultListener(RoutingResultListener listener);

    void removeRoutingResultListener(RoutingResultListener listener);

    void setAuthenticationMissing();

    /**
     * Check if a policy violation was detected while processing the request.
     *
     * <p>If the policy processing turns out to fail, a Policy-URL: should be sent back
     * to the requestor.</p>
     *
     * @return true if the request is considered to be in violation of the policy
     */
    boolean isRequestPolicyViolated();

    /**
     * Note that a policy violation was detected while processing the request.
     *
     * <p>If the policy processing turns out to fail, a Policy-URL: should be sent back
     * to the requestor.</p>
     */
    void setRequestPolicyViolated();

    /**
     * This means that the requestor included a policy version in the request header (bridge)
     * and the version number was wrong. This is slightly different from policy violated because
     * a policy could be violated even if the client has the right policy.
     */
    void setRequestClaimingWrongPolicyVersion();

    boolean isRequestClaimingWrongPolicyVersion();

    /**
     * Get the associated published service for the policy.
     *
     * @return the service or null if unknown 
     */
    PublishedService getService();

    void setService(PublishedService service);

    PolicyContextCache getCache();

    void setCache(PolicyContextCache cache);

    Set<HttpCookie> getCookies();

    void addCookie(HttpCookie cookie);

    ArrayList<String> getIncrementedCounters();

    /**
     * Sets the value of a new or existing context variable.
     * 
     * Note that context variable names are case-preserving upon storage, but
     * case-insensitive upon lookup.
     * If the name passed in is existing but with character case change, the new
     * spelling will be adopted.
     * <p/>
     * This method is more expensive than you might assume.  If your intent is to make a built-in
     * variable available, consider adding it to BuiltinVariables instead.  That way it won't cost anything
     * until someone attempts to use it.
     *
     * @param name the name of the variable to set.  if null, do nothing.
     * @param value may be null.
     * @throws VariableNotSettableException if the variable is known, but not settable.
     */
    void setVariable(String name, Object value) throws VariableNotSettableException;

    /**
     * Get the value of a context variable if it's set, otherwise throw.
     *
     * @param name the name of the variable to get (case-insensitive), ie "requestXpath.result".  Required.
     * @return  the Object representing the value of the specified variable.  Never null.
     * @throws NoSuchVariableException  if no value is set for the specified variable
     */
    Object getVariable(String name) throws NoSuchVariableException;

    Map<String, Object> getVariableMap(String[] names, Audit auditor);

    void routingStarted();

    void routingFinished();

    long getRoutingStartTime();

    long getRoutingEndTime();

    /** 
     * @return total duration (in milliseconds) of all routings (e.g., if this policy has multiple routings)
     */
    long getRoutingTotalTime();

    Operation getOperation()
            throws IOException, SAXException, WSDLException, InvalidDocumentFormatException;

    /**
     * Whether or not the transport layer should send back a response at all.
     *
     * <p>Note this is depends on the FaultLevel that is set.</p>
     *
     * @return true means the requestor's connection should be dropped completly
     */
    boolean isStealthResponseMode();

    /**
     * @return the time when this request's processing started
     */
    long getStartTime();

    /**
     * Gets the last URL to which the SSG <em>attempted</em> to send this request.
     *
     * @see #getRoutingStatus to find out whether the routing was successful.
     */
    URL getRoutedServiceUrl();

    void setRoutedServiceUrl(URL routedServiceUrl);

    long getEndTime();

    void setEndTime();

    /**
     * tells the SSG what the soap fault returned to a requestor should look like
     * when a policy evaluation fails. If not set by the policy, will return null.
     *
     * @return the fault level or null for default
     */
    SoapFaultLevel getFaultlevel();

    /**
     * tells the SSG what the soap fault returned to a requestor should look like
     * when a policy evaluation fails
     */
    void setFaultlevel(SoapFaultLevel faultlevel);

    /**
     * @param assertion the ServerAssertion that just finished. Must not be null.
     * @param status the AssertionStatus that was returned from the ServerAssertion's checkRequest() method. Must not be null.
     */
    void assertionFinished(ServerAssertion assertion, AssertionStatus status);

    /**
     * A linear log of the results of processing each assertion that was run in the policy.
     */
    List<AssertionResult> getAssertionResults();

    Message getTargetMessage( MessageTargetable targetable) throws NoSuchVariableException;

    Message getTargetMessage( MessageTargetable targetable, boolean allowNonMessageVar) throws NoSuchVariableException;

    /**
     * Check if this WS-Security version 1.1 is preferred for secured response messages.
     * <p/>
     * This method will return true if {@link #setResponseWss11()} has been called <b>OR</b> if request processor
     * results exist and {@link com.l7tech.security.xml.processor.ProcessorResult#isWsse11Seen()} returns
     * true.
     *
     * @return true if WS-Security version 1.1 is preferred for message level security in response messages.
     */
    boolean isResponseWss11();

    /**
     * Mark this context as preferring to use WS-Security version 1.1 for secured response messages.
     */
    void setResponseWss11();

    void setMalformedRequest();

    boolean isMalformedRequest();

    List<MessageContextMapping> getMappings();

    void setMappings(List<MessageContextMapping> mappings);

    boolean isRequestWasCompressed();

    void setRequestWasCompressed(boolean requestWasCompressed);

    AssertionStatus getPolicyResult();

    void setPolicyResult(AssertionStatus policyResult);

    /**
     * @return true if the MessageProcessor got as far as calling checkRequest() for this context.
     */
    boolean isPolicyExecutionAttempted();

    /**
     * @param attempted true when the MessageProcessor gets as far as calling checkRequest() for this context.
     */
    void setPolicyExecutionAttempted(boolean attempted);

    void runOnClose( Runnable runMe );

    /**
     * Call this when you are done using a PEC. This is important for PolicyEnforcementContext#close() to
     * function properly.
     */
    @Override
    void close();

    final static class AssertionResult {
        private final Assertion assertion;
        private final AssertionStatus status;
        private final Object detailsKey;

        protected AssertionResult( final Assertion assertion,
                                   final AssertionStatus status,
                                   final Object detailsKey ) {
            this.assertion = assertion;
            this.status = status;
            this.detailsKey = detailsKey;
        }

        public Assertion getAssertion() {
            return assertion;
        }

        public AssertionStatus getStatus() {
            return status;
        }

        public Object getDetailsKey() {
            return detailsKey;
        }
    }
}
