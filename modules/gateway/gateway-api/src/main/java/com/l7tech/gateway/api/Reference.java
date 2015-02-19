package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.Date;
import java.util.List;

/**
 * This represents a reference to a rest entity.
 *
 * @author Victor Kazakov
 */
@XmlTransient
public abstract class Reference<R> {
    private String id;
    private String type;
    private String name;
    private List<Link> links;
    private R content;
    private Date date;

    @XmlElement(name = "Id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlElement(name = "Type", required = true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @XmlElement(name = "Name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement(name = "Link")
    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    @XmlTransient
    public R getContent() {
        return content;
    }

    public void setContent(R content) {
        date = new Date();
        this.content = content;
    }

    @XmlElement(name = "TimeStamp")
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
