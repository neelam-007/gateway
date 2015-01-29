package com.l7tech.gateway.api.impl;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 * Support class for implementing extensions.
 */
@XmlTransient
public class ExtensionSupport {

    //- PROTECTED

    @XmlAnyAttribute
    protected Map<QName, Object> getAttributeExtensions() {
        return attributeExtensions;
    }

    final protected void setAttributeExtensions( final Map<QName, Object> attributeExtensions ) {
        this.attributeExtensions = attributeExtensions;
    }

    protected Extension getExtension() {
        return extension;
    }

    protected void setExtension( final Extension extension ) {
        this.extension = extension;
    }

    protected List<Object> getExtensions() {
        return extensions;
    }

    protected final void setExtensions( final List<Object> extensions ) {
        this.extensions = extensions;
    }

    //- PRIVATE

    private Extension extension;
    private List<Object> extensions;
    private Map<QName,Object> attributeExtensions;

}
