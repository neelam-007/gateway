/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.message;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.RequestId;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.message.ProcessingContext;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.server.RequestIdGenerator;
import com.l7tech.server.policy.assertion.CompositeRoutingResultListener;
import com.l7tech.server.policy.assertion.RoutingResultListener;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.Pair;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.xml.SoapFaultLevel;
import com.l7tech.xml.soap.SoapUtil;
import org.xml.sax.SAXException;

import javax.wsdl.Operation;
import javax.wsdl.WSDLException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;

/**
 * Holds message processing state needed by policy enforcement server (SSG) message processor and policy assertions.
 * TODO write some farking javadoc
 */
class PolicyEnforcementContextImpl extends ProcessingContext<AuthenticationContext> implements PolicyEnforcementContext {
    private final long startTime = System.currentTimeMillis();
    private long endTime;
    private final RequestId requestId;
    private ArrayList<String> incrementedCounters = new ArrayList<String>();
    private final Map<ServerAssertion,ServerAssertion> deferredAssertions = new LinkedHashMap<ServerAssertion, ServerAssertion>();
    private boolean replyExpected;
    private Level auditLevel;
    private boolean auditSaveRequest;
    private boolean auditSaveResponse;
    private SoapFaultLevel faultlevel = null;
    private boolean isRequestPolicyViolated = false;
    private boolean isRequestClaimingWrongPolicyVersion = false;
    private PublishedService service;
    private Set<HttpCookie> cookies = new LinkedHashSet<HttpCookie>();
    private Set<AssertionStatus> seenAssertionStatus = new HashSet<AssertionStatus>();
    private final Map<String,Object> variables = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
    private PolicyContextCache cache;
    private CompositeRoutingResultListener routingResultListener = new CompositeRoutingResultListener();
    private boolean operationAttempted = false;
    private Operation cachedOperation = null;
    private RoutingStatus routingStatus = RoutingStatus.NONE;
    private URL routedServiceUrl;
    private long routingStartTime;
    private long routingEndTime;
    private long routingTotalTime;
    private AssertionStatus policyoutcome;
    private List<MessageContextMapping> mappings = new ArrayList<MessageContextMapping>(5);
    private boolean requestWasCompressed;
    private boolean responseWss11;
    private boolean malformedRequest;
    private boolean policyExecutionAttempted;

    protected PolicyEnforcementContextImpl(Message request, Message response) {
        super(request, response);
        this.requestId = RequestIdGenerator.next();
    }

    @Override
    protected AuthenticationContext buildContext() {
        return new AuthenticationContext();
    }

    @Override
    public RoutingStatus getRoutingStatus() {
        return routingStatus;
    }

    @Override
    public void setRoutingStatus(RoutingStatus routingStatus) {
        this.routingStatus = routingStatus;
    }

    /**
     * Check if this context's routing status is ROUTED or ATTEMPTED.
     *
     * @return true iff. this context routing status is ROUTED or ATTEMPTED.
     */
    @Override
    public boolean isPostRouting() {
        return RoutingStatus.ROUTED.equals(getRoutingStatus()) || RoutingStatus.ATTEMPTED.equals(getRoutingStatus());
    }

    @Override
    public boolean isReplyExpected() {
        return replyExpected;
    }

    public void setReplyExpected(boolean replyExpected) {
        this.replyExpected = replyExpected;
    }

    @Override
    public Level getAuditLevel() {
        return auditLevel;
    }

    @Override
    public void setAuditLevel(Level auditLevel) {
        this.auditLevel = auditLevel;
    }

    @Override
    public RequestId getRequestId() {
        return requestId;
    }

    @Override
    public void addSeenAssertionStatus(AssertionStatus assertionStatus) {
        if(assertionStatus==null) throw new NullPointerException("assertionStatus must not be null");
        seenAssertionStatus.add(assertionStatus);
    }

    @Override
    public Set<AssertionStatus> getSeenAssertionStatus() {
        return Collections.unmodifiableSet(seenAssertionStatus);
    }

    @Override
    public boolean isAuditSaveRequest() {
        return auditSaveRequest;
    }

    @Override
    public void setAuditSaveRequest(boolean auditSaveRequest) {
        this.auditSaveRequest = auditSaveRequest;
    }

    @Override
    public boolean isAuditSaveResponse() {
        return auditSaveResponse;
    }

    @Override
    public void setAuditSaveResponse(boolean auditSaveResponse) {
        this.auditSaveResponse = auditSaveResponse;
    }

    @Override
    public Collection<ServerAssertion> getDeferredAssertions() {
        return deferredAssertions.values();
    }

