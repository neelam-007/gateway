package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.HasFolder;

/**
 * Extension of EntityHeader with some service information.
 *
 * @author Steve Jones
 */
public class ServiceHeader extends EntityHeader implements HasFolder {

    //- PUBLIC

    public ServiceHeader(final PublishedService svc) {
        this( svc.isSoap(),
              svc.isDisabled(),
              svc.displayName(),
              svc.getOid(),
              svc.getName(),
              svc.getName(),
              svc.getFolderOid());
    }

    public ServiceHeader(final boolean isSoap,
                         final boolean isDisabled,
                         final String displayName,
                         final Long serviceOid,
                         final String name,
                         final String description,
                         final long folderOid) {
        super(serviceOid == null ? -1 : serviceOid, EntityType.SERVICE, name, description);
        this.isSoap = isSoap;
        this.isDisabled = isDisabled;
        this.displayName = displayName;
        this.folderOid = folderOid;        
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

    public Long getFolderOid() {
        return folderOid;
    }
    
    public String toString() {
        return getDisplayName();
    }

    //- PRIVATE

    private final boolean isSoap;
    private final boolean isDisabled;
    private final String displayName;
    private final long folderOid;
}
