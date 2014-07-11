package com.l7tech.external.assertions.xmppassertion.server;


import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.*;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * User: ashah
 * Date: 24/04/12
 * Time: 5:30 PM
 */
public class MockJdbcConnectionManager implements JdbcConnectionManager {
    @Override
    public JdbcConnection getJdbcConnection(String connectionName) throws FindException {
        return null;
    }

    @Nullable
    @Override
    public JdbcConnection findByPrimaryKey(Goid goid) throws FindException {
        return null;
    }

    @Override
    public Collection<EntityHeader> findAllHeaders() throws FindException {
        return null;
    }

    @Override
    public Collection<EntityHeader> findAllHeaders(int offset, int windowSize) throws FindException {
        return null;
    }

    @Override
    public Collection<JdbcConnection> findAll() throws FindException {
        return null;
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return null;
    }

    @Override
    public Goid save(JdbcConnection entity) throws SaveException {
        return null;
    }

    @Override
    public void save( Goid id, JdbcConnection entity ) throws SaveException {

    }

    @Override
    public Integer getVersion(Goid goid) throws FindException {
        return null;
    }

    @Override
    public Map<Goid, Integer> findVersionMap() throws FindException {
        return null;
    }

    @Override
    public void delete(JdbcConnection entity) throws DeleteException {
    }

    @Nullable
    @Override
    public JdbcConnection getCachedEntity(Goid goid, int maxAge) throws FindException {
        return null;
    }

    @Override
    public Class<? extends Entity> getInterfaceClass() {
        return null;
    }

    @Override
    public EntityType getEntityType() {
        return null;
    }

    @Override
    public String getTableName() {
        return null;
    }

    @Override
    public JdbcConnection findByUniqueName(String name) throws FindException {
        return null;
    }

    @Override
    public void delete(Goid goid) throws DeleteException, FindException {
    }

    @Override
    public void update(JdbcConnection entity) throws UpdateException {
    }

    @Override
    public JdbcConnection findByHeader(EntityHeader header) throws FindException {
        return null;
    }

    @Override
    public List<JdbcConnection> findPagedMatching( int offset, int count, @Nullable String sortProperty, @Nullable Boolean ascending, @Nullable Map<String, List<Object>> matchProperties ) throws FindException {
        return null;
    }

    @Override
    public List<String> getSupportedDriverClass() {
        return null;
    }

    @Override
    public boolean isDriverClassSupported(String driverClass) {
        return false;
    }
}
