package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.AliasableHeader;
import com.l7tech.objectmodel.folder.HasFolder;

/**
 * Extension of EntityHeader with some service information.
 *
 * @author Steve Jones
 */
//todo [Donal] create a common super class implementing HasFolder, AliasableHeader shared by services and policies
public class ServiceHeader extends EntityHeader implements HasFolder, AliasableHeader {

    //- PUBLIC

    public ServiceHeader(final PublishedService svc) {
        this( svc.isSoap(),
              svc.isDisabled(),
              svc.displayName(),
              svc.getOid(),
              svc.getName(),
              svc.getName(),
              svc.getFolderOid(),
              svc.isAlias());
    }

    public ServiceHeader(final ServiceHeader serviceHeader){
        this(serviceHeader.isSoap(),
             serviceHeader.isDisabled(),
             serviceHeader.getDisplayName(),
             serviceHeader.getOid(),
             serviceHeader.getName(),
             serviceHeader.getDescription(),
             serviceHeader.getFolderOid(),
             serviceHeader.isAlias());
    }
    
    public ServiceHeader(final boolean isSoap,
                         final boolean isDisabled,
                         final String displayName,
                         final Long serviceOid,
                         final String name,
                         final String description,
                         final long folderOid,
                         final boolean isAlias) {
        super(serviceOid == null ? -1 : serviceOid, EntityType.SERVICE, name, description);
        this.isSoap = isSoap;
        this.isDisabled = isDisabled;
        this.displayName = displayName;
        this.folderOid = folderOid;
        this.isAlias = isAlias;
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

    public void setFolderOid(long folderOid) {
        this.folderOid = folderOid; 
    }

    public String toString() {
        return getDisplayName();
    }

    public boolean isAlias() {
        return isAlias;
    }

    public void setIsAlias(boolean isAlias) {
        this.isAlias = isAlias;
    }

    //- PRIVATE

    private final boolean isSoap;
    private final boolean isDisabled;
    private final String displayName;
    private long folderOid;
    private boolean isAlias;
}
