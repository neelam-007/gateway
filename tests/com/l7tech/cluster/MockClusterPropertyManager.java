package com.l7tech.cluster;

import java.util.Collection;
import java.util.Map;
import java.util.Collections;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.NamedEntity;

/**
 * Mock CPM, not currently functional, should probably use serverconfig.properties to get default values.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class MockClusterPropertyManager implements ClusterPropertyManager {

    public String getProperty(String key) throws FindException {
        return "";
    }

    public void setProperty(String key, String value) throws SaveException, UpdateException, DeleteException {
    }

    public Collection findAll() throws FindException {
        return Collections.EMPTY_LIST;
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        return Collections.EMPTY_LIST;
    }

    public Collection findAllHeaders() throws FindException {
        return Collections.EMPTY_LIST;
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        return Collections.EMPTY_LIST;
    }

    public Map findVersionMap() throws FindException {
        return Collections.EMPTY_MAP;
    }

    public Entity getCachedEntity(long o, int maxAge) throws FindException, CacheVeto {
        throw new FindException("Not implemented");
    }

    public Integer getVersion(long oid) throws FindException {
        throw new FindException("Not implemented");
    }

    public NamedEntity getCachedEntityByName(String name, int maxAge) throws FindException {
        throw new FindException("Not implemented");
    }
}
