package com.l7tech.objectmodel;

import com.l7tech.objectmodel.folder.HasFolderGoid;

/**
 * EntityHeader for Aliases.
 *
 * <p>Extension of entity header with additional data for the target of the
 * alias and for the containing Folder.</p>
 */
public class AliasHeader<ET extends GoidEntity> extends ZoneableEntityHeader implements HasFolderGoid {

    //- PUBLIC

    public AliasHeader( Alias<ET> alias ) {
        super( alias.getGoid(),
               EntityTypeRegistry.getEntityType(alias.getClass()),
               alias instanceof NamedEntity ? ((NamedEntity)alias).getName() : null,
               null,
               alias.getVersion());
        aliasedEntityType = alias.getEntityType();
        aliasedEntityId = alias.getEntityGoid();
        folderGoid = alias.getFolder() != null ? alias.getFolder().getGoid() : null;
        securityZoneGoid = alias.getSecurityZone() == null ? null : alias.getSecurityZone().getGoid();
    }

    public EntityType getAliasedEntityType() {
        return aliasedEntityType;
    }

    public void setAliasedEntityType(EntityType aliasedEntityType) {
        this.aliasedEntityType = aliasedEntityType;
    }

    public Goid getAliasedEntityId() {
        return aliasedEntityId;
    }

    public void setAliasedEntityId(Goid aliasedEntityId) {
        this.aliasedEntityId = aliasedEntityId;
    }

    public Goid getFolderGoid() {
        return folderGoid;
    }

    public void setFolderGoid(Goid folderGoid) {
        this.folderGoid = folderGoid;
    }

    //- PRIVATE

    private EntityType aliasedEntityType;
    private Goid aliasedEntityId;
    private Goid folderGoid;
}
