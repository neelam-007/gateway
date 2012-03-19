package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.AuditRecordHeader;
import com.l7tech.gateway.common.audit.AuditSearchCriteria;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Simple audit record manager that delegates to an underlying implementation.
 */
public class DelegatingSimpleAuditRecordManager implements SimpleAuditRecordManager {

    //- PUBLIC

    public DelegatingSimpleAuditRecordManager( @NotNull final SimpleAuditRecordManager delegate ) {
        this.delegate = delegate;
    }

    @Override
    public int findCount( final AuditSearchCriteria criteria ) throws FindException {
        return delegate().findCount( criteria );
    }

    @Override
    public List<AuditRecordHeader> findHeaders( final AuditSearchCriteria criteria ) throws FindException {
        return delegate().findHeaders( criteria );
    }

    @Override
    public Collection<AuditRecord> findPage( final SortProperty sortProperty, final boolean ascending, final int offset, final int count, final AuditSearchCriteria criteria ) throws FindException {
        return delegate().findPage( sortProperty, ascending, offset, count, criteria );
    }

    @Override
    public Map<Long, byte[]> getDigestForAuditRecords( final Collection<Long> auditRecordIds ) throws FindException {
        return delegate().getDigestForAuditRecords( auditRecordIds );
    }

    @Override
    public void delete( final AuditRecord entity ) throws DeleteException {
        delegate().delete( entity );
    }

    @Override
    public void delete( final long oid ) throws DeleteException, FindException {
        delegate().delete( oid );
    }

    @Override
    public Collection<AuditRecord> findAll() throws FindException {
        return delegate().findAll();
    }

    @Override
    public Collection<AuditRecordHeader> findAllHeaders() throws FindException {
        return delegate().findAllHeaders();
    }

    @Override
    public Collection<AuditRecordHeader> findAllHeaders( final int offset, final int windowSize ) throws FindException {
        return delegate().findAllHeaders( offset, windowSize );
    }

    @Override
    @Nullable
    public AuditRecord findByHeader( final EntityHeader header ) throws FindException {
        return delegate().findByHeader( header );
    }

    @Nullable
    @Override
    public AuditRecord findByPrimaryKey( final long oid ) throws FindException {
        return delegate().findByPrimaryKey( oid );
    }

    @Override
    @Nullable
    public AuditRecord findByUniqueName( final String name ) throws FindException {
        return delegate().findByUniqueName( name );
    }

    @Override
    public Map<Long, Integer> findVersionMap() throws FindException {
        return delegate().findVersionMap();
    }

    @Override
    @Nullable
    public AuditRecord getCachedEntity( final long o, final int maxAge ) throws FindException {
        return delegate().getCachedEntity( o, maxAge );
    }

    @Override
    public EntityType getEntityType() {
        return delegate().getEntityType();
    }

    @Override
    public String getTableName() {
        return delegate().getTableName();
    }

    @Override
    public Class<? extends Entity> getInterfaceClass() {
        return delegate().getInterfaceClass();
    }

    @Override
    public Integer getVersion( final long oid ) throws FindException {
        return delegate().getVersion( oid );
    }

    @Override
    public long save( final AuditRecord entity ) throws SaveException {
        return delegate().save( entity );
    }

    @Override
    public void update( final AuditRecord entity ) throws UpdateException {
        delegate().update( entity );
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return delegate().getImpClass();
    }

    //- PROTECTED

    /**
     * Subclasses using this constructor must override <code>delegate()</code>
     */
    protected DelegatingSimpleAuditRecordManager() {
        delegate = null;
    }

    protected SimpleAuditRecordManager delegate() {
        return delegate;
    }

    //- PRIVATE

    private final SimpleAuditRecordManager delegate;
}
