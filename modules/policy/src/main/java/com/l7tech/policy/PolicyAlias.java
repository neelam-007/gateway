package com.l7tech.policy;

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
public class PolicyAlias extends Alias<Policy> {
    @Deprecated // For Serialization and persistence only
    protected PolicyAlias() { }

    public PolicyAlias( final Policy policy, final Folder folder ) {
        super(policy, folder, policy.getSecurityZone() == null || !policy.getSecurityZone().permitsEntityType(EntityType.POLICY_ALIAS)? null : policy.getSecurityZone());
    }

    public PolicyAlias( final PolicyHeader policy, final Folder folder, @Nullable final SecurityZone securityZone) {
        super(policy.getGoid(), folder, securityZone);
    }

    @XmlTransient
    @Override
    public EntityType getEntityType() {
        return EntityType.POLICY;
    }

    @Override
    @Migration(mapName = NONE, mapValue = NONE, resolver = PropertyResolver.Type.POLICY_ALIAS)
    @Dependency(type = Dependency.DependencyType.POLICY, methodReturnType = Dependency.MethodReturnType.GOID)
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
