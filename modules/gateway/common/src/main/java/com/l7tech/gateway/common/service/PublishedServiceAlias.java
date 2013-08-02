package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.Alias;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.search.Dependency;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import static com.l7tech.objectmodel.migration.MigrationMappingSelection.NONE;

/**
 * @author darmstrong
 */
@XmlRootElement
public class PublishedServiceAlias extends Alias<PublishedService> {
    @Deprecated // For Serialization and persistence only
    protected PublishedServiceAlias() { }

    public PublishedServiceAlias( final PublishedService pService,
                                  final Folder folder) {
        super(pService, folder, pService == null ? null : pService.getSecurityZone() == null || !pService.getSecurityZone().permitsEntityType(EntityType.SERVICE_ALIAS) ? null : pService.getSecurityZone());
    }

    public PublishedServiceAlias( final ServiceHeader pService,
                                  final Folder folder,
                                  @Nullable final SecurityZone securityZone) {
        super(pService.getGoid(), folder, securityZone);
    }

    @XmlTransient
    @Override
    public EntityType getEntityType() {
        return EntityType.SERVICE;
    }

    @Override
    @Dependency(type = Dependency.DependencyType.SERVICE, methodReturnType = Dependency.MethodReturnType.GOID)
    @Migration(mapName = NONE, mapValue = NONE, resolver = PropertyResolver.Type.SERVICE_ALIAS)
    public Goid getEntityGoid() {
        return entityGoid;
    }

    // needed here for JAXB serialization
    @Override
    @Deprecated
    public void setEntityGoid(Goid entityGoid) {
        super.setEntityGoid(entityGoid);
    }
}
