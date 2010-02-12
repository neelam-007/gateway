package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorFactory;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * 
 */
@XmlRootElement(name="JMSDestination")
@XmlType(name="JMSDestinationType", propOrder={"jmsDestinationDetails","jmsConnection","extensions"})
@AccessorFactory.ManagedResource(name ="jmsDestinations")
public class JMSDestinationMO extends ManagedObject {

    //- PUBLIC

    @XmlTransient
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

    @XmlElement(name="JMSDestinationDetails", required=true)
    public JMSDestinationDetails getJmsDestinationDetails() {
        return jmsDestinationDetails;
    }

    public void setJmsDestinationDetails( final JMSDestinationDetails jmsDestinationDetails ) {
        this.jmsDestinationDetails = jmsDestinationDetails;
    }

    @XmlElement(name="JMSConnection")
    public JMSConnection getJmsConnection() {
        return jmsConnection;
    }

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