    @Override
    public void addDeferredAssertion(ServerAssertion owner, ServerAssertion decoration) {
        deferredAssertions.put(owner, decoration);
    }

    @Override
    public void removeDeferredAssertion(ServerAssertion owner) {
        deferredAssertions.remove(owner);
    }

    @Override
    public RoutingResultListener getRoutingResultListener() {
        return routingResultListener;
    }

    @Override
    public void addRoutingResultListener(RoutingResultListener listener) {
        routingResultListener.addListener(listener);
    }

    @Override
    public void removeRoutingResultListener(RoutingResultListener listener) {
        routingResultListener.removeListener(listener);
    }

    @Override
    public void setAuthenticationMissing() {
        super.getDefaultAuthenticationContext().setAuthenticationMissing();
        setRequestPolicyViolated();
    }

    /**
     * Check if a policy violation was detected while processing the request.
     * If the policy processing turns out to fail, a Policy-URL: should be sent back
     * to the requestor.
     * @return true if the request is considered to be in violation of the policy
     */
    @Override
    public boolean isRequestPolicyViolated() {
        return isRequestPolicyViolated;
    }

    /**
     * Note that a policy violation was detected while processing the request.
     * If the policy processing turns out to fail, a Policy-URL: should be sent back
     * to the requestor.
     */
    @Override
    public void setRequestPolicyViolated() {
        isRequestPolicyViolated = true;
    }

    /**
     * This means that the requestor included a policy version in the request header (bridge)
     * and the version number was wrong. This is slightly different from policy violated because
     * a policy could be violated even if the client has the right policy.
     */
    @Override
    public void setRequestClaimingWrongPolicyVersion() {
        isRequestClaimingWrongPolicyVersion = true;
    }

    @Override
    public boolean isRequestClaimingWrongPolicyVersion() {
        return isRequestClaimingWrongPolicyVersion;
    }

    @Override
    public void setService(PublishedService service) {
        this.service = service;
    }

    @Override
    public PublishedService getService() {
        return service;
    }

    @Override
    public void setCache(PolicyContextCache cache) {
        this.cache = cache;
    }

    @Override
    public PolicyContextCache getCache() {
        return cache;
    }

    @Override
    public Set<HttpCookie> getCookies() {
        return Collections.unmodifiableSet(cookies);
    }

    @Override
    public void addCookie(HttpCookie cookie) {
        Set<HttpCookie> toRemove = new HashSet<HttpCookie>();
        for (HttpCookie currentCookie : cookies) {
            if (currentCookie.getCookieName().equals(cookie.getCookieName())) {
                toRemove.add(currentCookie);
            }
        }
        cookies.removeAll(toRemove);
        cookies.add(cookie);
    }

    @Override
    public ArrayList<String> getIncrementedCounters() {
        return incrementedCounters;
    }

    /**
     * Sets the value of a new or existing context variable.
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
            if (variables.containsKey(name)) {
                variables.remove(name); // So that case change in name will cause map key to be updated as well.
            }
            variables.put(name, value);
        }
    }

    protected boolean isBuiltinVariable(String name) {
        return BuiltinVariables.isSupported(name) && ServerVariables.isValidForContext(name, this);
    }

    protected void setBuiltinVariable(String name, Object value) throws NoSuchVariableException {
        ServerVariables.set(name, value, this);
    }

    protected Object getBuiltinVariable(String name) throws NoSuchVariableException {
        return ServerVariables.get(name, this);
    }

    /**
     * Get the value of a context variable if it's set, otherwise throw.
     *
     * @param name the name of the variable to get (case-insensitive), ie "requestXpath.result".  Required.
     * @return  the Object representing the value of the specified variable.  Never null.
     * @throws NoSuchVariableException  if no value is set for the specified variable
     */
    @Override
    public Object getVariable(String name) throws NoSuchVariableException {
        final Object value;

        if (isBuiltinVariable(name)) {
            value = getBuiltinVariable(name);
        } else {
            value = variables.get(name);
        }

        if (value == null) throw new NoSuchVariableException(name);

        return value;
    }

    /**
     * Get the value of a context variable, with name resolution
     *
     * @param inName the name of the variable to get (case-insensitive), ie "requestXpath.result".  Required.
     * @return a Pair containing the matched variable name and the variable value; never null but the variable value can be null
     * @throws NoSuchVariableException if the given name does not resolve to any variable
     */
    private Pair<String, Object> getVariableWithNameLookup(String inName) throws NoSuchVariableException {
        String outName = inName;
        final Object value;

        if (isBuiltinVariable(inName)) {
            value = getBuiltinVariable(inName);
        } else {
            String mname = Syntax.getMatchingName(inName, variables.keySet());
            if (mname != null) {
                outName = mname;
                value = variables.get(mname);
            } else {
                throw new NoSuchVariableException(inName);
            }
        }

        return new Pair<String, Object>(outName, value);
    }

