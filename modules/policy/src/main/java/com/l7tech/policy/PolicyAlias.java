package com.l7tech.policy;

import com.l7tech.objectmodel.Alias;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.migration.MigrationMappingSelection.NONE;
import com.l7tech.objectmodel.folder.Folder;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * @author darmstrong
 */
@XmlRootElement
public class PolicyAlias extends Alias<Policy> {
    @Deprecated // For Serialization and persistence only
    protected PolicyAlias() { }

    public PolicyAlias( final Policy policy, final Folder folder ) {
        super(policy, folder, policy.getSecurityZone());
    }

    public PolicyAlias( final PolicyHeader policy, final Folder folder ) {
        // TODO set security zone
        super(policy.getOid(), folder, null);
    }

    @XmlTransient
    @Override
    public EntityType getEntityType() {
        return EntityType.POLICY;
    }

    @Override
    @Migration(mapName = NONE, mapValue = NONE, resolver = PropertyResolver.Type.POLICY_ALIAS)
    public long getEntityOid() {
        return entityOid;
    }

    // needed here for JAXB serialization
    @Override
    @Deprecated
    public void setEntityOid(long entityOid) {
        super.setEntityOid(entityOid);
    }
}
