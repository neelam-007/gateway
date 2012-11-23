package com.l7tech.server.transport.http;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.gateway.common.transport.http.HttpAdmin;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpPassthroughRule;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.audit.AuditContextFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.ServerHttpRoutingAssertion;
import com.l7tech.util.IOUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.ByteArrayInputStream;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Admin class for testing methods of @{link HttpRoutingAssertion}
 *
 * @author rraquepo
 */
public class HttpAdminImpl implements HttpAdmin, ApplicationContextAware {
    private static final Logger logger = Logger.getLogger(HttpAdminImpl.class.getName());
    private ApplicationContext appCtx;

    @Override
    public String testConnection(final String[] serverUrls, final String testMessage, final HttpRoutingAssertion assertion) throws HttpAdminException {
        if (serverUrls == null || serverUrls.length == 0) {
            throw new HttpAdminException("HttpAdminException: serverUrls param is empty", null, null);
        }
        final StringBuffer sessionLog = new StringBuffer();
        final StringBuffer responseStr = new StringBuffer();
        boolean hasError = false;
        try {
            //simulate the request using the real server assertion
            final Message testRequestVariable = new Message(new ByteArrayStashManager(), ContentTypeHeader.parseValue(getConfiguredContentType(assertion)), new ByteArrayInputStream(testMessage.getBytes()));
            testRequestVariable.getMimeKnob();
            for (String serverUrl : serverUrls) {
                try {
                    assertion.setProtectedServiceUrl(serverUrl);//make sure we are testing one URL at a time, this forces it
                    assertion.setRequestMsgSrc("testRequestMessage");
                    assertion.setCustomURLs(null);
                    final PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message(), true);
                    pec.setVariable("testRequestMessage",testRequestVariable);
                    final ServerHttpRoutingAssertion serverAssertion = new ServerHttpRoutingAssertion(assertion, getApplicationContext());
                    sessionLog.append("\"");
                    sessionLog.append(serverUrl);
                    sessionLog.append("\"\n");

                    pec.setAuditLevel(Level.ALL);

                    //we need a custom audit to capture audit infos
                    KidResult kid = AuditContextFactory.doWithCustomAuditContext(new DetailCollectingAuditContext(), new Callable<KidResult>() {
                        @Override
                        public KidResult call() throws Exception {
                            AuditContext auditContext = AuditContextFactory.getCurrent();
                            AssertionStatus status = serverAssertion.checkRequest(pec);
                            Map<Object, List<AuditDetail>> details = auditContext.getDetails();
                            return new KidResult(status, details);
                        }
                    });

                    //gather all audit details so we can display it to the user
                    for (List<AuditDetail> details : kid.details.values()) {
                        if (details != null) {
                            for (AuditDetail detail : details) {
                                int messageId = detail.getMessageId();
                                final StringBuffer messageBuffer = new StringBuffer();
                                final AuditDetailMessage message = MessagesUtil.getAuditDetailMessageById(messageId);
                                final MessageFormat mf = new MessageFormat((message == null ? null : message.getMessage()));
                                mf.format(detail.getParams(), messageBuffer, new FieldPosition(0));
                                if (AssertionMessages.HTTPROUTE_RESPONSE_BADSTATUS.getId() == messageId || AssertionMessages.HTTPROUTE_RESPONSE_NOCONTENTTYPE.getId() == messageId
                                        || AssertionMessages.HTTPROUTE_RESPONSE_NOXML.getId() == messageId) {
                                    sessionLog.append("URL is valid but ");
                                }
                                sessionLog.append(messageBuffer.toString());
                                sessionLog.append("\n");
                            }
                        }
                    }
                    Message response = pec.getResponse();
                    byte[] message = IOUtils.slurpStream(response.getMimeKnob().getEntireMessageBodyAsInputStream());
                    responseStr.append("\"");
                    responseStr.append(serverUrl);
                    responseStr.append("\"\n");
                    responseStr.append("------------------------------------------------\n");
                    responseStr.append(new String(IOUtils.slurpStream( response.getMimeKnob().getEntireMessageBodyAsInputStream()), ContentTypeHeader.TEXT_DEFAULT.getEncoding()));
                    responseStr.append("\n------------------------------------------------\n");
                    if (AssertionStatus.NONE != kid.assertionStatus) {
                        hasError = true;
                        sessionLog.append("Connection test has failed.");
                        sessionLog.append("\n");
                    } else {
                        sessionLog.append("Connection test was successful.");
                        sessionLog.append("\n");
                    }
                    sessionLog.append("\n");
                } catch (Exception e) {
                    e.printStackTrace();
                    hasError = true;
                    if (e.getCause() != null) {
                        sessionLog.append(e.getCause());
                        sessionLog.append("\n");
                    }
                    if (e.getMessage() != null) {
                        sessionLog.append(e.getMessage());
                        sessionLog.append("\n");
                    }
                    sessionLog.append("\n");
                }
            }//end of for
        } catch (Exception e) {
            sessionLog.append(e.getMessage());
            throw new HttpAdminException("HttpAdminException:" + e.getMessage(), sessionLog.toString(), responseStr.toString());
        }
        if(logger.getLevel()==Level.FINER){
            logger.log(Level.FINER, sessionLog.toString());
            logger.log(Level.FINER, responseStr.toString());
        }
        if (hasError) {
            throw new HttpAdminException("HttpAdminException", sessionLog.toString(), responseStr.toString());
        }
        return responseStr.toString();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.appCtx = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return appCtx;
    }

    private class KidResult {
        final AssertionStatus assertionStatus;
        final Map<Object, List<AuditDetail>> details;

        private KidResult(AssertionStatus assertionStatus, Map<Object, List<AuditDetail>> details) {
            this.assertionStatus = assertionStatus;
            this.details = details;
        }
    }

    private class DetailCollectingAuditContext implements AuditContext {
        private final Map<Object, List<AuditDetailEvent.AuditDetailWithInfo>> details = new LinkedHashMap<Object, List<AuditDetailEvent.AuditDetailWithInfo>>();

        @Override
        public void addDetail(AuditDetail detail, Object source) {
            addDetail(new AuditDetailEvent.AuditDetailWithInfo(source, detail, null, null));
        }

        @Override
        public void addDetail(AuditDetailEvent.AuditDetailWithInfo auditDetailInfo) {
            Object source = auditDetailInfo.getSource();
            List<AuditDetailEvent.AuditDetailWithInfo> list = details.get(source);
            if (list == null) {
                list = new ArrayList<AuditDetailEvent.AuditDetailWithInfo>();
                details.put(source, list);
            }
            list.add(auditDetailInfo);
        }

        @Override
        public Set getHints() {
            return Collections.emptySet();
        }

        @Override
        public Map<Object, List<AuditDetail>> getDetails() {
            Map<Object, List<AuditDetail>> ads = new HashMap<Object, List<AuditDetail>>();

            for (Map.Entry<Object, List<AuditDetailEvent.AuditDetailWithInfo>> entry : details.entrySet()) {
                List<AuditDetail> ds = new ArrayList<AuditDetail>();
                for (AuditDetailEvent.AuditDetailWithInfo detailWithInfo : entry.getValue()) {
                    ds.add(detailWithInfo.getDetail());
                }
                ads.put(entry.getKey(), ds);
            }

            return Collections.unmodifiableMap(ads);
        }

        @Override
        public void setContextVariables(Map<String, Object> variables) {
            throw new UnsupportedOperationException("Unsupported operation");
        }
    }
    
    private String getConfiguredContentType(final HttpRoutingAssertion assertion){
        String contentType = "text/xml; charset=utf-8";
        //check if there's an header named content-type
        if(assertion.getRequestHeaderRules().getRules()!=null && assertion.getRequestHeaderRules().getRules().length > 0){
            for(HttpPassthroughRule rule: assertion.getRequestHeaderRules().getRules()){
                if(rule.getName().equalsIgnoreCase("content-type")){
                    if (rule.getCustomizeValue() != null && rule.getCustomizeValue().length() > 0) {
                        return rule.getCustomizeValue();
                    }
                }
            }
        }
        if(assertion.getTestContentType()!=null){
            //user selected a particular content type
            contentType=assertion.getTestContentType();
        }
        return contentType;
    }

}
