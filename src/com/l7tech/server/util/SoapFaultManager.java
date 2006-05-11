package com.l7tech.server.util;

import com.l7tech.common.xml.SoapFaultLevel;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.ServerConfig;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.policy.assertion.AssertionStatus;

import java.util.logging.Logger;
import java.util.logging.Level;

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
                    NodeList res = tmp.getElementsByTagNameNS("http://www.layer7tech.com/ws/policy/fault", "policyResult");
                    Element policyResultEl = (Element)res.item(0);
                    //Element policyResultEl = XmlUtil.findOnlyOneChildElementByName(tmp.getDocumentElement(), "http://www.layer7tech.com/ws/policy/fault", "policyResult");
                    policyResultEl.setAttribute("status", globalstatus.getMessage());
                    output = XmlUtil.nodeToFormattedString(tmp);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "could not construct generic fault", e);
                }
                break;
            case SoapFaultLevel.MEDIUM_DETAIL_FAULT:
                // todo, construct entire soap fault based on what the pec tells us what happened
                break;
            case SoapFaultLevel.FULL_TRACE_FAULT:
                // todo, construct entire soap fault based on what the pec tells us what happened
                break;
        }
        return output;
    }

    private static final String GENERIC_FAULT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "    <soapenv:Body>\n" +
                        "        <soapenv:Fault>\n" +
                        "            <faultcode>Server</faultcode>\n" +
                        "            <faultstring>Assertion Falsified</faultstring>\n" +
                        "            <faultactor>http://soong:8080/xml/blub</faultactor>\n" +
                        "            <l7:policyResult xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\"/>\n" +
                        "        </soapenv:Fault>\n" +
                        "    </soapenv:Body>\n" +
                        "</soapenv:Envelope>";

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        auditor = new Auditor(this, applicationContext, logger);
    }
}
