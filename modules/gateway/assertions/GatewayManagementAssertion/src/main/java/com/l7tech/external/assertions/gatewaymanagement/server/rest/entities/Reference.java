package com.l7tech.external.assertions.gatewaymanagement.server.rest.entities;

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
@XmlType(name = "ReferenceType")
public class Reference {
    private String href;
    //this is the xlink type. We currently only use simple xlinks
    private static final String xlinkType = "simple";
    private String content;

    public Reference() {
    }

    public Reference(String href, String content) {
        this.href = href;
        this.content = content;
    }

    /**
     * The uri to retrieve the entity from.
     *
     * @return The uri to retrieve the entity from
     */
    @XmlAttribute(name = "href", namespace = "http://www.w3.org/1999/xlink")
    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    /**
     * The type of xlink. Right now this is aways 'simple'
     *
     * @return 'simple'
     */
    @XmlAttribute(name = "type", namespace = "http://www.w3.org/1999/xlink")
    public String getXlinkType() {
        return xlinkType;
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
