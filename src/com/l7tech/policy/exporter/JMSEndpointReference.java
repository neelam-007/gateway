package com.l7tech.policy.exporter;

import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.console.util.JmsUtilities;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A reference to a JMSConnection used by an assertion of type JmsRoutingAssertion
 * included in an exported policy.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 19, 2004<br/>
 * $Id$<br/>
 */
public class JMSEndpointReference extends ExternalReference {
    public JMSEndpointReference(long endpointOid) {
        oid = endpointOid;
        JmsAdmin admin = Registry.getDefault().getJmsManager();
        JmsConnection jmsConnection = null;
        try {
            JmsEndpoint jmsEndpoint = admin.findEndpointByPrimaryKey(endpointOid);
            if (jmsEndpoint != null) {
                endpointName = jmsEndpoint.getName();
                jmsConnection = admin.findConnectionByPrimaryKey(jmsEndpoint.getConnectionOid());
            }
        } catch (RemoteException e) {
            logger.log(Level.SEVERE, "cannot retrieve endpoint. partial reference will be built", e);
            jmsConnection = null;
        } catch (FindException e){
            logger.log(Level.SEVERE, "cannot retrieve endpoint. partial reference will be built", e);
            jmsConnection = null;
        }
        if (jmsConnection != null) {
            initialContextFactoryClassname = jmsConnection.getInitialContextFactoryClassname();
            jndiUrl = jmsConnection.getJndiUrl();
            queueFactoryUrl = jmsConnection.getQueueFactoryUrl();
            topicFactoryUrl = jmsConnection.getTopicFactoryUrl();
            destinationFactoryUrl = jmsConnection.getDestinationFactoryUrl();
        }
    }

    public static JMSEndpointReference parseFromElement(Element el) throws InvalidDocumentFormatException {
        // make sure passed element has correct name
        if (!el.getNodeName().equals(REF_EL_NAME)) {
            throw new InvalidDocumentFormatException("Expecting element of name " + REF_EL_NAME);
        }
        JMSEndpointReference output = new JMSEndpointReference();
        String val = getParamFromEl(el, OID_EL_NAME);
        if (val != null) {
            output.oid = Long.parseLong(val);
        }
        output.endpointName = getParamFromEl(el, EPNAME_EL_NAME);
        output.initialContextFactoryClassname = getParamFromEl(el, CONTEXT_EL_NAME);
        output.jndiUrl = getParamFromEl(el, JNDI_EL_NAME);
        output.queueFactoryUrl = getParamFromEl(el, QUEUE_EL_NAME);
        output.topicFactoryUrl = getParamFromEl(el, TOPIC_EL_NAME);
        output.destinationFactoryUrl = getParamFromEl(el, DESTINATION_EL_NAME);
        return output;
    }

    private JMSEndpointReference() {
        super();
    }

    public String getEndpointName() {
        return endpointName;
    }

    public String getInitialContextFactoryClassname() {
        return initialContextFactoryClassname;
    }

    public String getJndiUrl() {
        return jndiUrl;
    }

    public String getQueueFactoryUrl() {
        return queueFactoryUrl;
    }

    public void setLocalizeReplace(long newEndpointId) {
        localizeType = LocaliseAction.REPLACE;
        localEndpointId = newEndpointId;
    }

    public void setLocalizeDelete() {
        localizeType = LocaliseAction.DELETE;
    }

    public void setLocalizeIgnore() {
        localizeType = LocaliseAction.IGNORE;
    }

