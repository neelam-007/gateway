package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * This was created: 11/4/13 as 3:24 PM
 *
 * @author Victor Kazakov
 */
@XmlRootElement(name = "DependentObject")
@XmlType(propOrder = {"name", "objectType", "id", "uri"})
public class DependentObjectMO {
    private String name;
    private String objectType;
    private String id;
    private String uri;

    DependentObjectMO(){}

    @XmlElement(name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement(name = "objectType")
    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    @XmlElement(name = "id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlElement(name = "uri")
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
