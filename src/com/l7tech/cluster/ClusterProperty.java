/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 15, 2005<br/>
 */
package com.l7tech.cluster;

import java.io.IOException;
import java.io.ObjectOutputStream;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.server.ServerConfig;

/**
 * A row in the cluster_properties table. On the server-side, this is managed through
 * the ClusterPropertyManager, and on the client side, through the ClusterStatusAdmin interface.
 *
 * @author flascelles@layer7-tech.com
 */
public class ClusterProperty extends NamedEntityImp {
    private static final long serialVersionUID = -5971674585207716765L;

    private String value;
    private String description;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        if(description==null) {
            ServerConfig sc = ServerConfig.getInstance();
            String serverPropName = sc.getNameFromClusterName(getName());
            if(serverPropName!=null) {
                description = sc.getPropertyDescription(serverPropName);
            }
            if(description==null) description = "";
        }
        return description;
    }

    /** @return true if this property should be hidden in the cluster property GUI. */
    public boolean isHiddenInGui() {
        // Currently, there's only 1 hidden property, so for now we'll just hardcode it rather than
        // add a whole new DB column and support code
        return "license".equals(_name);
    }

    public boolean equals(Object other) {
        ClusterProperty cp = (ClusterProperty)other;
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

    private void writeObject(ObjectOutputStream out) throws IOException {
        getDescription();
        out.defaultWriteObject();
    }
}