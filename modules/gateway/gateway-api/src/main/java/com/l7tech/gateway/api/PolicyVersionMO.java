package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * This is the policy version MO
 *
 * @author Victor Kazakov
 */
@XmlRootElement(name = "PolicyVersion")
@XmlType(name = "PolicyVersionType", propOrder = {"ordinal", "comment", "policyId", "time", "active", "xml", "extension", "extensions"})
public class PolicyVersionMO extends ElementExtendableAccessibleObject {
    private String id;
    private long ordinal;
    private String comment;
    private String policyId;
    private long time;
    private boolean active;
    private String xml;

    PolicyVersionMO() {
    }

    @XmlElement(name = "ordinal")
    public long getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(long ordinal) {
        this.ordinal = ordinal;
    }

    @XmlElement(name = "comment")
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @XmlElement(name = "policyId")
    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    @XmlElement(name = "time")
    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @XmlElement(name = "active")
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @XmlElement(name = "xml")
    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }
}
