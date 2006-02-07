/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.message;

import com.l7tech.common.RequestId;
import com.l7tech.common.audit.AuditContext;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.ProcessingContext;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionResult;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.policy.variable.VariableMap;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.server.RequestIdGenerator;
import com.l7tech.server.policy.assertion.CompositeRoutingResultListener;
import com.l7tech.server.policy.assertion.RoutingResultListener;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.service.PublishedService;
import org.xml.sax.SAXException;

import javax.wsdl.Operation;
import javax.wsdl.WSDLException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds message processing state needed by policy enforcement server (SSG) message processor and policy assertions.
 * TODO write some farking javadoc
 */
public class PolicyEnforcementContext extends ProcessingContext {
    private static final Logger logger = Logger.getLogger(PolicyEnforcementContext.class.getName());

    private final RequestId requestId;
    private ArrayList incrementedCounters = new ArrayList();
    private final Map deferredAssertions = new LinkedHashMap();
    private boolean replyExpected;
    private RoutingStatus routingStatus = RoutingStatus.NONE;
    private User authenticatedUser;
    private boolean authenticated;
    private Level auditLevel;
    private boolean auditSaveRequest;
    private boolean auditSaveResponse;
    private List assertionResults = Collections.EMPTY_LIST;
    private SoapFaultDetail faultDetail = null;
    private boolean isAuthenticationMissing = false;
    private boolean isRequestPolicyViolated = false;
    private PublishedService service;
    private Set cookies = new LinkedHashSet();
    private AuditContext auditContext = null;
    private final Map variables = new HashMap();
    private long routingStartTime;
    private long routingEndTime;
    private boolean isStealthResponseMode = false;
    private PolicyContextCache cache;
    private CompositeRoutingResultListener routingResultListener = new CompositeRoutingResultListener();

    public PolicyEnforcementContext(Message request, Message response) {
        super(request, response);
        this.requestId = RequestIdGenerator.next();
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public User getAuthenticatedUser() {
        return authenticatedUser;
    }

    public AuditContext getAuditContext() {
        return auditContext;
    }

    public void setAuditContext(AuditContext auditContext) {
        this.auditContext = auditContext;
    }

    public void setAuthenticatedUser(User authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
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

    public Collection getDeferredAssertions() {
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

    public void addResult(AssertionResult result) {
        if (assertionResults == Collections.EMPTY_LIST)
            assertionResults = new ArrayList();
        assertionResults.add(result);
    }

    public Iterator results() {
        return assertionResults.iterator();
    }

    public SoapFaultDetail getFaultDetail() {
        return faultDetail;
    }

    public void setFaultDetail(SoapFaultDetail faultDetail) {
        this.faultDetail = faultDetail;
    }

    public List getAssertionResults() {
        return assertionResults;
    }

    public void setAssertionResults(List assertionResults) {
        this.assertionResults = assertionResults;
    }

    /**
     * Check if some authentication credentials that were expected in the request were not found.
     * This implies {@link #setRequestPolicyViolated}, as well.
     */
    public boolean isAuthenticationMissing() {
        return isAuthenticationMissing;
    }

    /**
     * Report that some authentication credentials that were expected in the request were not found.
     * This implies requestPolicyViolated, as well.
     */
    public void setAuthenticationMissing() {
        isAuthenticationMissing = true;
        setRequestPolicyViolated();
    }

    /**
     * Check if a policy violation was detected while processing the request.
     * If the policy processing turns out to fail, a Policy-URL: should be sent back
     * to the requestor.
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

    public Set getCookies() {
        return Collections.unmodifiableSet(cookies);
    }

    public void addCookie(HttpCookie cookie) {
        Set toRemove = new HashSet();
        for(Iterator ci=cookies.iterator(); ci.hasNext(); ) {
            HttpCookie currentCookie = (HttpCookie) ci.next();
            if(currentCookie.getCookieName().equals(cookie.getCookieName())) {
                toRemove.add(currentCookie);
            }
        }
        cookies.removeAll(toRemove);
        cookies.add(cookie);
    }

    public ArrayList getIncrementedCounters() {
        return incrementedCounters;
    }

    public void setVariable(String name, Object value) throws VariableNotSettableException {
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

    public VariableMap getVariableMap(String[] names, Auditor auditor) {
        VariableMap vars = new VariableMap();
        for (int i = 0; i < names.length; i++) {
            try {
                vars.put(names[i], getVariable(names[i].toLowerCase()));
            } catch (NoSuchVariableException e) {
                vars.addBadName(names[i]);
                auditor.logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, new String[] {names[i]});
            }
        }
        return vars;
    }

    public void routingStarted() {
        this.routingStartTime = System.currentTimeMillis();
    }

    public void routingFinished() {
        this.routingEndTime = System.currentTimeMillis();
    }

    public long getRoutingStartTime() {
        return routingStartTime;
    }

    public long getRoutingEndTime() {
        return routingEndTime;
    }

    public Operation getOperation()
            throws IOException, SAXException, WSDLException, InvalidDocumentFormatException
    {
        if (operationAttempted)
            return cachedOperation;

        operationAttempted = true;
        PublishedService service = getService();
        if (service == null || service.getWsdlXml() == null || service.getWsdlXml().length() <= 0) {
            return null;
        }

        Wsdl wsdl = service.parsedWsdl();
        cachedOperation = SoapUtil.getOperation(wsdl, getRequest());
        return cachedOperation;
    }

    /**
     * Whether or not the transport layer should send back a response at all.
     * @return true means the requestor's connection should be dropped completly
     */
    public boolean isStealthResponseMode() {
        return isStealthResponseMode;
    }

    /**
     * This tells the transport layer to not send back a response at all even if a response has been constructed.
     * @param stealthResponseMode true means the requestor's connection should be dropped completly
     */
    public void setStealthResponseMode(boolean stealthResponseMode) {
        isStealthResponseMode = stealthResponseMode;
    }

    private boolean operationAttempted = false;
    private Operation cachedOperation = null;
}
