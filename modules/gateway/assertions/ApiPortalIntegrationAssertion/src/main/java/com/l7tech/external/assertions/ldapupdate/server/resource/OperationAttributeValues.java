package com.l7tech.external.assertions.ldapupdate.server.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class for a list of values required because jaxb does not support marshalling of Lists.
 *
 * @author rraquepo
 */
@XmlAccessorType(XmlAccessType.NONE)
public class OperationAttributeValues {
    public OperationAttributeValues() {
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    @XmlElement(name = "value", namespace = JAXBResourceUnmarshaller.NAMESPACE)
    List<String> values = new ArrayList<String>();
}
