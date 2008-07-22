package com.l7tech.wsdl;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Iterator;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class BindingOperationInfo implements Cloneable, Serializable {

    //- PUBLIC

    public BindingOperationInfo(String name, Map multipart) {
        this.name = name;
        this.multipart.putAll(multipart);
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

    /**
     * Map of (String) names to (MimePartInfo) values
     *
     * @return the (possibly empty) mutable Map.
     */
    public Map getMultipart() {
        return multipart;
    }

    public void setMultipart(Map multipart) {
        this.multipart.clear();
        this.multipart.putAll(multipart);
    }

    /**
     * Map of (String) content-types to (MimePartInfo) values
     *
     * @return the (possibly empty) mutable Map.
     */
    public Map getExtraMultipart() {
        return extraMultipart;
    }

    public void setExtraMultipart(Map extraMultipart) {
        this.extraMultipart.clear();
        this.extraMultipart.putAll(extraMultipart);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BindingOperationInfo)) return false;

        final BindingOperationInfo bindingOperationInfo = (BindingOperationInfo) o;

        if (multipart != null ? !multipart.equals(bindingOperationInfo.multipart) : bindingOperationInfo.multipart != null) return false;
        if (extraMultipart != null ? !extraMultipart.equals(bindingOperationInfo.extraMultipart) : bindingOperationInfo.extraMultipart != null) return false;
        if (name != null ? !name.equals(bindingOperationInfo.name) : bindingOperationInfo.name != null) return false;
        if (xpath != null ? !xpath.equals(bindingOperationInfo.xpath) : bindingOperationInfo.xpath != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 29 * result + (xpath != null ? xpath.hashCode() : 0);
        result = 29 * result + (multipart != null ? multipart.hashCode() : 0);
        result = 29 * result + (extraMultipart != null ? extraMultipart.hashCode() : 0);
        return result;
    }

    public Object clone() {
        try {
            BindingOperationInfo clone = (BindingOperationInfo) super.clone();
            clone.multipart = (Map)((LinkedHashMap)multipart).clone();
            clone.extraMultipart = (Map)((LinkedHashMap)extraMultipart).clone();
            
            if (clone.multipart != null) {
                for(Iterator iterator = clone.multipart.entrySet().iterator(); iterator.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    entry.setValue(((MimePartInfo)entry.getValue()).clone());
                }
            }
            if (clone.extraMultipart != null) {
                for(Iterator iterator = clone.extraMultipart.entrySet().iterator(); iterator.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    entry.setValue(((MimePartInfo)entry.getValue()).clone());
                }
            }
            return clone;
        }
        catch(CloneNotSupportedException cnse) {
            throw new RuntimeException("Clone not supported", cnse);
        }
    }

    //- PROTECTED

    /**
     * Constructor for policy serializer
     */
    public BindingOperationInfo() {
    }

    //- PRIVATE

    private String name;
    private String xpath;
    private Map multipart = new LinkedHashMap();    // list of MimePartInfo
    private Map extraMultipart = new LinkedHashMap();    // list of extra MimePartInfo

}
