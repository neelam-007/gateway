/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.message;

import com.l7tech.cluster.ClusterPropertyManager;
import com.l7tech.common.RequestId;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.AuditDetail;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.ProcessingContext;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.SoapFaultLevel;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.server.RequestIdGenerator;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.identity.AuthCache;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.policy.assertion.CompositeRoutingResultListener;
import com.l7tech.server.policy.assertion.RoutingResultListener;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.service.PublishedService;
import org.xml.sax.SAXException;

import javax.wsdl.Operation;
import javax.wsdl.WSDLException;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;

/**
 * Holds message processing state needed by policy enforcement server (SSG) message processor and policy assertions.
 * TODO write some farking javadoc
 */
public class PolicyEnforcementContext extends ProcessingContext {
    private final long startTime = System.currentTimeMillis();
    private long endTime;
    private final RequestId requestId;
    private ArrayList<String> incrementedCounters = new ArrayList<String>();
    private final Map<ServerAssertion,ServerAssertion> deferredAssertions = new LinkedHashMap<ServerAssertion, ServerAssertion>();
    private boolean replyExpected;
    private AuthenticationResult lastAuthenticationResult = null;
    private List<AuthenticationResult> authenticationResults = new ArrayList<AuthenticationResult>();
    private Level auditLevel;
    private boolean auditSaveRequest;
    private boolean auditSaveResponse;
    private SoapFaultLevel faultlevel = null;
    private boolean isRequestPolicyViolated = false;
    private boolean isRequestClaimingWrongPolicyVersion = false;
    private PublishedService service;
    private Set<HttpCookie> cookies = new LinkedHashSet<HttpCookie>();
    private Set<AssertionStatus> seenAssertionStatus = new HashSet<AssertionStatus>();
    private AuditContext auditContext = null;
    private SoapFaultManager soapFaultManager = null;
    private final Map<String,Object> variables = new HashMap<String, Object>();
    private int authSuccessCacheTime = AuthCache.SUCCESS_CACHE_TIME;
    private int authFailureCacheTime = AuthCache.FAILURE_CACHE_TIME;
    private PolicyContextCache cache;
    private CompositeRoutingResultListener routingResultListener = new CompositeRoutingResultListener();
    private boolean operationAttempted = false;
    private Operation cachedOperation = null;
    private ClusterPropertyManager clusterPropertyManager = null;

    private RoutingStatus routingStatus = RoutingStatus.NONE;
    private URL routedServiceUrl;
    private long routingStartTime;
    private long routingEndTime;
    private long routingTotalTime;
    private AssertionStatus policyoutcome;
    private static ThreadLocal<PolicyEnforcementContext> instanceHolder = new ThreadLocal<PolicyEnforcementContext>();

    private static class VariableInfo {
        private final Object value;
        private final VariableMetadata meta;

        private VariableInfo(Object value, VariableMetadata meta) {
            this.value = value;
            this.meta = meta;
        }
    }

    public PolicyEnforcementContext(Message request, Message response) {
        super(request, response);
        this.requestId = RequestIdGenerator.next();
        setInstance();
    }

    private void setInstance() {
        instanceHolder.set(this);
    }

    /**
     * There is one PolicyEnforcementContext per request sent to the SSG. This allows the caller to
     * retrieve the current PEC for the thread which is this is being called from.
     * @see PolicyEnforcementContext#close()
     * @return the last PEC constructed in the current thread, null if none
     */
    public static PolicyEnforcementContext getCurrent() {
        return instanceHolder.get();
    }

    /**
     * Call this when you are done using a PEC. This is important for PolicyEnforcementContext#close() to
     * function properly.
     */
    public void close() {
        instanceHolder.set(null);
        super.close();
    }

    public boolean isAuthenticated() {
        return lastAuthenticationResult != null;
    }

    public void addAuthenticationResult(AuthenticationResult authResult) {
        if (!authenticationResults.contains(authResult)) {
            authenticationResults.add(authResult);
        }
        this.lastAuthenticationResult = authResult;
    }

    public List<AuthenticationResult> getAllAuthenticationResults() {
        return authenticationResults;
    }

    public AuthenticationResult getLastAuthenticationResult() {
        return lastAuthenticationResult;
    }

    public User getLastAuthenticatedUser() {
        return lastAuthenticationResult == null ? null : lastAuthenticationResult.getUser();
    }

    public AuditContext getAuditContext() {
        return auditContext;
    }

    public void setSoapFaultManager(SoapFaultManager soapFaultManager) {
        this.soapFaultManager = soapFaultManager;
    }

    public ClusterPropertyManager getClusterPropertyManager() {
        return clusterPropertyManager;
    }

    public void setClusterPropertyManager(ClusterPropertyManager clusterPropertyManager) {
        this.clusterPropertyManager = clusterPropertyManager;
    }

