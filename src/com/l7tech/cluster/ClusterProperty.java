/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 15, 2005<br/>
 */
package com.l7tech.cluster;

import java.io.Serializable;

/**
 * A row in the cluster_properties table. On the server-side, this is managed through
 * the ClusterPropertyManager, and on the client side, through the ClusterStatusAdmin interface.
 *
 * @author flascelles@layer7-tech.com
 */
public class ClusterProperty implements Serializable {
    private static final long serialVersionUID = -5971674585207716763L;

    private String key;
    private String value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /** @return true if this property should be hidden in the cluster property GUI. */
    public boolean isHiddenInGui() {
        // Currently, there's only 1 hidden property, so for now we'll just hardcode it rather than
        // add a whole new DB column and support code
        return "license".equals(key);
    }
}
