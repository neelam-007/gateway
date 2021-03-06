package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * The JMSDestinationMO managed object represents a JMS Queue.
 *
 * <p>The Accessor for JMS destinations is read only. JMS destinations can be
 * accessed by identifier only.</p>
 *
 * @see ManagedObjectFactory#createJMSDestination()
 */
@XmlRootElement(name="JMSDestination")
@XmlType(name="JMSDestinationType", propOrder={"jmsDestinationDetail","jmsConnection","extension","extensions"})
@AccessorSupport.AccessibleResource(name ="jmsDestinations")
public class JMSDestinationMO extends ElementExtendableAccessibleObject {

    //- PUBLIC

    @Override
    public Integer getVersion() {
        Integer version = null;

        if ( jmsDestinationDetail != null ) {
            version = jmsDestinationDetail.getVersion();
        }

        return version;
    }

    @Override
    public void setVersion( final Integer version ) {
        if ( jmsDestinationDetail != null ) {
            jmsDestinationDetail.setVersion( version );
        }
    }

    /**
     * Get the destination details (required)
     *
     * @return The details or null
     */
    @XmlElement(name="JMSDestinationDetail", required=true)
    public JMSDestinationDetail getJmsDestinationDetail() {
        return jmsDestinationDetail;
    }

    /**
     * Set the destination details
     *
     * @param jmsDestinationDetail The details to use.
     */
    public void setJmsDestinationDetail( final JMSDestinationDetail jmsDestinationDetail ) {
        this.jmsDestinationDetail = jmsDestinationDetail;
    }

    /**
     * Get the connection information for this destination.
     *
     * @return The connection or null
     */
    @XmlElement(name="JMSConnection")
    public JMSConnection getJmsConnection() {
        return jmsConnection;
    }

    /**
     * Set the connection information for this destination.
     *
     * @param jmsConnection The connection to use.
     */
    public void setJmsConnection( final JMSConnection jmsConnection ) {
        this.jmsConnection = jmsConnection;
    }

    //- PACKAGE

    JMSDestinationMO() {        
    }

    //- PRIVATE

    private JMSDestinationDetail jmsDestinationDetail;
    private JMSConnection jmsConnection;
}
