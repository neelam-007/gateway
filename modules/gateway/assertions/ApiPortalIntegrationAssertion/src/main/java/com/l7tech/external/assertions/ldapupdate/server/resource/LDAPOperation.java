package com.l7tech.external.assertions.ldapupdate.server.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author rraquepo
 */
@XmlRootElement(name = "LDAPOperation", namespace = JAXBResourceUnmarshaller.NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
public class LDAPOperation implements Resource {
    @XmlElement(name = "operation", namespace = JAXBResourceUnmarshaller.NAMESPACE, nillable = false)
    private String operation;
    @XmlElement(name = "dn", namespace = JAXBResourceUnmarshaller.NAMESPACE, nillable = false)
    private String dn;
    @XmlElement(name = "attributes", namespace = JAXBResourceUnmarshaller.NAMESPACE)
    private OperationAttributes attributes = new OperationAttributes();

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public OperationAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(OperationAttributes attributes) {
        this.attributes = attributes;
    }
}
