package com.l7tech.external.assertions.ldapupdate.server.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author rraquepo
 */
@XmlRootElement(name = "LDAPOperations", namespace = JAXBResourceUnmarshaller.NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
public class LDAPOperations implements Resource {
    public List<LDAPOperation> getOperations() {
        return operations;
    }

    public void setOperations(List<LDAPOperation> operations) {
        this.operations = operations;
    }

    @XmlElement(name = "LDAPOperation", namespace = JAXBResourceUnmarshaller.NAMESPACE)
    List<LDAPOperation> operations = new ArrayList<LDAPOperation>();
}
