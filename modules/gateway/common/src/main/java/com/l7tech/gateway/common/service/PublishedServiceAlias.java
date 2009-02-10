package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.Alias;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.migration.MigrationMappingSelection.NONE;
import com.l7tech.objectmodel.folder.Folder;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author darmstrong
 */
@XmlRootElement
public class PublishedServiceAlias extends Alias<PublishedService> {
    @Deprecated // For Serialization and persistence only
    protected PublishedServiceAlias() { }

    public PublishedServiceAlias(PublishedService pService, Folder folder) {
        super(pService, folder);
    }

    @Override
    @Migration(mapName = NONE, mapValue = NONE, resolver = PropertyResolver.Type.SERVICE_ALIAS)
    public long getEntityOid() {
        return entityOid;
    }

    // needed here for JAXB serialization
    @Override
    public void setEntityOid(long entityOid) {
        super.setEntityOid(entityOid);
    }
}
