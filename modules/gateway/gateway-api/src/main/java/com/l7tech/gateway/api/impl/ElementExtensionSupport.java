package com.l7tech.gateway.api.impl;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.List;

/**
 * Support class for implementing extensions (including elements).
 */
@XmlTransient
public class ElementExtensionSupport extends ExtensionSupport {

    @XmlElement(name="Extension")
    @Override
    protected Extension getExtension() {
        return super.getExtension();
    }

    @XmlAnyElement(lax=true)
    @Override
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }
}
