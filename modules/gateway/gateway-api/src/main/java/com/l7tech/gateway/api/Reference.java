package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * This represents a reference to a rest entity.
 *
 * @author Victor Kazakov
 */
@XmlRootElement(name = "Reference")
@XmlType(name = "EntityReferenceType")
public class Reference {
    private String href;
    private String entityId;
    private String entityType;
    private String content;

    Reference() {
    }

    Reference(String href, String entityId, String entityType, String content) {
        this.href = href;
        this.content = content;
        this.entityId = entityId;
        this.entityType = entityType;
    }

    /**
     * The uri to retrieve the entity from.
     *
     * @return The uri to retrieve the entity from
     */
    @XmlAttribute(name = "href")
    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    @XmlAttribute(name = "entityId")
    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    @XmlAttribute(name = "entityType")
    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    /**
     * The content of the reference element
     *
     * @return The content
     */
    @XmlValue
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
