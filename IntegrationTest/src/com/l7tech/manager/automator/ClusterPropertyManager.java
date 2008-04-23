package com.l7tech.manager.automator;

import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.cluster.ClusterProperty;
import com.l7tech.admin.AdminContext;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;

/**
 * Retrieves and updates cluster properties.
 */
public class ClusterPropertyManager {
    private ClusterStatusAdmin clusterStatusAdmin;

    /**
     * Creates a new instance of ClusterPropertyManager.
     *
     * @param adminContext
     */
    public ClusterPropertyManager(AdminContext adminContext) {
        clusterStatusAdmin = adminContext.getClusterStatusAdmin();
    }

    /**
     * Returns the value of the specified cluster property.
     *
     * @param name The name of the cluster property
     * @return The cluster property's value
     * @throws FindException
     */
    public String getClusterPropertyValue(String name) throws FindException {
        ClusterProperty cp = clusterStatusAdmin.findPropertyByName(name);
        return cp.getValue();
    }

    /**
     * Updates the value of a cluster property.
     * 
     * @param name The name of the cluster property
     * @param value The new value of the cluster property
     * @throws FindException
     * @throws SaveException
     * @throws UpdateException
     * @throws DeleteException
     */
    public void setClusterProperty(String name, String value) throws FindException, SaveException, UpdateException, DeleteException {
        ClusterProperty cp = clusterStatusAdmin.findPropertyByName(name);
        if(cp != null) {
            cp.setValue(value);
            clusterStatusAdmin.saveProperty(cp);
            System.out.println("Updated cluster property \"" + name + "\" to \"" + value + "\"");
        }
    }
}
