package com.l7tech.server.util;

import com.l7tech.common.audit.AuditDetail;
import com.l7tech.server.audit.Auditor;
import com.l7tech.common.audit.Messages;
import com.l7tech.common.audit.AuditDetailMessage;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.SoapFaultLevel;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.wsp.TypeMappingUtils;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.cluster.ClusterPropertyManager;
import com.l7tech.cluster.ClusterProperty;
import com.l7tech.objectmodel.FindException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private ClusterPropertyManager clusterPropertiesManager;
    private ApplicationContext applicationContext;
    private final HashMap<Integer, String> cachedOverrideAuditMessages = new HashMap<Integer, String>();
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
    public static final String FAULT_NS = "http://www.layer7tech.com/ws/policy/fault";

    private final Timer checker;

    public SoapFaultManager(ServerConfig serverConfig, Timer timer) {
        if (timer == null) timer = new Timer("Soap fault manager refresh", true);
        this.serverConfig = serverConfig;
        this.checker = timer;
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
        String tmp = serverConfig.getPropertyCached("defaultfaultlevel");
        // default setting not available through config ?
        if (tmp == null) {
            logger.warning("Cannot retrieve defaultfaultlevel server properties falling back on hardcoded defaults");
            populateUltimateDefaults(fromSettings);
        } else {
            try {
                fromSettings.setLevel(Integer.parseInt(tmp));
                fromSettings.setIncludePolicyDownloadURL(Boolean.parseBoolean(serverConfig.getPropertyCached("defaultfaultpolicyurl")));
                fromSettings.setFaultTemplate(serverConfig.getPropertyCached("defaultfaulttemplate"));
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "user setting " + tmp + " for defaultfaultlevel is invalid", e);
                populateUltimateDefaults(fromSettings);
            }
        }
        lastParsedFromSettings = System.currentTimeMillis();
        return fromSettings;
    }

    private void populateUltimateDefaults(SoapFaultLevel input) {
        input.setLevel(SoapFaultLevel.GENERIC_FAULT);
        input.setIncludePolicyDownloadURL(true);
    }

    /**
     * constructs a soap fault based on the pec and the level desired.
     * @return may return null if level is SoapFaultLevel.DROP_CONNECTION otherwise returns a string containing the soap fault xml
     */
    public String constructReturningFault(SoapFaultLevel faultLevelInfo, PolicyEnforcementContext pec) {
        String output = null;
        AssertionStatus globalstatus = pec.getPolicyResult();
        if (globalstatus == null) {
            // if this happens, it means a bug needs fixing where a path fails to set a value on the policy result
            logger.severe("PolicyEnforcementContext.policyResult not set. Fallback on SERVER_ERROR");
            globalstatus = AssertionStatus.SERVER_ERROR;
        }
        switch (faultLevelInfo.getLevel()) {
            case SoapFaultLevel.DROP_CONNECTION:
                break;
            case SoapFaultLevel.TEMPLATE_FAULT:
                output = ExpandVariables.process(faultLevelInfo.getFaultTemplate(), pec.getVariableMap(faultLevelInfo.getVariablesUsed(), auditor), auditor);
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
                    // should not happen
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
     * SOAP faults resulting from an exception that occurs in the processing of the policy.
     * Receiving such a fault sould be considered a bug.
     */
    public String constructExceptionFault(Throwable e, PolicyEnforcementContext pec) {
        String output = null;
        try {
            Document tmp = XmlUtil.stringToDocument(EXCEPTION_FAULT);
            NodeList res = tmp.getElementsByTagNameNS(FAULT_NS, "policyResult");
            // populate @status element
            Element policyResultEl = (Element)res.item(0);
            policyResultEl.setAttribute("status", e.getMessage());
            // populate the faultactor value
            String actor;
            try {
                actor = pec.getVariable("request.url").toString();
                // todo, catch cases when this throws and just fix it
            } catch (NoSuchVariableException notfound) {
                logger.log(Level.WARNING, "this variable is not found but should always be set", notfound);
                actor = "ssg";
            }
            res = tmp.getElementsByTagName("faultactor");
            Element faultactor = (Element)res.item(0);
            faultactor.setTextContent(actor);
            output = XmlUtil.nodeToFormattedString(tmp);
        } catch (Exception el) {
            // should not happen
            logger.log(Level.WARNING, "Unexpected exception", el);
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
            String actor;
            try {
                actor = pec.getVariable("request.url").toString();
                // todo, catch cases when this throws and just fix it
            } catch (NoSuchVariableException notfound) {
                logger.log(Level.WARNING, "this variable is not found but should always be set", notfound);
                actor = "ssg";
            }
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
                String assertionattr = "l7p:" + TypeMappingUtils.findTypeMappingByClass(result.getAssertion().getClass(), null).getExternalName();
                assertionResultEl.setAttribute("assertion", assertionattr);
                List<AuditDetail> details = result.getDetails();
                if (details != null) {
                    for (AuditDetail detail : details) {
                        int msgid = detail.getMessageId();
                        // only show details FINE and higher for medium details, show all details for full details
                        if (includeSuccesses || (Messages.getAuditDetailMessageById(msgid).getLevel().intValue() >= Level.INFO.intValue())) {
                            Element detailMsgEl = tmp.createElementNS(FAULT_NS, "l7:detailMessage");
                            detailMsgEl.setAttribute("id", Long.toString(detail.getMessageId()));
                            // add text node with actual message. see below for logpanel sample:
                            StringBuffer msgbuf = new StringBuffer();
                            MessageFormat mf = new MessageFormat(getMessageById(msgid));
                            mf.format(detail.getParams(), msgbuf, new FieldPosition(0));
                            detailMsgEl.setTextContent(msgbuf.toString());
                            assertionResultEl.appendChild(tmp.importNode(detailMsgEl, true));
                        }
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

    /**
     * gets the assertion detail message giving priority to overriden defaults in the cluster property table.
     * caches the cluster overrides so it does not have to look them up all the time.
     */
    private String getMessageById(int msgid) {
        ReentrantReadWriteLock.ReadLock lock = cacheLock.readLock();
        lock.lock();
        try {
            String cachedMessage = cachedOverrideAuditMessages.get(msgid);
            if (cachedMessage != null) {
                return cachedMessage;
            }
        } finally {
            lock.unlock();
        }
        AuditDetailMessage message = Messages.getAuditDetailMessageById(msgid);
        return message==null ? null : message.getMessage();
    }

    private static final String GENERIC_FAULT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "    <soapenv:Body>\n" +
                        "        <soapenv:Fault>\n" +
                        "            <faultcode>soapenv:Server</faultcode>\n" +
                        "            <faultstring>Policy Falsified</faultstring>\n" +
                        "            <faultactor/>\n" +
                        "            <detail>\n" +
                        "                 <l7:policyResult xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\"/>\n" +
                        "            </detail>\n" +
                        "        </soapenv:Fault>\n" +
                        "    </soapenv:Body>\n" +
                        "</soapenv:Envelope>";

    private static final String EXCEPTION_FAULT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "    <soapenv:Body>\n" +
                        "        <soapenv:Fault>\n" +
                        "            <faultcode>soapenv:Server</faultcode>\n" +
                        "            <faultstring>Error in assertion processing</faultstring>\n" +
                        "            <faultactor/>\n" +
                        "            <detail>\n" +
                        "                 <l7:policyResult xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\"/>\n" +
                        "            </detail>\n" +
                        "        </soapenv:Fault>\n" +
                        "    </soapenv:Body>\n" +
                        "</soapenv:Envelope>";

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        auditor = new Auditor(this, applicationContext, logger);
        this.applicationContext = applicationContext;
        clusterPropertiesManager = (ClusterPropertyManager)applicationContext.getBean("clusterPropertyManager");
        final SoapFaultManager tasker = this;
        TimerTask task = new TimerTask() {
            public void run() {
                try {
                    tasker.updateOverrides();
                } catch(Exception e) {
                    logger.log(Level.WARNING, "Error updating message overrides.", e);
                }
            }
        };
        checker.schedule(task, 10000, 30000);
    }

    private void updateOverrides() {
        if (clusterPropertiesManager == null) {
            clusterPropertiesManager = (ClusterPropertyManager)applicationContext.getBean("clusterPropertyManager");
            if (clusterPropertiesManager == null) {
                logger.info("cant get handle on ClusterPropertiesManager");
                return;
            }
        }
        try {
            Collection fromTable = clusterPropertiesManager.findAll();
            ReentrantReadWriteLock.WriteLock lock = cacheLock.writeLock();
            lock.lock();
            try {
                cachedOverrideAuditMessages.clear();
                for (Object aFromTable : fromTable) {
                    ClusterProperty clusterProperty = (ClusterProperty) aFromTable;
                    if (clusterProperty.getName() != null && clusterProperty.getName().startsWith(Messages.OVERRIDE_PREFIX)) {
                        try {
                            Integer key = new Integer(clusterProperty.getName().substring(Messages.OVERRIDE_PREFIX.length()));
                            if (clusterProperty.getValue() != null) {
                                cachedOverrideAuditMessages.put(key, clusterProperty.getValue());
                            }
                        } catch (NumberFormatException e) {
                            logger.fine("thought this was an override, but it's not (" + clusterProperty.getName() + ")");
                        }
                    }
                }
            } finally {
                lock.unlock();
            }

        } catch (FindException e) {
            logger.log(Level.WARNING, "Cannot get cluster properties", e);
        }
    }
}
