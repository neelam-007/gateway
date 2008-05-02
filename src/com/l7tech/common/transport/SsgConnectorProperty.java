package com.l7tech.common.transport;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Represents an arbitrary property associated with an SsgConnector instance.  This allows connectors to be configured
 * that didn't exist when a particular Gateway core's hibernate mappings file was written (ie, added via .aar files).
 */
@XmlRootElement
public class SsgConnectorProperty extends NamedEntityImp {
    private static final long serialVersionUID = 1L;

    private SsgConnector connector;
    private String value;

    protected SsgConnectorProperty() { 
    }

    public SsgConnectorProperty(SsgConnector connector, String name, String value) {
        this.connector = connector;
        this._name = name;
        this.value = value;
    }

    /*This is very important for Jaxb. Without this @XmlTransient annotation marshalling an SsgConnector
    * will go into an infinite loop as the SsgConnector to which 'this' SsgConnectorProperty belongs is what
    * causes this objct to be marshalled which in turn causes it's parent to be marshalled due to this
    * bi-directional relationship, which causes the infinite loop behaviour.*/
    @XmlTransient
    public SsgConnector getConnector() {
        return connector;
    }

    public void setConnector(SsgConnector connector) {
        this.connector = connector;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SsgConnectorProperty that = (SsgConnectorProperty)o;

        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
