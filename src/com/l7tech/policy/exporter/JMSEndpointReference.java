package com.l7tech.policy.exporter;

import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.JmsUtilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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

    void serializeToRefElement(Element referencesParentElement) {
        Element refEl = referencesParentElement.getOwnerDocument().createElement(REF_EL_NAME);
        refEl.setAttribute(ExporterConstants.REF_TYPE_ATTRNAME, JMSEndpointReference.class.getName());
        referencesParentElement.appendChild(refEl);
        Element oidEl = referencesParentElement.getOwnerDocument().createElement(OID_EL_NAME);
        Text txt = referencesParentElement.getOwnerDocument().createTextNode(Long.toString(oid));
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
        if (initialContextFactoryClassname != null) {
            txt = referencesParentElement.getOwnerDocument().createTextNode(initialContextFactoryClassname);
            icfcEl.appendChild(txt);
        }
        if (jndiUrl != null) {
            txt = referencesParentElement.getOwnerDocument().createTextNode(jndiUrl);
            jndiEl.appendChild(txt);
        }
        if (queueFactoryUrl != null) {
            txt = referencesParentElement.getOwnerDocument().createTextNode(queueFactoryUrl);
            qfuEl.appendChild(txt);
        }
        if (topicFactoryUrl != null) {
            txt = referencesParentElement.getOwnerDocument().createTextNode(topicFactoryUrl);
            tfuEl.appendChild(txt);
        }
        if (destinationFactoryUrl != null) {
            txt = referencesParentElement.getOwnerDocument().createTextNode(destinationFactoryUrl);
            dfuEl.appendChild(txt);
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
        List jmsQueues = JmsUtilities.loadJmsQueues(false);
        for (Iterator iterator = jmsQueues.iterator(); iterator.hasNext();) {
            JmsAdmin.JmsTuple jmsTuple = (JmsAdmin.JmsTuple) iterator.next();
            // what makes a jms queue the same?
            // let's say a combination of JndiUrl, CONTEXT_EL_NAME, QUEUE_EL_NAME and TOPIC_EL_NAME
            if (!jmsTuple.getConnection().getJndiUrl().equals(jndiUrl)) {
                continue;
            } else if (!jmsTuple.getConnection().getInitialContextFactoryClassname().equals(initialContextFactoryClassname)) {
                continue;
            } else if (!jmsTuple.getConnection().getQueueFactoryUrl().equals(queueFactoryUrl)) {
                continue;
            }
            // WE HAVE A MATCH!
            logger.fine("The local JMS endpoint was resolved from oid " + oid + " to " + localEndpointId);
            localEndpointId = jmsTuple.getEndpoint().getOid();
            return true;
        }
        logger.warning("The JMS endpoint cannot be resolved.");
        return false;
    }

    void localizeAssertion(Assertion assertionToLocalize) {
        if (assertionToLocalize instanceof JmsRoutingAssertion) {
            JmsRoutingAssertion jmsRoutingAssertion = (JmsRoutingAssertion) assertionToLocalize;
            if (jmsRoutingAssertion.getEndpointOid() != null &&
                jmsRoutingAssertion.getEndpointOid().longValue() == oid) {
                jmsRoutingAssertion.setEndpointOid(new Long(localEndpointId));
                logger.fine("Endpoint id was replaced from " + oid + " to localEndpointId");
            }
        }
    }

    private final Logger logger = Logger.getLogger(JMSEndpointReference.class.getName());

    private long oid;
    private long localEndpointId;
    private String initialContextFactoryClassname;
    private String jndiUrl;
    private String queueFactoryUrl;
    private String topicFactoryUrl;
    private String destinationFactoryUrl;
    public static final String REF_EL_NAME = "JMSConnectionReference";
    public static final String OID_EL_NAME = "OID";
    public static final String CONTEXT_EL_NAME = "InitialContextFactoryClassname";
    public static final String JNDI_EL_NAME = "JndiUrl";
    public static final String QUEUE_EL_NAME = "QueueFactoryUrl";
    public static final String TOPIC_EL_NAME = "TopicFactoryUrl";
    public static final String DESTINATION_EL_NAME = "DestinationFactoryUrl";
}
