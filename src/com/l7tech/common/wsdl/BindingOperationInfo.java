package com.l7tech.common.wsdl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class BindingOperationInfo implements Serializable {
    protected String name;
    protected String xpath;
    protected Map multipart = new HashMap();    // list of MimePartInfo

    public BindingOperationInfo() {
    }

    public BindingOperationInfo(String name, Map multipart) {
        this.name = name;
        this.multipart = multipart;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getXpath() {
        return xpath;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    public Map getMultipart() {
        return multipart;
    }

    public void setMultipart(Map multipart) {
        this.multipart = multipart;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BindingOperationInfo)) return false;

        final BindingOperationInfo bindingOperationInfo = (BindingOperationInfo) o;

        if (multipart != null ? !multipart.equals(bindingOperationInfo.multipart) : bindingOperationInfo.multipart != null) return false;
        if (name != null ? !name.equals(bindingOperationInfo.name) : bindingOperationInfo.name != null) return false;
        if (xpath != null ? !xpath.equals(bindingOperationInfo.xpath) : bindingOperationInfo.xpath != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 29 * result + (xpath != null ? xpath.hashCode() : 0);
        result = 29 * result + (multipart != null ? multipart.hashCode() : 0);
        return result;
    }
}