    void serializeToRefElement(Element referencesParentElement) {
        Element refEl = referencesParentElement.getOwnerDocument().createElement(REF_EL_NAME);
        refEl.setAttribute(ExporterConstants.REF_TYPE_ATTRNAME, JMSEndpointReference.class.getName());
        referencesParentElement.appendChild(refEl);
        Element oidEl = referencesParentElement.getOwnerDocument().createElement(OID_EL_NAME);
        Text txt = XmlUtil.createTextNode(referencesParentElement, Long.toString(oid));
        oidEl.appendChild(txt);
        refEl.appendChild(oidEl);
        Element icfcEl = referencesParentElement.getOwnerDocument().createElement(CONTEXT_EL_NAME);
        refEl.appendChild(icfcEl);
        Element jndiEl = referencesParentElement.getOwnerDocument().createElement(JNDI_EL_NAME);
        refEl.appendChild(jndiEl);
        Element qfuEl = referencesParentElement.getOwnerDocument().createElement(QUEUE_EL_NAME);
        refEl.appendChild(qfuEl);
        Element tfuEl = referencesParentElement.getOwnerDocument().createElement(TOPIC_EL_NAME);
        refEl.appendChild(tfuEl);
        Element dfuEl = referencesParentElement.getOwnerDocument().createElement(DESTINATION_EL_NAME);
        refEl.appendChild(dfuEl);
        Element epnEl = referencesParentElement.getOwnerDocument().createElement(EPNAME_EL_NAME);
        refEl.appendChild(epnEl);
        if (initialContextFactoryClassname != null) {
            txt = XmlUtil.createTextNode(referencesParentElement, initialContextFactoryClassname);
            icfcEl.appendChild(txt);
        }
        if (jndiUrl != null) {
            txt = XmlUtil.createTextNode(referencesParentElement, jndiUrl);
            jndiEl.appendChild(txt);
        }
        if (queueFactoryUrl != null) {
            txt = XmlUtil.createTextNode(referencesParentElement, queueFactoryUrl);
            qfuEl.appendChild(txt);
        }
        if (topicFactoryUrl != null) {
            txt = XmlUtil.createTextNode(referencesParentElement, topicFactoryUrl);
            tfuEl.appendChild(txt);
        }
        if (destinationFactoryUrl != null) {
            txt = XmlUtil.createTextNode(referencesParentElement, destinationFactoryUrl);
            dfuEl.appendChild(txt);
        }
        if (endpointName != null) {
            txt = XmlUtil.createTextNode(referencesParentElement, endpointName);
            epnEl.appendChild(txt);
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JMSEndpointReference)) return false;

        final JMSEndpointReference jmsEndpointReference = (JMSEndpointReference) o;

        if (destinationFactoryUrl != null ? !destinationFactoryUrl.equals(jmsEndpointReference.destinationFactoryUrl) : jmsEndpointReference.destinationFactoryUrl != null) return false;
        if (initialContextFactoryClassname != null ? !initialContextFactoryClassname.equals(jmsEndpointReference.initialContextFactoryClassname) : jmsEndpointReference.initialContextFactoryClassname != null) return false;
        if (jndiUrl != null ? !jndiUrl.equals(jmsEndpointReference.jndiUrl) : jmsEndpointReference.jndiUrl != null) return false;
        if (queueFactoryUrl != null ? !queueFactoryUrl.equals(jmsEndpointReference.queueFactoryUrl) : jmsEndpointReference.queueFactoryUrl != null) return false;
        if (topicFactoryUrl != null ? !topicFactoryUrl.equals(jmsEndpointReference.topicFactoryUrl) : jmsEndpointReference.topicFactoryUrl != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (initialContextFactoryClassname != null ? initialContextFactoryClassname.hashCode() : 0);
        result = 29 * result + (jndiUrl != null ? jndiUrl.hashCode() : 0);
        result = 29 * result + (queueFactoryUrl != null ? queueFactoryUrl.hashCode() : 0);
        result = 29 * result + (topicFactoryUrl != null ? topicFactoryUrl.hashCode() : 0);
        result = 29 * result + (destinationFactoryUrl != null ? destinationFactoryUrl.hashCode() : 0);
        return result;
    }

    /**
     * Checks whether or not an external reference can be mapped on this local
     * system without administrator interaction.
     */
    boolean verifyReference() {
        Collection tempMatches = new ArrayList(); // contains JmsAdmin.JmsTuple objects that have partial match
        List jmsQueues = JmsUtilities.loadJmsQueues(false);
        for (Iterator iterator = jmsQueues.iterator(); iterator.hasNext();) {
            JmsAdmin.JmsTuple jmsTuple = (JmsAdmin.JmsTuple) iterator.next();
            // what makes a jms queue the same?
            // let's say a combination of JndiUrl, CONTEXT_EL_NAME, QUEUE_EL_NAME and TOPIC_EL_NAME
            if (jmsTuple.getEndpoint().isMessageSource()) {
                continue;
            } else if (!jmsTuple.getConnection().getJndiUrl().equals(jndiUrl)) {
                continue;
            } else if (!jmsTuple.getConnection().getInitialContextFactoryClassname().equals(initialContextFactoryClassname)) {
                continue;
            } else if (!jmsTuple.getConnection().getQueueFactoryUrl().equals(queueFactoryUrl)) {
                continue;
            }
            // we have a partial match
            tempMatches.add(jmsTuple);

        }
        if (tempMatches.isEmpty()) {
            logger.warning("The JMS endpoint cannot be resolved.");
            return false;
        } else {
            // Try to discriminate using name property
            for (Iterator i = tempMatches.iterator(); i.hasNext();) {
                JmsAdmin.JmsTuple jmsTuple = (JmsAdmin.JmsTuple)i.next();
                if (jmsTuple.getEndpoint().getName().equals(endpointName)) {
                    // WE HAVE A PERFECT MATCH!
                    logger.fine("The local JMS endpoint was resolved from oid " + oid + " to " + localEndpointId);
                    localEndpointId = jmsTuple.getEndpoint().getOid();
                    localizeType = LocaliseAction.REPLACE;
                    return true;
                }
            }
            // Otherwise, use first partial match
            JmsAdmin.JmsTuple jmsTuple = (JmsAdmin.JmsTuple)tempMatches.iterator().next();
            logger.fine("The local JMS endpoint was resolved from oid " + oid + " to " + localEndpointId);
            localEndpointId = jmsTuple.getEndpoint().getOid();
            localizeType = LocaliseAction.REPLACE;
            return true;
        }
    }

