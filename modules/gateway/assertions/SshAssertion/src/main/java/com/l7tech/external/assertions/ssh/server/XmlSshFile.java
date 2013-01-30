package com.l7tech.external.assertions.ssh.server;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class is used to deserialize an xml file
 *
 * @author Victor Kazakov
 */

@XmlRootElement(name = "file")
public class XmlSshFile {
    private String name;
    private Long size;
    private Long lastModified;
    private Boolean file;

    public XmlSshFile() {
    }

    public XmlSshFile(String name, Boolean file) {
        this.name = name;
        this.file = file;
    }

    public XmlSshFile(String name, long size, long lastModified) {
        this.name = name;
        this.size = size;
        this.lastModified = lastModified;
    }

    @XmlAttribute
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlAttribute
    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    @XmlAttribute
    public Long getLastModified() {
        return lastModified;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    @XmlAttribute
    public Boolean isFile() {
        return file;
    }

    public void setFile(Boolean file) {
        this.file = file;
    }
}
