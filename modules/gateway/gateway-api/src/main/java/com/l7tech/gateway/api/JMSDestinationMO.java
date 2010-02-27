package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorFactory;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * The JMSDestinationMO managed object represents a JMS Queue.
 *
 * <p>The Accessor for JMS destinations is read only. JMS destinations can be
 * accessed by identifier only.</p>
 *
 * @see ManagedObjectFactory#createJMSDestination()
 */
@XmlRootElement(name="JMSDestination")
@XmlType(name="JMSDestinationType", propOrder={"jmsDestinationDetails","jmsConnection","extensions"})
@AccessorFactory.AccessibleResource(name ="jmsDestinations")
public class JMSDestinationMO extends AccessibleObject {

    //- PUBLIC

    @Override
    public Integer getVersion() {
        Integer version = null;

        if ( jmsDestinationDetails != null ) {
            version = jmsDestinationDetails.getVersion();
        }

        return version;
    }

    @Override
    public void setVersion( final Integer version ) {
        if ( jmsDestinationDetails != null ) {
            jmsDestinationDetails.setVersion( version );
        }
    }

    /**
     * Get the destination details (required)
     *
     * @return The details or null
     */
    @XmlElement(name="JMSDestinationDetails", required=true)
    public JMSDestinationDetails getJmsDestinationDetails() {
        return jmsDestinationDetails;
    }

    /**
     * Set the destination details
     *
     * @param jmsDestinationDetails The details to use.
     */
    public void setJmsDestinationDetails( final JMSDestinationDetails jmsDestinationDetails ) {
        this.jmsDestinationDetails = jmsDestinationDetails;
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

    //- PROTECTED

    @XmlAnyElement(lax=true)
    @Override
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }

    @Override
    protected void setExtensions( final List<Object> extensions ) {
        super.setExtensions( extensions );
    }

    //- PACKAGE

    JMSDestinationMO() {        
    }

    //- PRIVATE

    private JMSDestinationDetails jmsDestinationDetails;
    private JMSConnection jmsConnection;
}