    public void setAuditContext(AuditContext auditContext) {
        this.auditContext = auditContext;
        /*if (auditContext != null) {
            auditContext.setPec(this);
        } else {
            Logger.getLogger("com.l7tech.blah").severe("unexpected audit context " + auditContext);
        }*/
    }

    public RoutingStatus getRoutingStatus() {
        return routingStatus;
    }

    public void setRoutingStatus(RoutingStatus routingStatus) {
        this.routingStatus = routingStatus;
    }

    public boolean isReplyExpected() {
        return replyExpected;
    }

    public void setReplyExpected(boolean replyExpected) {
        this.replyExpected = replyExpected;
    }

    public Level getAuditLevel() {
        return auditLevel;
    }

    public void setAuditLevel(Level auditLevel) {
        this.auditLevel = auditLevel;
    }

    public RequestId getRequestId() {
        return requestId;
    }

    public void addSeenAssertionStatus(AssertionStatus assertionStatus) {
        if(assertionStatus==null) throw new NullPointerException("assertionStatus must not be null");
        seenAssertionStatus.add(assertionStatus);
    }

    public Set<AssertionStatus> getSeenAssertionStatus() {
        return Collections.unmodifiableSet(seenAssertionStatus);
    }

    public boolean isAuditSaveRequest() {
        return auditSaveRequest;
    }

    public void setAuditSaveRequest(boolean auditSaveRequest) {
        this.auditSaveRequest = auditSaveRequest;
    }

    public boolean isAuditSaveResponse() {
        return auditSaveResponse;
    }

    public void setAuditSaveResponse(boolean auditSaveResponse) {
        this.auditSaveResponse = auditSaveResponse;
    }

    public Collection<ServerAssertion> getDeferredAssertions() {
        return deferredAssertions.values();
    }

    public void addDeferredAssertion(ServerAssertion owner, ServerAssertion decoration) {
        deferredAssertions.put(owner, decoration);
    }

    public void removeDeferredAssertion(ServerAssertion owner) {
        deferredAssertions.remove(owner);
    }

    public RoutingResultListener getRoutingResultListener() {
        return routingResultListener;
    }

    public void addRoutingResultListener(RoutingResultListener listener) {
        routingResultListener.addListener(listener);
    }

    public void removeRoutingResultListener(RoutingResultListener listener) {
        routingResultListener.removeListener(listener);
    }

    public void setAuthenticationMissing() {
        super.setAuthenticationMissing();
        setRequestPolicyViolated();
    }

    /**
     * Check if a policy violation was detected while processing the request.
     * If the policy processing turns out to fail, a Policy-URL: should be sent back
     * to the requestor.
     * @return true if the request is considered to be in violation of the policy
     */
    public boolean isRequestPolicyViolated() {
        return isRequestPolicyViolated;
    }

    /**
     * Note that a policy violation was detected while processing the request.
     * If the policy processing turns out to fail, a Policy-URL: should be sent back
     * to the requestor.
     */
    public void setRequestPolicyViolated() {
        isRequestPolicyViolated = true;
    }

    /**
     * This means that the requestor included a policy version in the request header (bridge)
     * and the version number was wrong. This is slightly different from policy violated because
     * a policy could be violated even if the client has the right policy.
     */
    public void setRequestClaimingWrongPolicyVersion() {
        isRequestClaimingWrongPolicyVersion = true;
    }

    public boolean isRequestClaimingWrongPolicyVersion() {
        return isRequestClaimingWrongPolicyVersion;
    }

    public void setService(PublishedService service) {
        this.service = service;
    }

    public PublishedService getService() {
        return service;
    }

    public void setCache(PolicyContextCache cache) {
        this.cache = cache;
    }

    public PolicyContextCache getCache() {
        return cache;
    }

    public Set<HttpCookie> getCookies() {
        return Collections.unmodifiableSet(cookies);
    }

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

    public ArrayList<String> getIncrementedCounters() {
        return incrementedCounters;
    }

    /**
     * @param name the name of the variable to set.  if null, do nothing.
     * @param value may be null.
     * @throws VariableNotSettableException if the variable is known, but not settable.
     */
    public void setVariable(String name, Object value) throws VariableNotSettableException {
        if (name == null) return;
        if (BuiltinVariables.isSupported(name)) {
            try {
                ServerVariables.set(name, value, this);
            } catch (NoSuchVariableException e) {
                throw new RuntimeException("Variable '" + name + "' is supposedly supported, but doesn't exist", e);
            }
        } else {
            variables.put(name.toLowerCase(), value);
        }
    }

    /**
     * Get the value of a context variable.
     *  
     * @param name the name of the variable to get, ie "requestXpath.result".  Required.
     * @return  the Object representing the value of the specified variable.  Never null.
     * @throws NoSuchVariableException  if no value is set for the specified variable
     */
    public Object getVariable(String name) throws NoSuchVariableException {
        Object value;
        if (BuiltinVariables.isSupported(name)) {
            try {
                value = ServerVariables.get(name, this);
            } catch (NoSuchVariableException e) {
                throw new RuntimeException("Variable '" + name + "' is supposedly supported, but doesn't exist", e);
            }
        } else {
            value = variables.get(name.toLowerCase());
        }

        if (value == null) throw new NoSuchVariableException(name);

        return value;
    }

