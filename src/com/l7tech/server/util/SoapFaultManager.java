package com.l7tech.server.util;

import com.l7tech.common.xml.SoapFaultLevel;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.AuditDetail;
import com.l7tech.common.audit.Messages;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.ServerConfig;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.wsp.TypeMappingUtils;
import com.l7tech.policy.wsp.WspConstants;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;
import java.text.MessageFormat;
import java.text.FieldPosition;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;

/**
 * Server side SoapFaultLevel utils.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 8, 2006<br/>
 */
public class SoapFaultManager implements ApplicationContextAware {
    private final ServerConfig serverConfig;
    private final Logger logger = Logger.getLogger(SoapFaultManager.class.getName());
    private long lastParsedFromSettings;
    private SoapFaultLevel fromSettings;
    private Auditor auditor;
    public static final String FAULT_NS = "http://www.layer7tech.com/ws/policy/fault";

    public SoapFaultManager(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    /**
     * Read settings from server configuration and assemble a SoapFaultLevel based on the default values.
     */
    public SoapFaultLevel getDefaultBehaviorSettings() {
        // cache at least one minute. todo, review
        if (fromSettings == null || (System.currentTimeMillis() - lastParsedFromSettings) > 60000) {
            return constructFaultLevelFromServerConfig();
        }
        return fromSettings;
    }

    private synchronized SoapFaultLevel constructFaultLevelFromServerConfig() {
        // parse default settings from system settings
        fromSettings = new SoapFaultLevel();
        fromSettings.setLevel(Integer.parseInt(serverConfig.getProperty("defaultfaultlevel")));
        fromSettings.setIncludePolicyDownloadURL(Boolean.parseBoolean(serverConfig.getProperty("defaultfaultpolicyurl")));
        fromSettings.setFaultTemplate(serverConfig.getProperty("defaultfaulttemplate"));
        lastParsedFromSettings = System.currentTimeMillis();
        return fromSettings;
    }

    /**
     * constructs a soap fault based on the pec and the level desired.
     * @return may return null if level is SoapFaultLevel.DROP_CONNECTION otherwise returns a string containing the soap fault xml
     */
    public String constructReturningFault(SoapFaultLevel faultLevelInfo, PolicyEnforcementContext pec) {
        String output = null;
        AssertionStatus globalstatus = pec.getPolicyResult();
        if (globalstatus == null) {
            logger.severe("PolicyEnforcementContext.policyResult not set");
        }
        switch (faultLevelInfo.getLevel()) {
            case SoapFaultLevel.DROP_CONNECTION:
                break;
            case SoapFaultLevel.TEMPLATE_FAULT:
                output = ExpandVariables.process(faultLevelInfo.getFaultTemplate(), pec.getVariableMap(faultLevelInfo.getVariablesUsed(), auditor));
                break;
            case SoapFaultLevel.GENERIC_FAULT:
                try {
                    Document tmp = XmlUtil.stringToDocument(GENERIC_FAULT);
                    NodeList res = tmp.getElementsByTagNameNS(FAULT_NS, "policyResult");
                    // populate @status element
                    Element policyResultEl = (Element)res.item(0);
                    policyResultEl.setAttribute("status", globalstatus.getMessage());
                    // populate the faultactor value
                    String actor = pec.getVariable("request.url").toString();
                    res = tmp.getElementsByTagName("faultactor");
                    Element faultactor = (Element)res.item(0);
                    faultactor.setTextContent(actor);
                    output = XmlUtil.nodeToFormattedString(tmp);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "could not construct generic fault", e);
                }
                break;
            case SoapFaultLevel.MEDIUM_DETAIL_FAULT:
                output = buildDetailedFault(pec, globalstatus, false);
                break;
            case SoapFaultLevel.FULL_TRACE_FAULT:
                output = buildDetailedFault(pec, globalstatus, true);
                break;
        }
        return output;
    }

