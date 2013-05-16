package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.HasSecurityZoneOid;
import com.l7tech.objectmodel.OrganizationHeader;
import org.jetbrains.annotations.Nullable;

/**
 * Extension of EntityHeader with some service information.
 *
 * @author Steve Jones
 */
public class ServiceHeader extends OrganizationHeader implements HasSecurityZoneOid {

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
              svc.getVersion(),
              svc.getRoutingUri(),
              svc.isTracingEnabled(),
              svc.getPolicy() == null ? false : svc.getPolicy().isDisabled(),
              svc.getSecurityZone() == null ? null : svc.getSecurityZone().getOid());
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
             serviceHeader.getVersion(),
             serviceHeader.getRoutingUri(),
             serviceHeader.isTracingEnabled(),
             serviceHeader.isPolicyDisabled(),
             serviceHeader.getSecurityZoneOid());
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
                         final int version,
                         final String routingUri,
                         final boolean tracingEnabled,
                         final boolean isPolicyDisabled,
                         final Long securityZoneOid) {
        super(serviceOid == null ? -1 : serviceOid, EntityType.SERVICE, name, description, version);
        this.isSoap = isSoap;
        this.isDisabled = isDisabled;
        this.displayName = displayName;
        this.folderOid = folderOid;
        this.aliasOid = aliasOid;
        this.policyRevision = policyRevision;
        this.routingUri = routingUri;
        this.tracingEnabled = tracingEnabled;
        this.isPolicyDisabled = isPolicyDisabled;
        this.securityZoneOid = securityZoneOid;
    }

    public boolean isSoap() {
        return isSoap;
    }

    public boolean isDisabled() {
        return isDisabled;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public String getRoutingUri() {
        return routingUri;
    }

    /**
     * Get the policy revision ordinal if available.
     *
     * @return The policy revision or 0 if not available.
     */
    public long getPolicyRevision() {
        return policyRevision;
    }

    public boolean isTracingEnabled() {
        return tracingEnabled;
    }

    @Nullable
    @Override
    public Long getSecurityZoneOid() {
        return securityZoneOid;
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
    private final String routingUri;
    private final boolean tracingEnabled;
    private final Long securityZoneOid;
}