    public Map<String, Object> getVariableMap(String[] names, Auditor auditor) {
        Map<String, Object> vars = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        for (String name : names) {
            try {
                vars.put(name, getVariable(name));
            } catch (NoSuchVariableException e) {
                auditor.logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, name);
            }
        }
        return vars;
    }

    public void routingStarted() {
        this.routingStartTime = System.currentTimeMillis();
    }

    public void routingFinished() {
        this.routingEndTime = System.currentTimeMillis();
        routingTotalTime += routingEndTime - routingStartTime;
    }

    public long getRoutingStartTime() {
        return routingStartTime;
    }

    public long getRoutingEndTime() {
        return routingEndTime;
    }

    /** @return total duration (in milliseconds) of all routings (e.g., if this policy has multiple routings) */
    public long getRoutingTotalTime() {
        return routingTotalTime;
    }

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
    public boolean isStealthResponseMode() {
        return faultlevel!=null && faultlevel.getLevel() == SoapFaultLevel.DROP_CONNECTION;
    }

    /**
     * @return the time when this request's processing started
     */
    public long getStartTime() {
        return startTime;
    }

    public int getAuthSuccessCacheTime() {
        return authSuccessCacheTime;
    }

    public int getAuthFailureCacheTime() {
        return authFailureCacheTime;
    }

    /**
     * Gets the last URL to which the SSG <em>attempted</em> to send this request.
     *
     * @see #getRoutingStatus to find out whether the routing was successful.
     */
    public URL getRoutedServiceUrl() {
        return routedServiceUrl;
    }

    public void setRoutedServiceUrl(URL routedServiceUrl) {
        this.routedServiceUrl = routedServiceUrl;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime() {
        if (endTime != 0) throw new IllegalStateException("Can't call setEndTime() twice");
        endTime = System.currentTimeMillis();
    }

    /**
     * tells the SSG what the soap fault returned to a requestor should look like
     * when a policy evaluation fails. If not set by the policy, will return the system
     * defaults.
     * @return should never return null
     */
    public SoapFaultLevel getFaultlevel() {
        if (faultlevel == null) {
            faultlevel = soapFaultManager.getDefaultBehaviorSettings();
        }
        return faultlevel;
    }

    /**
     * tells the SSG what the soap fault returned to a requestor should look like
     * when a policy evaluation fails
     */
    public void setFaultlevel(SoapFaultLevel faultlevel) {
        this.faultlevel = faultlevel;
    }

    private final Map<ServerAssertion, AssertionStatus> assertionStatuses = new LinkedHashMap<ServerAssertion, AssertionStatus>();
    private LinkedList<AssertionResult> assertionResultList;

    /**
     * @param assertion the ServerAssertion that just finished. Must not be null.
     * @param status the AssertionStatus that was returned from the ServerAssertion's checkRequest() method. Must not be null.
     */
    public void assertionFinished(ServerAssertion assertion, AssertionStatus status) {
        if (assertion == null || status == null) throw new NullPointerException();
        assertionStatuses.put(assertion, status);
    }

    /**
     * A linear log of the results of processing each assertion that was run in the policy.
     */
    public List<AssertionResult> getAssertionResults(AuditContext auditContext) {
        Map<Object, List<AuditDetail>> detailMap = auditContext.getDetails();
        if (assertionResultList == null) {
            assertionResultList = new LinkedList<AssertionResult>();
            for (Map.Entry<ServerAssertion, AssertionStatus> entry : assertionStatuses.entrySet()) {
                ServerAssertion serverAssertion = entry.getKey();
                AssertionStatus status = entry.getValue();
                List<AuditDetail> assertionDetails = detailMap.get(serverAssertion);
                AssertionResult trace = new AssertionResult(serverAssertion.getAssertion(), status, assertionDetails);
                assertionResultList.add(trace);
            }
        }
        return assertionResultList;
    }

    public final static class AssertionResult {
        private final Assertion assertion;
        private final AssertionStatus status;
        private final List<AuditDetail> details;

        private AssertionResult(Assertion assertion, AssertionStatus status, List<AuditDetail> details) {
            this.assertion = assertion;
            this.status = status;
            this.details = details;
        }

        public Assertion getAssertion() {
            return assertion;
        }

        public AssertionStatus getStatus() {
            return status;
        }

        public List<AuditDetail> getDetails() {
            return details;
        }
    }

    public AssertionStatus getPolicyResult() {
        return policyoutcome;
    }

    public void setPolicyResult(AssertionStatus policyoutcome) {
        this.policyoutcome = policyoutcome;
    }
}