    @Override
    public Map<String, Object> getVariableMap(String[] names, Audit auditor) {
        Map<String, Object> vars = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        for (String name : names) {
            try {
                final Pair<String, Object> tuple = getVariableWithNameLookup(name);
                vars.put(tuple.left, tuple.right);
                if (tuple.right == null) {
                    auditor.logAndAudit(AssertionMessages.VARIABLE_IS_NULL, name);
                }
            } catch (NoSuchVariableException e) {
                auditor.logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, name);
            }
        }
        return vars;
    }

    @Override
    public void routingStarted() {
        this.routingStartTime = System.currentTimeMillis();
    }

    @Override
    public void routingFinished() {
        this.routingEndTime = System.currentTimeMillis();
        routingTotalTime += routingEndTime - routingStartTime;
    }

    @Override
    public long getRoutingStartTime() {
        return routingStartTime;
    }

    @Override
    public long getRoutingEndTime() {
        return routingEndTime;
    }

    /** @return total duration (in milliseconds) of all routings (e.g., if this policy has multiple routings) */
    @Override
    public long getRoutingTotalTime() {
        return routingTotalTime;
    }

    @Override
    public Operation getOperation()
            throws IOException, SAXException, WSDLException, InvalidDocumentFormatException
    {
        if (operationAttempted)
            return cachedOperation;

        operationAttempted = true;
        if (service == null || service.getWsdlXml() == null || service.getWsdlXml().length() <= 0) {
            return null;
        }

        Wsdl wsdl = service.parsedWsdl();
        cachedOperation = SoapUtil.getOperation(wsdl, getRequest());
        return cachedOperation;
    }

    /**
     * Whether or not the transport layer should send back a response at all.
     *
     * <p>Note this is depends on the FaultLevel that is set.</p>
     *
     * @return true means the requestor's connection should be dropped completly
     */
    @Override
    public boolean isStealthResponseMode() {
        return faultlevel!=null && faultlevel.getLevel() == SoapFaultLevel.DROP_CONNECTION;
    }

    /**
     * @return the time when this request's processing started
     */
    @Override
    public long getStartTime() {
        return startTime;
    }

    /**
     * Gets the last URL to which the SSG <em>attempted</em> to send this request.
     *
     * @see #getRoutingStatus to find out whether the routing was successful.
     */
    @Override
    public URL getRoutedServiceUrl() {
        return routedServiceUrl;
    }

    @Override
    public void setRoutedServiceUrl(URL routedServiceUrl) {
        this.routedServiceUrl = routedServiceUrl;
    }

    @Override
    public long getEndTime() {
        return endTime;
    }

    @Override
    public void setEndTime() {
        if (endTime != 0) throw new IllegalStateException("Can't call setEndTime() twice");
        endTime = System.currentTimeMillis();
    }

    /**
     * tells the SSG what the soap fault returned to a requestor should look like
     * when a policy evaluation fails. If not set by the policy, will return null.
     * 
     * @return the fault level or null for default
     */
    @Override
    public SoapFaultLevel getFaultlevel() {
        return faultlevel;
    }

    /**
     * tells the SSG what the soap fault returned to a requestor should look like
     * when a policy evaluation fails
     */
    @Override
    public void setFaultlevel(SoapFaultLevel faultlevel) {
        this.faultlevel = faultlevel;
    }

    private final Map<ServerAssertion, AssertionStatus> assertionStatuses = new LinkedHashMap<ServerAssertion, AssertionStatus>();
    private LinkedList<AssertionResult> assertionResultList;

    /**
     * @param assertion the ServerAssertion that just finished. Must not be null.
     * @param status the AssertionStatus that was returned from the ServerAssertion's checkRequest() method. Must not be null.
     */
    @Override
    public void assertionFinished(ServerAssertion assertion, AssertionStatus status) {
        if (assertion == null || status == null) throw new NullPointerException();
        assertionStatuses.put(assertion, status);
    }

    /**
     * A linear log of the results of processing each assertion that was run in the policy.
     */
    @Override
    public List<AssertionResult> getAssertionResults() {
        if (assertionResultList == null) {
            assertionResultList = new LinkedList<AssertionResult>();
            for (Map.Entry<ServerAssertion, AssertionStatus> entry : assertionStatuses.entrySet()) {
                ServerAssertion serverAssertion = entry.getKey();
                AssertionStatus status = entry.getValue();
                AssertionResult trace = new AssertionResult(serverAssertion.getAssertion(), status, serverAssertion);
                assertionResultList.add(trace);
            }
        }
        return assertionResultList;
    }

    @Override
    public Message getTargetMessage(final MessageTargetable targetable) throws NoSuchVariableException {
        return getTargetMessage(targetable, false);
    }

    @Override
    public Message getTargetMessage(final MessageTargetable targetable, boolean allowNonMessageVar) throws NoSuchVariableException {
        switch(targetable.getTarget()) {
            case REQUEST:
                return getRequest();

            case RESPONSE:
                return getResponse();

            case OTHER:
                final String variableName = targetable.getOtherTargetMessageVariable();
                if (variableName == null)
                    throw new IllegalArgumentException("Target is OTHER but no variable name was set");

                final Object value = getVariable(variableName);

                if (value == null)
                    throw new NoSuchVariableException(variableName, "Variable value is null");

                if (value instanceof Message)
                    return (Message) value;

                if (!allowNonMessageVar)
                    throw new NoSuchVariableException(variableName,
                            MessageFormat.format("Request message source (\"{0}\") is a context variable of the wrong type (expected={1}, actual={2}).",
                                    variableName, Message.class, value.getClass()));

                return createContextVariableBackedMessage(variableName, value.toString());

            default:
                throw new IllegalArgumentException("Unsupported message target: " + targetable.getTarget());
        }
    }

    /**
     * Check if this WS-Security version 1.1 is preferred for secured response messages.
     * <p/>
     * This method will return true if {@link #setResponseWss11()} has been called <b>OR</b> if request processor
     * results exist and {@link ProcessorResult#isWsse11Seen()} returns
     * true.
     *
     * @return true if WS-Security version 1.1 is preferred for message level security in response messages.
     */
    @Override
    public boolean isResponseWss11() {
        if (responseWss11)
            return true;
        ProcessorResult pr = getRequest().getSecurityKnob().getProcessorResult();
        return pr != null && pr.isWsse11Seen();

    }

    /**
     * Mark this context as preferring to use WS-Security version 1.1 for secured response messages.
     */
    @Override
    public void setResponseWss11() {
        this.responseWss11 = true;
    }

    private Message createContextVariableBackedMessage(final String variableName, String initialValue) {
        Message mess = new Message();
        final ContentTypeHeader ctype = ContentTypeHeader.TEXT_DEFAULT;
        try {
            final ContextVariableKnob cvk = new ContextVariableKnob(variableName);

            StashManager sm = new ByteArrayStashManager() {
                @Override
                public void stash(int ordinal, byte[] in, int offset, int length) {
                    super.stash(ordinal, in, offset, length);
                    if (ordinal != 0) // Probably won't happen but you never knob 
                        return;

                    // Write back the modified context variable
                    try {
                        String encoding = ctype.getEncoding();
                        if (cvk.getOverrideEncoding() != null)
                            encoding = cvk.getOverrideEncoding();
                        final String val = new String(in, offset, length, encoding);
                        if (!val.equals(getVariable(variableName)))
                            setVariable(variableName, val);
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e); // can't happen
                    } catch (NoSuchVariableException e) {
                        throw new RuntimeException(e); // Normally not possible
                    }
                }
            };

            mess.initialize(sm, ctype, new ByteArrayInputStream(initialValue.getBytes(ctype.getEncoding())));
            mess.attachKnob(ContextVariableKnob.class, cvk);
            return mess;
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    @Override
    public void setMalformedRequest() {
        this.malformedRequest = true;
    }

    @Override
    public boolean isMalformedRequest() {
        return malformedRequest;
    }

    /** @param policyTried true when the MessageProcessor gets as far as calling checkRequest() for this context. */
    @Override
    public void setPolicyExecutionAttempted(boolean policyTried) {
        this.policyExecutionAttempted = policyTried;
    }

    /** @return true if the MessageProcessor got as far as calling checkRequest() for this context. */
    @Override
    public boolean isPolicyExecutionAttempted() {
        return policyExecutionAttempted;
    }

    @Override
    public AssertionStatus getPolicyResult() {
        return policyoutcome;
    }

    @Override
    public void setPolicyResult(AssertionStatus policyoutcome) {
        this.policyoutcome = policyoutcome;
    }

    @Override
    public List<MessageContextMapping> getMappings() {
        return mappings;
    }

    @Override
    public void setMappings(List<MessageContextMapping> mappings) {
        this.mappings = mappings;
    }

    @Override
    public boolean isRequestWasCompressed() {
        return requestWasCompressed;
    }

    @Override
    public void setRequestWasCompressed(boolean requestWasCompressed) {
        this.requestWasCompressed = requestWasCompressed;
    }
}
