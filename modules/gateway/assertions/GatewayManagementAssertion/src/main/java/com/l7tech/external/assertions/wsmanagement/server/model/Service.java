package com.l7tech.external.assertions.wsmanagement.server.model;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlAttribute;

/**
 *  <Service type="soap" enabled="false">
 *  <Name>Warehouse</Name>
 *  <OperationValidation>false</OperationValidation>
 *  <TransportBindings>
 *    <HttpTransportBinding>
 *      <ResolutionUri>/asdf</ResolutionUri>
 *      <HttpMethods>
 *        <HttpMethod>POST</HttpMethod>
 *      </HttpMethods>
 *    </HttpTransportBinding>
 *  </TransportBindings>
 *  <ResourceReference type="wsdl" url="#wsdl"/>
 *  <ResourceReference type="policy" url="#policy"/>
 *  <ResourceBag>
 *     ...
 *  </ResourceBag>
 *</Service>
 *
 */
@XmlRootElement(name="Service")
@XmlType(propOrder={"name","operationValidation"})
public class Service {

    //- PUBLIC

    @XmlAttribute(required=true)
    public String getType() {
        return type;
    }

    public void setType( final String type ) {
        this.type = type;
    }

    @XmlAttribute(required=true)
    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled( final boolean enabled ) {
        this.enabled = enabled;
    }

    @XmlElement(name="Name", required=true)
    public String getName() {
        return name;
    }

    public void setName( final String name ) {
        this.name = name;
    }

    @XmlElement(name="OperationValidation", required=true)
    public boolean getOperationValidation() {
        return operationValidation;
    }

    public void setOperationValidation( final boolean operationValidation ) {
        this.operationValidation = operationValidation;
    }

    //- PRIVATE

    private String type;
    private boolean enabled;
    private String name;
    private boolean operationValidation; //TODO [steve] this is a SOAP option
    
}
