package com.l7tech.objectmodel;

import com.l7tech.objectmodel.folder.HasFolderOid;

/**
 * EntityHeader for Aliases.
 *
 * <p>Extension of entity header with additional data for the target of the
 * alias and for the containing Folder.</p>
 */
public class AliasHeader<ET extends PersistentEntity> extends ZoneableEntityHeader implements HasFolderOid {

    //- PUBLIC

    public AliasHeader( Alias<ET> alias ) {
        super( alias.getOid(),
               EntityTypeRegistry.getEntityType(alias.getClass()),
               alias instanceof NamedEntity ? ((NamedEntity)alias).getName() : null,
               null,
               alias.getVersion());
        aliasedEntityType = alias.getEntityType();
        aliasedEntityId = alias.getEntityOid();
        folderOid = alias.getFolder() != null ? alias.getFolder().getOid() : null;
        securityZoneGoid = alias.getSecurityZone() == null ? null : alias.getSecurityZone().getGoid();
    }

    public EntityType getAliasedEntityType() {
        return aliasedEntityType;
    }

    public void setAliasedEntityType(EntityType aliasedEntityType) {
        this.aliasedEntityType = aliasedEntityType;
    }

    public Long getAliasedEntityId() {
        return aliasedEntityId;
    }

    public void setAliasedEntityId(Long aliasedEntityId) {
        this.aliasedEntityId = aliasedEntityId;
    }

    public Long getFolderOid() {
        return folderOid;
    }

    public void setFolderOid(Long folderOid) {
        this.folderOid = folderOid;
    }

    //- PRIVATE

    private EntityType aliasedEntityType;
    private Long aliasedEntityId;
    private Long folderOid;
}
