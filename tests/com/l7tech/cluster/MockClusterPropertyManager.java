package com.l7tech.cluster;

import com.l7tech.objectmodel.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

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

    public Entity findByPrimaryKey(long oid) throws FindException {
        return null;
    }

    public long save(Entity entity) throws SaveException {
        return 0;
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

    public void delete(Entity entity) throws DeleteException {
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
