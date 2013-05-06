package com.l7tech.server.ssh.client;

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
    private Integer permissions;

    public XmlSshFile() {
    }

    public XmlSshFile(String name, Boolean file) {
        this.name = name;
        this.file = file;
    }

    public XmlSshFile(String name, Boolean file, long size, long lastModified) {
        this(name, file);
        this.size = size;
        this.lastModified = lastModified;
    }

    public XmlSshFile(String name, Boolean file, long size, long lastModified, int permissions) {
        this(name, file);
        this.size = size;
        this.lastModified = lastModified;
        this.permissions = permissions;
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

    @XmlAttribute
    public Integer getPermissions() {
        return permissions;
    }

    public void setPermissions(Integer permissions) {
        this.permissions = permissions;
    }
}
