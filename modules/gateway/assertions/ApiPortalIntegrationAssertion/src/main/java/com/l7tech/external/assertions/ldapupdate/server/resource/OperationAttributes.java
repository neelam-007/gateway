package com.l7tech.external.assertions.ldapupdate.server.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class for a list of attributes required because jaxb does not support marshalling of Lists.
 *
 * @author rraquepo
 */
@XmlRootElement(name = "attributes", namespace = JAXBResourceUnmarshaller.NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
public class OperationAttributes {
    public OperationAttributes() {
    }

    public List<OperationAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<OperationAttribute> attributes) {
        this.attributes = attributes;
    }

    @XmlElement(name = "attribute", namespace = JAXBResourceUnmarshaller.NAMESPACE)
    List<OperationAttribute> attributes = new ArrayList<OperationAttribute>();
}
