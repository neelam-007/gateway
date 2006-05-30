package com.l7tech.cluster;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.NamedEntity;

/**
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public interface ClusterPropertyManager extends EntityManager {
    public NamedEntity getCachedEntityByName(String name, int maxAge) throws FindException;
    String getProperty(String key) throws FindException;
    void setProperty(String key, String value) throws SaveException, UpdateException, DeleteException;
}
