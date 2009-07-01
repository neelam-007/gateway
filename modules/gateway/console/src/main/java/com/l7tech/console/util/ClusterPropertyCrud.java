package com.l7tech.console.util;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.gateway.common.cluster.ClusterProperty;

/**
 * Utility class on SSM side to simplify CRUD of cluster properties.
 * <p/>
 * The actual admin interface can't just use get and put since that doesn't fit in with the RBAC model.
 * But we can implement a friendly get/put interface on the client side.
 * <p/>
 * All methods in this class require that the SSM is currently connected to a Gateway.
 */
public class ClusterPropertyCrud {
    /**
     * Get the specified cluster property from the currently-connected Gateway.
     *
     * @param propname the name of the cluster property to get.  Required.
     * @return the value of this cluster property, or null if the property is not set.
     * @throws FindException if the Gateway has a problem accessing its database
     */
    public static String getClusterProperty(String propname) throws FindException {
        ClusterProperty prop = Registry.getDefault().getClusterStatusAdmin().findPropertyByName(propname);
        return prop == null ? null : prop.getValue();
    }

    /**
     * Set the specified cluster property to the specified value, regardless of whether it currently
     * exists.
     *
     * @param propname the name of the cluster property to set.  Required.
     * @param value the value to set it to, or null to delete this cluster property.
     * @throws ObjectModelException if the Gateway has a problem accessing or modifying its database.
     */
    public static void putClusterProperty(String propname, String value) throws ObjectModelException {
        ClusterProperty prop = Registry.getDefault().getClusterStatusAdmin().findPropertyByName(propname);
        if (prop == null) {
            if (value != null) {
                prop = new ClusterProperty(propname, value);
                Registry.getDefault().getClusterStatusAdmin().saveProperty(prop);
            }
        } else {
            if (value == null) {
                Registry.getDefault().getClusterStatusAdmin().deleteProperty(prop);
            } else {
                prop.setValue(value);
                Registry.getDefault().getClusterStatusAdmin().saveProperty(prop);
            }
        }
    }

    /**
     * Ensure that the specified cluster property does not exist, regardless of whether it currently exists.
     * <p/>
     * This is identical to calling {@link #putClusterProperty(String, String)} with null as the second argument.
     * @param propname the name of the cluster property to delete.  Required.
     * @throws ObjectModelException if the Gateway has a problem accessing or modifying its database.
     */
    public static void deleteClusterProperty(String propname) throws ObjectModelException {
        putClusterProperty(propname, null);
    }
}
