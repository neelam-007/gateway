package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.ManagedObject;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.List;

/**
 * Extension of ManagedObject with element extension support.
 */
@XmlTransient
public abstract class ElementExtendableManagedObject extends ManagedObject {

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
