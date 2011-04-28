/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.esm.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.esm.EsmSubscriptionAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.wsdm.ServiceManagementAdministrationService;
import com.l7tech.server.wsdm.faults.FaultMappableException;
import com.l7tech.util.Charsets;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.SoapFaultLevel;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;

public class ServerEsmSubscriptionAssertion extends AbstractServerAssertion<EsmSubscriptionAssertion> {
    private static final Logger logger = Logger.getLogger(ServerEsmSubscriptionAssertion.class.getName());

    private Auditor auditor;
    private String[] variablesUsed;
    private ServiceManagementAdministrationService esmService;

    public ServerEsmSubscriptionAssertion(EsmSubscriptionAssertion assertion, ApplicationContext springAppContext) {
        super(assertion);
        this.auditor = springAppContext != null ? new Auditor(this, springAppContext, logger) : new LogOnlyAuditor(logger);
        this.variablesUsed = assertion.getVariablesUsed();
        this.esmService = EsmApplicationContext.getInstance(springAppContext).getEsmService();
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        logger.info("Forwarding message to WSDM Subscription Service");

        try {
            final Document response = esmService.handleSubscriptionRequest(context.getService().getOid(), context.getRequest(), assertion.getNotificationPolicyGuid());
            context.getResponse().initialize(response);
            return AssertionStatus.NONE;
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {ExceptionUtils.getMessage(e)}, e);
            return AssertionStatus.BAD_REQUEST;
        } catch (FaultMappableException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, e.getMessage());
            context.getResponse().initialize(ContentTypeHeader.XML_DEFAULT, e.getSoapFaultXML().getBytes(Charsets.UTF8));
            final SoapFaultLevel faultlevel = new SoapFaultLevel();
            faultlevel.setFaultTemplate(e.getSoapFaultXML());
            faultlevel.setLevel(SoapFaultLevel.TEMPLATE_FAULT);
            context.setFaultlevel(faultlevel);
            return AssertionStatus.FAILED;
        }
    }
}
