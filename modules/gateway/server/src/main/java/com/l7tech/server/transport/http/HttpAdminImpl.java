package com.l7tech.server.transport.http;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.gateway.common.transport.http.HttpAdmin;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.audit.AuditContextFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.ServerHttpRoutingAssertion;
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
    public void testConnection(final String[] serverUrls, final String testMessage, final HttpRoutingAssertion assertion) throws HttpAdminException {
        if (serverUrls == null || serverUrls.length == 0) {
            throw new HttpAdminException("HttpAdminException: serverUrls param is empty", null);
        }
        final StringBuffer sb = new StringBuffer();
        boolean hasError = false;
        try {
            XmlUtil.stringAsDocument(testMessage);//make sure it's a valid XML
            //simulate the request using the real server assertion
            final Message testRequestVariable = new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(testMessage.getBytes()));
            testRequestVariable.getXmlKnob().getDocumentReadOnly();
            testRequestVariable.getMimeKnob();
            for (String serverUrl : serverUrls) {
                try {
                    assertion.setProtectedServiceUrl(serverUrl);//make sure we are testing one URL at a time, this forces it
                    assertion.setRequestMsgSrc("testRequestMessage");
                    final PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message(), true);
                    pec.setVariable("testRequestMessage",testRequestVariable);
                    final ServerHttpRoutingAssertion serverAssertion = new ServerHttpRoutingAssertion(assertion, getApplicationContext());
                    sb.append("\"");
                    sb.append(serverUrl);
                    sb.append("\"\n");

                    pec.setAuditLevel(Level.INFO);

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
                                    sb.append("URL is valid but ");
                                }
                                sb.append(messageBuffer.toString());
                                sb.append("\n");
                            }
                        }
                    }

                    if (AssertionStatus.NONE != kid.assertionStatus) {
                        hasError = true;
                        sb.append("Connection test has failed.");
                        sb.append("\n");
                    } else {
                        sb.append("Connection test was successful.");
                        sb.append("\n");
                    }
                    sb.append("\n");
                } catch (Exception e) {
                    e.printStackTrace();
                    hasError = true;
                    if (e.getCause() != null) {
                        sb.append(e.getCause());
                        sb.append("\n");
                    }
                    if (e.getMessage() != null) {
                        sb.append(e.getMessage());
                        sb.append("\n");
                    }
                    sb.append("\n");
                }
            }//end of for
        } catch (Exception e) {
            sb.append(e.getMessage());
            throw new HttpAdminException("HttpAdminException:" + e.getMessage(), sb.toString());
        }
        logger.log(Level.FINER, sb.toString());//just in case we need to see details of the test even when the test is successful
        if (hasError) {
            throw new HttpAdminException("HttpAdminException", sb.toString());
        }
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

}