    boolean localizeAssertion(Assertion assertionToLocalize) {
        if (localizeType != LocaliseAction.IGNORE) {
            if (assertionToLocalize instanceof JmsRoutingAssertion) {
            JmsRoutingAssertion jmsRoutingAssertion = (JmsRoutingAssertion) assertionToLocalize;
                if (jmsRoutingAssertion.getEndpointOid() != null &&
                    jmsRoutingAssertion.getEndpointOid().longValue() == oid) {
                    if (localizeType == LocaliseAction.REPLACE) {
                        // replace endpoint id
                        jmsRoutingAssertion.setEndpointOid(new Long(localEndpointId));
                        // replace endpoint name
                        jmsRoutingAssertion.setEndpointName(endpointNameFromOid(localEndpointId));
                        logger.info("The endpoint id of the imported jms routing assertion has been changed " +
                                    "from " + oid + " to " + localEndpointId);
                    } else if (localizeType == LocaliseAction.DELETE) {
                        logger.info("Deleted this assertin from the tree.");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private String endpointNameFromOid(long oid) {
        JmsAdmin admin = Registry.getDefault().getJmsManager();
        if (admin == null) {
            logger.severe("Cannot get the JMSAdmin");
            return null;
        }
        try {
            JmsEndpoint endpoint = admin.findEndpointByPrimaryKey(oid);
            if (endpoint != null) return endpoint.getName();
        } catch (RemoteException e) {
            logger.warning("could not retreive the JMS endpoint from oid " + oid);
        } catch (FindException e) {
            logger.warning("could not retreive the JMS endpoint from oid " + oid);
        }
        logger.warning("The oid " + oid + " could not be used to get an endpoint name.");
        return null;
    }

    private final Logger logger = Logger.getLogger(JMSEndpointReference.class.getName());

    /**
     * Enum-type class for the type of localization to use.
     */
    public static class LocaliseAction {
        public static final LocaliseAction IGNORE = new LocaliseAction(1);
        public static final LocaliseAction DELETE = new LocaliseAction(2);
        public static final LocaliseAction REPLACE = new LocaliseAction(3);
        private LocaliseAction(int val) {
            this.val = val;
        }
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LocaliseAction)) return false;

            final LocaliseAction localiseAction = (LocaliseAction) o;

            if (val != localiseAction.val) return false;

            return true;
        }

        public int hashCode() {
            return val;
        }

        private int val = 0;
    }

    private long oid;
    private long localEndpointId;
    private String endpointName;
    private String initialContextFactoryClassname;
    private String jndiUrl;
    private String queueFactoryUrl;
    private String topicFactoryUrl;
    private String destinationFactoryUrl;
    private LocaliseAction localizeType = null;
    public static final String REF_EL_NAME = "JMSConnectionReference";
    public static final String OID_EL_NAME = "OID";
    public static final String EPNAME_EL_NAME = "EndpointName";
    public static final String CONTEXT_EL_NAME = "InitialContextFactoryClassname";
    public static final String JNDI_EL_NAME = "JndiUrl";
    public static final String QUEUE_EL_NAME = "QueueFactoryUrl";
    public static final String TOPIC_EL_NAME = "TopicFactoryUrl";
    public static final String DESTINATION_EL_NAME = "DestinationFactoryUrl";
}
