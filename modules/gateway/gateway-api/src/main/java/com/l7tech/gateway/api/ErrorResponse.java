package com.l7tech.gateway.api;


import javax.xml.bind.annotation.*;
import java.util.Date;

/**
 * This represents a error response
 *
 */
@XmlRootElement(name = "Error")
@XmlType(name = "Error", propOrder = {"type", "timestamp", "link", "detail",})
public class ErrorResponse {
    private String detail;
    private Link link;
    private String type;
    private Date timestamp;

    ErrorResponse() {
    }

    @XmlElement(name = "Detail")
    public String getDetail() {
        return detail;
    }

    public void setDetail(String message) {
        this.detail = message;
    }

    @XmlElement(name = "Link", required = true)
    public Link getLink() {
        return link;
    }

    public void setLink(Link link) {
        this.link = link;
    }

    @XmlElement(name = "Type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @XmlElement(name = "TimeStamp")
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

}
