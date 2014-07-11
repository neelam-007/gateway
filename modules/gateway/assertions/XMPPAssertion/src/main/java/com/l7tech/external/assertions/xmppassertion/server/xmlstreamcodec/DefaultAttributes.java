package com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec;

import org.xml.sax.Attributes;

import java.util.Collections;
import java.util.List;

/**
 * This is from the Apache Vysper project (org.apache.vysper.xml.sax.impl).
 * Trying to minimize changes from the original file so that we could switch
 * to using the vysper library.
 */
public class DefaultAttributes implements Attributes {
    private List<Attribute> attributes;

    public DefaultAttributes() {
        attributes = Collections.emptyList();
    }

    public DefaultAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    @Override
    public int getIndex(String qName) {
        for(int i = 0;i < attributes.size();i++) {
            Attribute attribute = attributes.get(i);
            if(qName.equals(attribute.getQname())) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public int getIndex(String uri, String localName) {
        for(int i = 0;i < attributes.size();i++) {
            Attribute attribute = attributes.get(i);
            if(uri.equals(attribute.getURI()) && localName.equals(attribute.getLocalName())) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public int getLength() {
        return -1;
    }

    @Override
    public String getLocalName(int index) {
        if(index < 0 || index >= attributes.size()) {
            return null;
        }

        return attributes.get(index).getLocalName();
    }

    @Override
    public String getQName(int index) {
        if(index < 0 || index >= attributes.size()) {
            return null;
        }

        return attributes.get(index).getQname();
    }

    @Override
    public String getType(int index) {
        if(index < 0 || index >= attributes.size()) {
            return null;
        }

        return "CDATA";
    }

    @Override
    public String getType(String qName) {
        return getType(getIndex(qName));
    }

    @Override
    public String getType(String uri, String localName) {
        return getType(getType(uri, localName));
    }

    @Override
    public String getURI(int index) {
        if(index < 0 || index >= attributes.size()) {
            return null;
        }

        return attributes.get(index).getURI();
    }

    @Override
    public String getValue(int index) {
        if(index < 0 || index >= attributes.size()) {
            return null;
        }

        return attributes.get(index).getValue();
    }

    @Override
    public String getValue(String qName) {
        return getValue(getIndex(qName));
    }

    @Override
    public String getValue(String uri, String localName) {
        return getValue(getIndex(uri, localName));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if(obj == null) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }

        DefaultAttributes other = (DefaultAttributes)obj;
        if(attributes == null) {
            if(other.attributes != null) {
                return false;
            }
        } else if(attributes.equals(other.attributes)) {
            return false;
        }

        return true;
    }
}
