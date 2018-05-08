package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditConfiguration;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.MockClusterPropertyManager;
import com.l7tech.server.policy.PolicyManagerStub;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

/**
 * @author luiwy01, 2017-09-18
 */
public class AuditConfigurationManagerStub implements AuditConfigurationManager {

  private AuditConfigurationManagerImpl auditConfigurationManager;

  public AuditConfigurationManagerStub(PolicyManagerStub policyManagerStub, MockClusterPropertyManager mockClusterPropertyManager) {
    auditConfigurationManager  = new AuditConfigurationManagerImpl(policyManagerStub,mockClusterPropertyManager, new PlatformTransactionManager(){
        @Override
        public TransactionStatus getTransaction(TransactionDefinition transactionDefinition) throws TransactionException {
          return null;
        }

        @Override
        public void commit(TransactionStatus transactionStatus) throws TransactionException { }

        @Override
        public void rollback(TransactionStatus transactionStatus) throws TransactionException {}
    });
  }

  @Override
  public AuditConfiguration findByPrimaryKey(Goid goid) throws FindException {
   return auditConfigurationManager.findByPrimaryKey(goid);
  }

  @Override
  public Collection<EntityHeader> findAllHeaders() throws FindException {
    return auditConfigurationManager.findAllHeaders();
  }

  @Override
  public Collection<EntityHeader> findAllHeaders(int offset, int windowSize) throws FindException {
    return auditConfigurationManager.findAllHeaders(offset,windowSize);
  }

  @Override
  public Collection<AuditConfiguration> findAll() throws FindException {
    return auditConfigurationManager.findAll();
  }

  @Override
  public Class<? extends Entity> getImpClass() {
    return auditConfigurationManager.getImpClass();
  }

  @Override
  public Goid save(AuditConfiguration entity) throws SaveException {
    return auditConfigurationManager.save(entity);
  }

  @Override
  public void save(Goid id, AuditConfiguration entity) throws SaveException {
    auditConfigurationManager.save(id,entity);
  }

  @Override
  public Integer getVersion(Goid goid) throws FindException {
    return auditConfigurationManager.getVersion(goid);
  }

  @Override
  public Map<Goid, Integer> findVersionMap() throws FindException {
    return auditConfigurationManager.findVersionMap();
  }

  @Override
  public void delete(AuditConfiguration entity) throws DeleteException {

  }

  @Nullable
  @Override
  public AuditConfiguration getCachedEntity(Goid goid, int maxAge) throws FindException {
    return auditConfigurationManager.getCachedEntity(goid, maxAge);
  }

  @Override
  public Class<? extends Entity> getInterfaceClass() {
    return auditConfigurationManager.getInterfaceClass();
  }

  @Override
  public EntityType getEntityType() {
    return auditConfigurationManager.getEntityType();
  }

  @Override
  public String getTableName() {
    return auditConfigurationManager.getTableName();
  }

  @Nullable
  @Override
  public AuditConfiguration findByUniqueName(String name) throws FindException {
    return auditConfigurationManager.findByUniqueName(name);
  }

  @Override
  public void delete(Goid goid) throws DeleteException, FindException {
    auditConfigurationManager.delete(goid);
  }

  @Override
  public void update(AuditConfiguration entity) throws UpdateException {
    auditConfigurationManager.update(entity);

  }

  @Nullable
  @Override
  public AuditConfiguration findByHeader(EntityHeader header) throws FindException {
    return auditConfigurationManager.findByHeader(header);
  }

  @Override
  public List<AuditConfiguration> findPagedMatching(int offset, int count, @Nullable String sortProperty, @Nullable Boolean ascending, @Nullable Map<String, List<Object>> matchProperties) throws FindException {
    return auditConfigurationManager.findPagedMatching(offset,count,sortProperty,ascending,matchProperties);
  }
}
