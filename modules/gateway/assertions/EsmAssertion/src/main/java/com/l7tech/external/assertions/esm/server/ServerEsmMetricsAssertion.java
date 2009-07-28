package com.l7tech.external.assertions.esm.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.esm.EsmMetricsAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.wsdm.ServiceManagementAdministrationService;
import com.l7tech.server.wsdm.faults.FaultMappableException;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.xml.SoapFaultLevel;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the EsmAssertion.
 *
 * @see com.l7tech.external.assertions.esm.EsmMetricsAssertion
 */
public class ServerEsmMetricsAssertion extends AbstractServerAssertion<EsmMetricsAssertion> {
    private static final Logger logger = Logger.getLogger(ServerEsmMetricsAssertion.class.getName());

    private final Auditor auditor;
    private final String[] variablesUsed;

    private ServiceManagementAdministrationService esmService;

    public ServerEsmMetricsAssertion(EsmMetricsAssertion assertion, ApplicationContext springAppContext) throws PolicyAssertionException {
        super(assertion);
        this.auditor = springAppContext != null ? new Auditor(this, springAppContext, logger) : new LogOnlyAuditor(logger);
        this.variablesUsed = assertion.getVariablesUsed();
        this.esmService = EsmApplicationContext.getInstance(springAppContext).getEsmService();
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        logger.info("Forwarding message to ESM QOS Metrics Service");

        try {
            final Document response = esmService.handleESMRequest(context.getService().getOid(), context.getRequest());
            context.getResponse().initialize(response);
            return AssertionStatus.NONE;
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {ExceptionUtils.getMessage(e)}, e);
            return AssertionStatus.BAD_REQUEST;
        } catch (FaultMappableException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, e.getMessage());
            context.getResponse().initialize(ContentTypeHeader.XML_DEFAULT, e.getSoapFaultXML().getBytes("UTF-8"));
            final SoapFaultLevel faultlevel = new SoapFaultLevel();
            faultlevel.setFaultTemplate(e.getSoapFaultXML());
            faultlevel.setLevel(SoapFaultLevel.TEMPLATE_FAULT);
            context.setFaultlevel(faultlevel);
            return AssertionStatus.FAILED;
        }
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
        logger.log(Level.INFO, "ServerEsmMetricsAssertion is preparing itself to be unloaded");
    }
}