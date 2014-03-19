package com.l7tech.server.message;

import com.l7tech.gateway.common.RequestId;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.policy.PolicyMetadata;
import com.l7tech.server.policy.assertion.RoutingResultListener;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.stepdebug.DebugContext;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.Pair;
import com.l7tech.xml.SoapFaultLevel;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXException;

import javax.wsdl.Binding;
import javax.wsdl.Operation;
import javax.wsdl.WSDLException;
import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;

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

    /**
     * @deprecated
     */
    @Deprecated
    Collection<ServerAssertion> getDeferredAssertions();

    /**
     * @deprecated
     */
    @Deprecated
    void addDeferredAssertion(ServerAssertion owner, ServerAssertion decoration);

    /**
     * @deprecated
     */
    @Deprecated
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

    /**
     * Get the metadata for the service policy for this context.
     *
     * @return The service policies metatdata, or null if not available.
     */
    PolicyMetadata getServicePolicyMetadata();

    /**
     * Set the metadata for the service policy for this context.
     *
     * @param policyMetadata service policies metatdata
     * @see #setService(PublishedService)
     */
    void setServicePolicyMetadata( PolicyMetadata policyMetadata );

    /**
     * Get the metadata for the policy (service or include) currently executing on this context.
     * <p/>
     * This will be the top-level metadata of {@link #getService} if no Include assertion is currently
     * executing on this context.  Otherwise, it will be the metadata of the policy of the most recent
     * pending Include.
     *
     * @return metadata for the most recent pending Include, or for the service policy if no Include assertions are currently active on this context.  May be null if no metadata has been set.
     */
    PolicyMetadata getCurrentPolicyMetadata();

    /**
     * Record the metadata for the policy (service or include) now executing on this context.
     * <p/>
     * This is managed by the ServerPolicyHandle and this method should not normally be called from other locations.
     *
     * @param policyMetadata the policy metadata to register, or null to clear any registered policy metadata.
     */
    void setCurrentPolicyMetadata(PolicyMetadata policyMetadata);

    PolicyContextCache getCache();

    void setCache(PolicyContextCache cache);

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
    void setVariable(@Nullable String name, @Nullable Object value) throws VariableNotSettableException;

    /**
     * Get the value of a context variable if it's set, otherwise throw.
     *
     * @param name the name of the variable to get (case-insensitive), ie "requestXpath.result".  Required.
     * @return  the Object representing the value of the specified variable.  May currently be null in some situations
     *          (for example, when a built-in variable returns null, or selects an empty collection, when using a wrapped PEC).
     * @throws NoSuchVariableException  if no value is set for the specified variable
     */
    Object getVariable(String name) throws NoSuchVariableException;

    /**
     * Get all none built-in context variables.
     *
     * @return A map containing none built-in context variable names and values.
     */
    Map<String, Object> getAllVariables();

    /**
     * Get variables Map for named variables in a new mutable case-insensitive map.
     *
     * @param names variables to retrieve
     * @param auditor for any audit messages
     * @return a new mutable map of case-insensitive variable name to value. Never null.
     */
    Map<String, Object> getVariableMap(String[] names, Audit auditor);

    void routingStarted();

    void routingFinished();

    long getRoutingStartTime();

    long getRoutingEndTime();

    /** 
     * @return total duration (in milliseconds) of all routings (e.g., if this policy has multiple routings)
     */
    long getRoutingTotalTime();

    /**
     * Get the Binding and Operation the request is targeted at. May not be found.
     * @return Pair with the Binding on the left and Operation on the right. May be null. If the pair is not null,
     * then the Binding and the Operation will be not null. Once this method has been called, the return value will
     * always be the same for subsequent calls.
     * @throws IOException
     * @throws SAXException
     * @throws WSDLException
     * @throws InvalidDocumentFormatException
     */
    Pair<Binding, Operation> getBindingAndOperation()
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
     * @return the duration (in nanosecond) of the assertion, the value will be overwritten after
     * each assertion execution.
     */
    long getAssertionLatencyNanos();


    /**
     * Set the latency of the assertion
     */
    void setAssertionLatencyNanos(long latency);

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
     * Notify the context that an assertion is about to begin evaluation.
     *
     * @param assertion The ServerAssertion that is to be evaluated. Must not be null.
     */
    void assertionStarting(ServerAssertion assertion);

    /**
     * @param assertion the ServerAssertion that just finished. Must not be null.
     * @param status the AssertionStatus that was returned from the ServerAssertion's checkRequest() method. Must not be null.
     */
    void assertionFinished(ServerAssertion assertion, AssertionStatus status);

    /**
     * A linear log of the results of processing each assertion that was run in the policy.
     */
    List<AssertionResult> getAssertionResults();

    /**
     * Get the assertion number for the currently evaluating assertion.
     *
     * <p>This is only valid during policy evaluation</p>
     *
     * @return The assertion number
     */
    Collection<Integer> getAssertionNumber();

    Message getTargetMessage( MessageTargetable targetable) throws NoSuchVariableException;

    /**
     * Get a target message, create one if not found.
     *
     * <p>If a variable with the name exists but is not a message variable then
     * this method will throw if <code>allowNonMessagevar</code> is <code>false</code>.</p>
     *
     * <p>NOTE: When non message variables are used, the caller MUST NOT initialize the
     * message with a new stash manager (see {@link Message#initialize(com.l7tech.common.mime.StashManager, com.l7tech.common.mime.ContentTypeHeader, java.io.InputStream) Message#initialize}).</p>
     *
     * @param targetable The target message
     * @param allowNonMessagevar True to allow non-message variables
     * @return The message (never null)
     * @throws NoSuchVariableException If the variable is not found or is not a Message and a Message is required
     * @throws VariableNotSettableException If the variable does not exist and cannot be created
     */
    Message getOrCreateTargetMessage( MessageTargetable targetable, boolean allowNonMessagevar ) throws NoSuchVariableException, VariableNotSettableException;

    Message getTargetMessage( MessageTargetable targetable, boolean allowNonMessageVar) throws NoSuchVariableException;

    /**
     * Check if this WS-Security version 1.1 is preferred for secured response messages.
     * <p/>
     * This method will return true if {@link #setResponseWss11(boolean)} has been called with the value
     * <code>true</code> <b>OR</b> if request processor results exist and 
     * {@link com.l7tech.security.xml.processor.ProcessorResult#isWsse11Seen()} returns true.
     *
     * @return true if WS-Security version 1.1 is preferred for message level security in response messages.
     */
    boolean isResponseWss11();

    /**
     * Mark this context as preferring to use (or not) WS-Security version 1.1 for secured response messages.
     *
     * @param wss11 true to prefer WS-Security 1.1
     */
    void setResponseWss11( boolean wss11 );

    void setMalformedRequest();

    boolean isMalformedRequest();

    /**
     * Use to save the L7a:MessageID from the request, if you know it will be needed for the response,
     * to save its having to be looked up later.
     *
     * @param messageId  the L7a:MessageID from the request; or, empty string to record that it didn't contain one; or, null to clear any saved value.
     */
    void setSavedRequestL7aMessageId(String messageId);

    /**
     * Get the save L7a:MessageID from the request, if any.
     *
     * @return the L7a:MessageID from the request, or empty string if it didn't contain one, or null if none was recorded.
     */
    String getSavedRequestL7aMessageId();

    List<MessageContextMapping> getMappings();

    void setMappings(List<MessageContextMapping> mappings);

    /**
     * Get the current assertion ordinal path, in top-down order.  This list is empty unless at least one
     * Include assertion is currently executing as an ancestor of the policy currently being executed.
     * <p/>
     * While a ServerInclude assertion executes, it pushes its ordinal onto the current assertion ordinal path.
     * This can be used to locate the over context in which a particular server assertion is executing.
     * <p/>
     * This path is kept in top-down order; that is, the ordinal of the first ServerInclude executed
     * comes first, and any ServerInclude executed while this first one was executing comes next.
     * <p/>
     * Example showing assertion paths as they are when checkRequest() is called (and returns) on the following:
     * <pre>
     *    - All                         { }
     *      - True                      { }
     *      - Include: Policy A         { }
     *        - True                    { 3 }
     *        - Include: Policy B       { 3 }
     *          - True                  { 3, 2 }
     *          - HTTP Basic            { 3, 2 }
     *          - Include: Policy C     { 3, 2 }
     *            - True                { 3, 2, 3 }
     *          - True                  { 3, 2 }
     *        - True                    { 3 }
     *      - True
     *      - Include: Policy C         { }
     *        - True                    { 5 }
     *      - Include: Policy D         { }
     *        - Include: Policy E       { 6 }
     *          - True                  { 6, 1 }
     *        - True                    { 6 }
     *      - True                      { }
     * </pre>
     *
     * @return the assertion ordinal path, ie { 3, 2, 3 }, in which the current policy fragment is executing.  May be empty but never null.
     */
    Collection<Integer> getAssertionOrdinalPath();

    /**
     * Push a new assertion ordinal onto the assertion ordinal path.
     * <p/>This should typically be invoked only by the ServerInclude assertion.
     *
     * @param ordinal the ordinal to add to the path.
     */
    void pushAssertionOrdinal(int ordinal);

    /**
     * Remove the last assertion ordinal from the path.
     * <p/>This should typically be invoked only by the ServerInclude assertion.
     *
     * @return the ordinal removed.
     * @throws NoSuchElementException if the assertion ordinal path is empty.
     */
    int popAssertionOrdinal() throws NoSuchElementException;

    void setTraceListener(AssertionTraceListener traceListener);

    boolean hasTraceListener();

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

    /**
     * Returns the debug context.
     *
     * @return the debug context, or null.
     */
    @Nullable DebugContext getDebugContext();

    /**
     * Sets the debug context.
     *
     * @param debugContext the debug context. Can be null.
     */
    void setDebugContext(@Nullable DebugContext debugContext);

    /**
     * Record an AuditContext that is/was associated with the processing of this PolicyEnforcementContext.
     * <p/>
     * Server assertions should not call this method.
     *
     * @param auditContext audit context to record, or null.
     */
    void setAuditContext(@Nullable AuditContext auditContext);

    /**
     * Get the AuditContext, if any, that is/was associated with the processing of this PolicyEnforcementContext.
     * <p/>
     * Server assertions should normally not attempt to use the audit context directly.
     *
     * @return audit context, or null.  May have already been flushed, if the MessageProcessor has already returned.
     */
    @Nullable AuditContext getAuditContext();

    /**
     * Add a task to the queue of tasks to perform in order when this context is closed.
     *
     * @param runMe the task to run.  Required.
     */
    void runOnClose( Runnable runMe );

    /**
     * Prepend a task to the queue of tasks to perform in order when this context is closed, so that
     * the new task runs before all the other tasks.
     * <p/>
     * Callers should generally avoid this method unless they have extremely specific requirements.
     * If there is any doubt, use {@link #runOnClose} instead.
     *
     * @param runMe the task to run.  Required.
     */
    public void runOnCloseFirst( Runnable runMe );

    /**
     * Call this when you are done using a PEC. This is important for PolicyEnforcementContext#close() to
     * function properly.
     */
    @Override
    void close();

    /**
     * @return true if response cookies should have their path overwritten with the gateway path or false if the
     * response cookies should retain their original path (ie. for a reverse-proxy).
     */
    boolean isOverwriteResponseCookiePath();

    /**
     * @param overwriteResponseCookiePath set to true if response cookies should have their path overwritten with the gateway path.
     */
    void setOverwriteResponseCookiePath(final boolean overwriteResponseCookiePath);

    /**
     * @return true if response cookies should have their domain overwritten with the gateway domain or false if the
     * response cookies should retain their original domain (ie. for a reverse-proxy).
     */
    boolean isOverwriteResponseCookieDomain();

    /**
     * @param overwriteResponseCookieDomain set to true if response cookies should have their domain overwritten with the gateway domain.
     */
    void setOverwriteResponseCookieDomain(final boolean overwriteResponseCookieDomain);

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
