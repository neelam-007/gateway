package com.l7tech.policy.exporter;

import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;

import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.Text;

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

    public void serializeToRefElement(Element referencesParentElement) {
        Element refEl = referencesParentElement.getOwnerDocument().createElement("JMSConnectionProviderReference");
        refEl.setAttribute(ExporterConstants.REF_TYPE_ATTRNAME, JMSEndpointReference.class.getName());
        referencesParentElement.appendChild(refEl);
        Element icfcEl = referencesParentElement.getOwnerDocument().createElement("InitialContextFactoryClassname");
        refEl.appendChild(icfcEl);
        Element jndiEl = referencesParentElement.getOwnerDocument().createElement("JndiUrl");
        refEl.appendChild(jndiEl);
        Element qfuEl = referencesParentElement.getOwnerDocument().createElement("QueueFactoryUrl");
        refEl.appendChild(qfuEl);
        Element tfuEl = referencesParentElement.getOwnerDocument().createElement("TopicFactoryUrl");
        refEl.appendChild(tfuEl);
        Element dfuEl = referencesParentElement.getOwnerDocument().createElement("DestinationFactoryUrl");
        refEl.appendChild(dfuEl);
        if (initialContextFactoryClassname != null) {
            Text txt = referencesParentElement.getOwnerDocument().createTextNode(initialContextFactoryClassname);
            icfcEl.appendChild(txt);
        }
        if (jndiUrl != null) {
            Text txt = referencesParentElement.getOwnerDocument().createTextNode(jndiUrl);
            jndiEl.appendChild(txt);
        }
        if (queueFactoryUrl != null) {
            Text txt = referencesParentElement.getOwnerDocument().createTextNode(queueFactoryUrl);
            qfuEl.appendChild(txt);
        }
        if (topicFactoryUrl != null) {
            Text txt = referencesParentElement.getOwnerDocument().createTextNode(topicFactoryUrl);
            tfuEl.appendChild(txt);
        }
        if (destinationFactoryUrl != null) {
            Text txt = referencesParentElement.getOwnerDocument().createTextNode(destinationFactoryUrl);
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

    private final Logger logger = Logger.getLogger(IdProviderReference.class.getName());

    private String initialContextFactoryClassname;
    private String jndiUrl;
    private String queueFactoryUrl;
    private String topicFactoryUrl;
    private String destinationFactoryUrl;
}