    /**
     * returns soap faults in the form of:
     * <?xml version="1.0" encoding="UTF-8"?>
     *   <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
     *      <soapenv:Body>
     *          <soapenv:Fault>
     *              <faultcode>Server</faultcode>
     *              <faultstring>Policy Falsified</faultstring>
     *              <faultactor>http://soong:8080/xml/blub</faultactor>
     *              <l7:policyResult status="Falsified" xmlns:l7="http://www.layer7tech.com/ws/policy/fault" xmlns:l7p="http://www.layer7tech.com/ws/policy">
     *                  <l7:assertionResult status="BAD" assertion="l7p:WssUsernameToken">
     *                      <l7:detailMessage id="4302">This request did not contain any WSS level security.</l7:detailMessage>
     *                      <l7:detailMessage id="5204">Request did not include an encrypted UsernameToken.</l7:detailMessage>
     *                  </l7:assertionResult>
     *              </l7:policyResult>
     *          </soapenv:Fault>
     *      </soapenv:Body>
     *  </soapenv:Envelope>
     */
    private String buildDetailedFault(PolicyEnforcementContext pec, AssertionStatus globalstatus, boolean includeSuccesses) {
        String output = null;
        try {
            Document tmp = XmlUtil.stringToDocument(GENERIC_FAULT);
            NodeList res = tmp.getElementsByTagNameNS(FAULT_NS, "policyResult");
            // populate @status element
            Element policyResultEl = (Element)res.item(0);
            policyResultEl.setAttribute("status", globalstatus.getMessage());
            policyResultEl.setAttribute("xmlns:l7p", WspConstants.L7_POLICY_NS);

            // populate the faultactor value
            String actor = pec.getVariable("request.url").toString();
            res = tmp.getElementsByTagName("faultactor");
            Element faultactor = (Element)res.item(0);
            faultactor.setTextContent(actor);

            List<PolicyEnforcementContext.AssertionResult> results = pec.getAssertionResults(pec.getAuditContext());
            for (PolicyEnforcementContext.AssertionResult result : results) {
                if (result.getStatus() == AssertionStatus.NONE && !includeSuccesses) {
                    continue;
                }
                Element assertionResultEl = tmp.createElementNS(FAULT_NS, "l7:assertionResult");
                assertionResultEl.setAttribute("status", result.getStatus().getMessage());
                String assertionattr = "l7p:" + TypeMappingUtils.findTypeMappingByClass(result.getAssertion().getClass()).getExternalName();
                assertionResultEl.setAttribute("assertion", assertionattr);
                List<AuditDetail> details = result.getDetails();
                if (details != null) {
                    for (AuditDetail detail : details) {
                        Element detailMsgEl = tmp.createElementNS(FAULT_NS, "l7:detailMessage");
                        detailMsgEl.setAttribute("id", Long.toString(detail.getMessageId()));
                        // add text node with actual message. see below for logpanel sample:
                        StringBuffer msgbuf = new StringBuffer();
                        MessageFormat mf = new MessageFormat(Messages.getMessageById(detail.getMessageId()));
                        mf.format(detail.getParams(), msgbuf, new FieldPosition(0));
                        detailMsgEl.setTextContent(msgbuf.toString());
                        assertionResultEl.appendChild(tmp.importNode(detailMsgEl, true));
                    }
                }
                policyResultEl.appendChild(tmp.importNode(assertionResultEl, true));
            }
            output = XmlUtil.nodeToFormattedString(tmp);
        } catch (Exception e) {
            logger.log(Level.WARNING, "could not construct generic fault", e);
        }
        return output;
    }

    private static final String GENERIC_FAULT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "    <soapenv:Body>\n" +
                        "        <soapenv:Fault>\n" +
                        "            <faultcode>Server</faultcode>\n" +
                        "            <faultstring>Assertion Falsified</faultstring>\n" +
                        "            <faultactor/>\n" +
                        "            <l7:policyResult xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\"/>\n" +
                        "        </soapenv:Fault>\n" +
                        "    </soapenv:Body>\n" +
                        "</soapenv:Envelope>";

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        auditor = new Auditor(this, applicationContext, logger);
    }
}
