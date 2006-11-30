/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 15, 2005<br/>
 */
package com.l7tech.cluster;

import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * A row in the cluster_properties table. On the server-side, this is managed through
 * the ClusterPropertyManager, and on the client side, through the ClusterStatusAdmin interface.
 *
 * @author flascelles@layer7-tech.com
 */
public class ClusterProperty extends NamedEntityImp {
    private static final long serialVersionUID = 1L;

    private String value;
    private String description;

    public ClusterProperty() { }

    public ClusterProperty(String name, String value) {
        this._name = name;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** @return true if this property should be hidden in the cluster property GUI. */
    public boolean isHiddenInGui() {
        // Currently, there's only 1 hidden property, so for now we'll just hardcode it rather than
        // add a whole new DB column and support code
        return "license".equals(_name)
            || "audit.acknowledge.highestTime".equals(_name);
    }

    public boolean equals(Object other) {
        ClusterProperty cp = (ClusterProperty)other;
        if (cp == null) return false;
        if (_oid != cp._oid) return false;
        if (!(_name.equals(cp._name))) return false;
        if (!(value.equals(cp.value))) return false;
        return true;
    }

    public int hashCode() {
        int result;
        result = (value != null ? value.hashCode() : 0);
        result = 29 * result + (_name != null ? _name.hashCode() : 0);
        result = 29 * result + (int)(_oid ^ (_oid >>> 32));
        return result;
    }

}