package com.l7tech.external.assertions.apiportalintegration.server.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.NONE)
public class AccountPlanMapping {
    @XmlElement(name = "Id", namespace = JAXBResourceMarshaller.NAMESPACE)
    private List<String> ids = new ArrayList<String>();

    public AccountPlanMapping() {}

    public AccountPlanMapping(final List<String> ids) {
        setIds(ids);
    }

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }
}
