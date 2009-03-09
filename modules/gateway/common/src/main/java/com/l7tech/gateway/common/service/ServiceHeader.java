package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.OrganizationHeader;

/**
 * Extension of EntityHeader with some service information.
 *
 * @author Steve Jones
 */
public class ServiceHeader extends OrganizationHeader {

    //- PUBLIC

    public ServiceHeader(final PublishedService svc) {
        this( svc.isSoap(),
              svc.isDisabled(),
              svc.displayName(),
              svc.getOid(),
              svc.getName(),
              svc.getName(),
              svc.getFolder() == null ? null : svc.getFolder().getOid(),
              null,
              svc.getPolicy() == null ? 0L : svc.getPolicy().getVersionOrdinal(),
              svc.getVersion() );
    }

    public ServiceHeader(final ServiceHeader serviceHeader){
        this(serviceHeader.isSoap(),
             serviceHeader.isDisabled(),
             serviceHeader.getDisplayName(),
             serviceHeader.getOid(),
             serviceHeader.getName(),
             serviceHeader.getDescription(),
             serviceHeader.getFolderOid(),
             serviceHeader.getAliasOid(),
             serviceHeader.getPolicyRevision(),
             serviceHeader.getVersion());
    }
    
    public ServiceHeader(final boolean isSoap,
                         final boolean isDisabled,
                         final String displayName,
                         final Long serviceOid,
                         final String name,
                         final String description,
                         final Long folderOid,
                         final Long aliasOid,
                         final long policyRevision,
                         final int version) {
        super(serviceOid == null ? -1 : serviceOid, EntityType.SERVICE, name, description, version);
        this.isSoap = isSoap;
        this.isDisabled = isDisabled;
        this.displayName = displayName;
        this.folderOid = folderOid;
        this.aliasOid = aliasOid;
        this.policyRevision = policyRevision;
    }

    public boolean isSoap() {
        return isSoap;
    }

    public boolean isDisabled() {
        return isDisabled;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the policy revision ordinal if available.
     *
     * @return The policy revision or 0 if not available.
     */
    public long getPolicyRevision() {
        return policyRevision;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    //- PRIVATE

    private final boolean isSoap;
    private final boolean isDisabled;
    private final String displayName;
    private final long policyRevision;
}
