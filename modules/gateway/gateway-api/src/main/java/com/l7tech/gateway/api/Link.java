package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * This class represents a reference link.
 */
@XmlRootElement(name = "Link")
@XmlType(name = "LinkType")
public class Link {
    private String uri;
    private String rel;

    Link() {
    }

    Link(String rel, String uri) {
        this.rel = rel;
        this.uri = uri;
    }

    /**
     * Get the link uri
     *
     * @return The link uri
     */
    @XmlAttribute(name = "uri", required = true)
    public String getUri() {
        return uri;
    }

    /**
     * Set the link uri
     *
     * @param uri The link uri
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * The link relation(type)
     *
     * @return The link relation (type)
     */
    @XmlAttribute(name = "rel", required = true)
    public String getRel() {
        return rel;
    }

    /**
     * Set the link relation (type)
     *
     * @param rel The link relation
     */
    public void setRel(String rel) {
        this.rel = rel;
    }
}
