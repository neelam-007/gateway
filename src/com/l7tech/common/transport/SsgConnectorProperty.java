package com.l7tech.common.transport;

import com.l7tech.objectmodel.imp.NamedEntityImp;


/**
 * Represents an arbitrary property associated with an SsgConnector instance.  This allows connectors to be configured
 * that didn't exist when a particular Gateway core's hibernate mappings file was written (ie, added via .aar files).
 */
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
