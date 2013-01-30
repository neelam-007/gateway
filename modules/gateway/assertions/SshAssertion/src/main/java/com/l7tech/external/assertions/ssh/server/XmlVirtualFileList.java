package com.l7tech.external.assertions.ssh.server;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * This class is used to deserialize a file list represented in xml
 *
 * @author Victor Kazakov
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "files")
public class XmlVirtualFileList {

    @XmlElement(name = "file", type=XmlSshFile.class)
    private List<XmlSshFile> fileList;

    public XmlVirtualFileList() {
    }

    public XmlVirtualFileList(List<XmlSshFile> fileList) {
        this.fileList = fileList;
    }

    public List<XmlSshFile> getFileList() {
        return fileList;
    }

    public void setFileList(List<XmlSshFile> fileList) {
        this.fileList = fileList;
    }
}
