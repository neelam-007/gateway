package com.l7tech.gateway.common.audit;

import com.l7tech.gateway.common.transport.ftp.FtpClientConfig;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.search.Dependency;
import org.jetbrains.annotations.Nullable;

/**
 * @author luiwy01, 2017-09-13
 */
public class AuditConfiguration  implements PersistentEntity, NamedEntity {

  public static Goid ENTITY_ID = new Goid(4,1);
  public static String ENTITY_NAME = "AuditConfig";

  private Goid sinkPolicyGoid;
  private Goid lookupPolicyGoid;
  private boolean alwaysSaveInternal = true; // default from cluster property
  private FtpClientConfig ftpClientConfig = null;


  @Override
  public String getId() {
    return ENTITY_ID.toString();
  }

  @Override
  public String getName() {
    return ENTITY_NAME;
  }

  @Dependency(type = Dependency.DependencyType.POLICY, methodReturnType = Dependency.MethodReturnType.GOID)
  @Nullable
  public Goid getSinkPolicyGoid() {
    return sinkPolicyGoid;
  }

  public void setSinkPolicyGoid(@Nullable Goid sinkPolicyGoid) {
      this.sinkPolicyGoid = sinkPolicyGoid;

  }

  @Dependency(type = Dependency.DependencyType.POLICY, methodReturnType = Dependency.MethodReturnType.GOID)
  @Nullable
  public Goid getLookupPolicyGoid() {
    return lookupPolicyGoid;
  }

  public void setLookupPolicyGoid(@Nullable Goid lookupPolicyGoid) {
    this.lookupPolicyGoid = lookupPolicyGoid;
  }

  public boolean isAlwaysSaveInternal() {
    return alwaysSaveInternal;
  }

  public void setAlwaysSaveInternal(boolean alwaysSaveInternal) {
    this.alwaysSaveInternal = alwaysSaveInternal;
  }

  @Nullable
  public FtpClientConfig getFtpClientConfig() {
    return ftpClientConfig;
  }

  public void setFtpClientConfig(@Nullable FtpClientConfig ftpClientConfig) {
    this.ftpClientConfig = ftpClientConfig;
  }

  public static EntityHeader getDefaultEntityHeader(){
      return new EntityHeader(new Goid(4,1), EntityType.AUDIT_CONFIG,"","");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AuditConfiguration)) return false;
    if (!super.equals(o)) return false;

    AuditConfiguration that = (AuditConfiguration) o;

    if (sinkPolicyGoid != null ? !sinkPolicyGoid.equals(that.sinkPolicyGoid) : that.sinkPolicyGoid != null) return false;
    if (lookupPolicyGoid != null ? !lookupPolicyGoid.equals(that.lookupPolicyGoid) : that.lookupPolicyGoid != null) return false;
    if (alwaysSaveInternal != that.alwaysSaveInternal) return false;
    if (ftpClientConfig != null ? !ftpClientConfig.equals(that.ftpClientConfig) : that.ftpClientConfig != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();

    result = 31 * result + (sinkPolicyGoid != null ? sinkPolicyGoid.hashCode() : 0);
    result = 31 * result + (lookupPolicyGoid != null ? lookupPolicyGoid.hashCode() : 0);
    result = 31 * result + (alwaysSaveInternal ? 1 : 0);
    result = 31 * result + (ftpClientConfig != null ? ftpClientConfig.hashCode() : 0);
    return result;
  }

  public void copyFrom(AuditConfiguration other) {
    this.setSinkPolicyGoid(other.getSinkPolicyGoid());
    this.setLookupPolicyGoid(other.getLookupPolicyGoid());
    this.setAlwaysSaveInternal(other.isAlwaysSaveInternal());
    this.setFtpClientConfig(other.getFtpClientConfig());
  }

  @Override
  public Goid getGoid() {
    return ENTITY_ID;
  }

  @Override
  public void setGoid(Goid goid) {
    if(!ENTITY_ID.equals(goid))
      throw new UnsupportedOperationException();
  }

  @Override
  public boolean isUnsaved() {
    return false;
  }

  @Override
  public int getVersion() {
    return 0;
  }

  @Override
  public void setVersion(int version) {
  }
}
