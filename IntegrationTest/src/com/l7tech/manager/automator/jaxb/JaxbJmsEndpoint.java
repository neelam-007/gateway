package com.l7tech.manager.automator.jaxb;

import com.l7tech.common.transport.jms.JmsEndpoint;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Apr 21, 2008
 * Time: 11:23:56 AM
 * JaxbJmsEndpoint wraps a JmsEndpoint. It provides a property jmsConnectionUniqueIdentifier which can uniquely
 * identity a JmsConnection. By storing this information with the JmsEndpoint when we are recreating the
 * JmsEndpoint on a fresh SSG we will be able to lookup the JmsConnection it requires without requring the oid
 * of the JmsConnection on the fresh SSG.
 * As you cannot look up a JmsConnection with anything other than an oid, you would need to first findAll JmsConnections
 * and then loop through each, creating it's unique identifier and comparing it to the unique identifier that
 * this class is storing.
 * So when moving JmsEndpoints, the JmsConnection's it requires must be created first.
 */
@XmlRootElement
public class JaxbJmsEndpoint {

    public JaxbJmsEndpoint(){
    }

    public JmsEndpoint getJmsEndPoint() {
        return jmsEndPoint;
    }

    public void setJmsEndPoint(JmsEndpoint jmsEndPoint) {
        this.jmsEndPoint = jmsEndPoint;
    }

    private JmsEndpoint jmsEndPoint;
    private String jmsConnectionUniqueIdentifier;

    public String getJmsConnectionUniqueIdentifier() {
        return jmsConnectionUniqueIdentifier;
    }

    public void setJmsConnectionUniqueIdentifier(String jmsConnectionUniqueIdentifier) {
        this.jmsConnectionUniqueIdentifier = jmsConnectionUniqueIdentifier;
    }
}
