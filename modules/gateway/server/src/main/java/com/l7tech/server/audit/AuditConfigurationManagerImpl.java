package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditConfiguration;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.transport.ftp.FtpUtils;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.Policy;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Option;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Manages the finding and saving of {@link AuditConfiguration}s.
 *
 * Note configurations are saved as hidden cluster properties
 *
 * @author alex
 * @version $Revision$
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class AuditConfigurationManagerImpl implements AuditConfigurationManager
{
    @Inject
    private ClusterPropertyManager clusterPropertyManager;

    @Inject
    private PolicyManager policyManager;

    @Inject
    private PlatformTransactionManager transactionManager;

    public AuditConfigurationManagerImpl(PolicyManager policyManager, ClusterPropertyManager clusterPropertyManager, PlatformTransactionManager platformTransactionManager) {
        this.clusterPropertyManager = clusterPropertyManager;
        this.policyManager = policyManager;
        this.transactionManager = platformTransactionManager;
    }

    @Nullable
    @Override
    public AuditConfiguration findByPrimaryKey(@Nullable Goid goid) throws FindException {
        if(goid == null || goid.equals(AuditConfiguration.ENTITY_ID)) {
            return getEntity();
        }
        return null;
    }

    public Collection<EntityHeader> findAllHeaders() throws FindException {
        return CollectionUtils.list(new EntityHeader(AuditConfiguration.ENTITY_ID,EntityType.AUDIT_CONFIG,AuditConfiguration.ENTITY_NAME,""));
    }

    @Override
    public Collection<EntityHeader> findAllHeaders(int offset, int windowSize) throws FindException {
        return findAllHeaders();
    }

    public Collection<AuditConfiguration> findAll() throws FindException {
        return CollectionUtils.list(getEntity());
    }

    public Class<? extends Entity> getImpClass() {
        return AuditConfiguration.class;
    }

    public Goid save(AuditConfiguration entity) throws SaveException {
        try {
            update(entity);
        } catch (UpdateException e) {
            throw new SaveException(e.getMessage(),e);
        }
        return AuditConfiguration.ENTITY_ID;
    }

    @Override
    public void save(Goid id, AuditConfiguration entity) throws SaveException {
        if(AuditConfiguration.ENTITY_ID.equals(id)){
            try {
                update(entity);
            } catch (UpdateException e) {
                throw new SaveException(e.getMessage(),e);
            }
        }else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Integer getVersion(Goid goid) throws FindException {
        return 0;
    }

    @Override
    public Map<Goid, Integer> findVersionMap() throws FindException {
        return new HashMap<>();
    }

    @Override
    public void delete(AuditConfiguration entity) throws DeleteException {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public AuditConfiguration getCachedEntity(Goid goid, int maxAge) throws FindException {
        return null;
    }

    public Class<? extends Entity> getInterfaceClass() {
        return AuditConfiguration.class;
    }

    public EntityType getEntityType() {
        return EntityType.AUDIT_CONFIG;
    }

    @Override
    public String getTableName() {
        return "";
    }

    @Nullable
    public AuditConfiguration findByUniqueName(String name) throws FindException {
        if(AuditConfiguration.ENTITY_NAME.equals(name))
            return getEntity();
        return null;
    }

    @Override
    public void delete(Goid goid) throws DeleteException, FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(AuditConfiguration entity) throws UpdateException {
        Option<UpdateException> result =  new TransactionTemplate(transactionManager).execute(new TransactionCallback<Option<UpdateException>>(){
            @Override
            public Option<UpdateException> doInTransaction(TransactionStatus transactionStatus) {
                try {
                    updateClusterProperty(ServerConfigParams.PARAM_AUDIT_SINK_POLICY_GUID, entity.getSinkPolicyGoid() == null ? null : getPolicyById(entity.getSinkPolicyGoid()).getGuid());
                    updateClusterProperty(ServerConfigParams.PARAM_AUDIT_LOOKUP_POLICY_GUID, entity.getLookupPolicyGoid() == null ? null : getPolicyById(entity.getLookupPolicyGoid()).getGuid());
                    updateClusterProperty(ServerConfigParams.PARAM_AUDIT_SINK_ALWAYS_FALLBACK, Boolean.valueOf(entity.isAlwaysSaveInternal()).toString());
                    updateClusterProperty(ServerConfigParams.PARAM_AUDIT_ARCHIVER_FTP_DESTINATION, entity.getFtpClientConfig() == null ? null : FtpUtils.serialize(entity.getFtpClientConfig()));
                    return Option.none();
                } catch (ObjectModelException | IOException ome) {
                    return Option.some(new UpdateException("Unable to update entity.", ome));
                }
            }
        });
        if (result.isSome()){
            throw result.some();
        }
    }

    private Policy getPolicyById(@Nullable Goid id) throws FindException{
        Policy policy = policyManager.findByPrimaryKey(id);
        if(policy == null)
        {
            throw new FindException("Policy not found id: " + id);
        }
        return policy;

    }

    private Policy getPolicyByGUID(@Nullable String guid) throws FindException{
        Policy policy = policyManager.findByGuid(guid);
        if(policy == null)
        {
            throw new FindException("Policy not found guid: " + guid);
        }
        return policy;

    }

    private void updateClusterProperty(@NotNull String key, String value) throws ObjectModelException {
        ClusterProperty prop = clusterPropertyManager.findByUniqueName(key);
        if(value == null){
            if(prop != null) {
                clusterPropertyManager.delete(prop);
            }
            return;
        }
        if (prop == null) {
            prop = new ClusterProperty(key, value);
            Goid goid = clusterPropertyManager.save(prop);
            if (!goid.equals(prop.getGoid())) prop.setGoid(goid);
        }
        prop.setValue(value);
        clusterPropertyManager.update(prop);
    }

    @Nullable
    public AuditConfiguration findByHeader(EntityHeader header) throws FindException {
        return findByPrimaryKey(header.getGoid());
    }

    @Override
    public List<AuditConfiguration> findPagedMatching(int offset, int count, @Nullable String sortProperty, @Nullable Boolean ascending, @Nullable Map<String, List<Object>> matchProperties) throws FindException {
        return CollectionUtils.list(getEntity());
    }

    private AuditConfiguration getEntity() throws FindException{
        try {
            AuditConfiguration auditConfiguration = new AuditConfiguration();
            String saveInternal = clusterPropertyManager.getProperty(ServerConfigParams.PARAM_AUDIT_SINK_ALWAYS_FALLBACK);
            if(saveInternal!=null) {
                auditConfiguration.setAlwaysSaveInternal(Boolean.parseBoolean(saveInternal));
            }
            String sinkPolicyGuid = clusterPropertyManager.getProperty(ServerConfigParams.PARAM_AUDIT_SINK_POLICY_GUID);
            if(sinkPolicyGuid != null){
                auditConfiguration.setSinkPolicyGoid(getPolicyByGUID(sinkPolicyGuid).getGoid());
            }
            String lookupPolicyGuid = clusterPropertyManager.getProperty(ServerConfigParams.PARAM_AUDIT_LOOKUP_POLICY_GUID);
            if(lookupPolicyGuid != null){
                auditConfiguration.setLookupPolicyGoid(getPolicyByGUID(lookupPolicyGuid).getGoid());
            }
            auditConfiguration.setFtpClientConfig(FtpUtils.deserialize(clusterPropertyManager.getProperty(ServerConfigParams.PARAM_AUDIT_ARCHIVER_FTP_DESTINATION)));
            return auditConfiguration;
        } catch (FindException | IOException | ClassNotFoundException e) {
            throw new FindException(e.getMessage(),e);
        }
    }
}
