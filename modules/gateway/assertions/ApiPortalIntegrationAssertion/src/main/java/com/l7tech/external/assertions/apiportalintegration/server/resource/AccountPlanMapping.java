package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.NONE)
public class AccountPlanMapping {
    @XmlElement(name = "Ids", namespace = JAXBResourceMarshaller.NAMESPACE)
    private String ids = StringUtils.EMPTY;

    public AccountPlanMapping() {}

    public AccountPlanMapping(final String ids) {
        setIds(ids);
    }

    public String getIds() {
        return ids;
    }

    public void setIds(String ids) {
        this.ids = ids;
    }
}
