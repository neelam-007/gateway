package com.l7tech.server.service;

import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;

import java.util.Collection;
import java.util.Map;

/**
 * Delegating ServiceManager that performs versioning of embedded Policies.
 *
 * <p>Creating / updating a service will activate it's policy and create a policy
 * version.</p>
 *
 * TODO this class should probably not exist, versioning should occur whenever a policy is saved.
 */
public class PolicyVersioningServiceManager implements ServiceManager {

    //- PUBLIC

    public PolicyVersioningServiceManager( final ServiceManager serviceManager,
                                           final PolicyVersionManager policyVersionManager ) {
        this.serviceManager = serviceManager;
        this.policyVersionManager = policyVersionManager;
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return serviceManager.getImpClass();
    }

    @Override
    public void addManageServiceRole(PublishedService service) throws SaveException {
        serviceManager.addManageServiceRole(service);
    }

    @Override
    public Collection<ServiceHeader> findAllHeaders(boolean includeAliases) throws FindException {
        return serviceManager.findAllHeaders(includeAliases);
    }

    @Override
    public String resolveWsdlTarget(String url) {
        return serviceManager.resolveWsdlTarget(url);
    }

    @Override
    public Collection<ServiceHeader> findHeaders(int offset, int windowSize, Map<String,String> filters) throws FindException {
        return serviceManager.findHeaders(offset, windowSize, filters);
    }

    @Override
    public void delete(PublishedService entity) throws DeleteException {
        serviceManager.delete(entity);
    }

    @Override
    public void delete(long oid) throws DeleteException, FindException {
        serviceManager.delete(oid);
    }

    @Override
    public Collection<PublishedService> findAll() throws FindException {
        return serviceManager.findAll();
    }

    @Override
    public Collection<ServiceHeader> findAllHeaders() throws FindException {
        return serviceManager.findAllHeaders();
    }

    @Override
    public Collection<ServiceHeader> findAllHeaders(int offset, int windowSize) throws FindException {
        return serviceManager.findAllHeaders(offset, windowSize);
    }

    @Override
    public PublishedService findByPrimaryKey(long oid) throws FindException {
        return serviceManager.findByPrimaryKey(oid);
    }

    @Override
    public PublishedService findByUniqueName(String name) throws FindException {
        return serviceManager.findByUniqueName(name);
    }

    @Override
    public Map<Long, Integer> findVersionMap() throws FindException {
        return serviceManager.findVersionMap();
    }

    @Override
    public PublishedService getCachedEntity(long o, int maxAge) throws FindException {
        return serviceManager.getCachedEntity(o, maxAge);
    }

    @Override
    public EntityType getEntityType() {
        return serviceManager.getEntityType();
    }

    @Override
    public Class<? extends Entity> getInterfaceClass() {
        return serviceManager.getInterfaceClass();
    }

    @Override
    public String getTableName() {
        return serviceManager.getTableName();
    }

    @Override
    public PublishedService findByHeader(EntityHeader header) throws FindException {
        return serviceManager.findByHeader(header);
    }

    @Override
    public Integer getVersion(long oid) throws FindException {
        return serviceManager.getVersion(oid);
    }

    @Override
    public long save(PublishedService service) throws SaveException {
        long oid = serviceManager.save(service);
        if ( service.getPolicy() != null ) {
            try {
                policyVersionManager.checkpointPolicy(service.getPolicy(), true, true);
            } catch ( ObjectModelException ome ) {
                throw new SaveException("Unable to save policy version when saving service.", ome);
            }
        }
        return oid;
    }

    @Override
    public void update(PublishedService service) throws UpdateException {
        serviceManager.update(service);
        if ( service.getPolicy() != null ) {
            try {
                policyVersionManager.checkpointPolicy(service.getPolicy(), true, false);
            } catch ( ObjectModelException ome ) {
                throw new UpdateException("Unable to save policy version when updating service.", ome);
            }
        }
    }

    @Override
    public void updateWithFolder(PublishedService service) throws UpdateException {
        serviceManager.updateWithFolder(service);
        if ( service.getPolicy() != null ) {
            try {
                policyVersionManager.checkpointPolicy(service.getPolicy(), true, false);
            } catch ( ObjectModelException ome ) {
                throw new UpdateException("Unable to save policy version when updating service.", ome);
            }
        }
    }

    @Override
    public void updateFolder(PublishedService entity, Folder folder) throws UpdateException {
        serviceManager.updateFolder(entity, folder);
    }

    @Override
    public void updateFolder(long entityId, Folder folder) throws UpdateException {
        serviceManager.updateFolder(entityId, folder);
    }

    @Override
    public void createRoles(PublishedService entity) throws SaveException {
        serviceManager.createRoles(entity);
    }

    //- PRIVATE

    private final ServiceManager serviceManager;
    private final PolicyVersionManager policyVersionManager;
}
