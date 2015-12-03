package com.l7tech.external.assertions.remotecacheassertion;

import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.search.Dependency;
import com.l7tech.util.GoidUpgradeMapper;

/**
 * Created by IntelliJ IDEA.
 * User: ashah
 * Date: 10/05/12
 * Time: 4:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class RemoteCacheAssertion extends Assertion {
    private long remoteCacheId = -1;
    private Goid remoteCacheGoid;

    public void setRemoteCacheId(long remoteCacheId) {
        this.remoteCacheId = remoteCacheId;
    }

    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Dependency(type = Dependency.DependencyType.GENERIC, methodReturnType = Dependency.MethodReturnType.GOID)
    public Goid getRemoteCacheGoid() {
        if (remoteCacheGoid == null && remoteCacheId > -1) {
            remoteCacheGoid = GoidUpgradeMapper.mapOid(EntityType.GENERIC, remoteCacheId);
        }

        return remoteCacheGoid;
    }

    public void setRemoteCacheGoid(Goid remoteCacheGoid) {
        this.remoteCacheGoid = remoteCacheGoid;
    }

    public String getRemoteCacheName() {

        String result = "Invalid Remote Cache Configuration selected";
        RemoteCacheEntity entity = null;

        RemoteCacheEntityAdmin remoteCacheEntityAdmin =
                Registry.getDefault().getExtensionInterface(RemoteCacheEntityAdmin.class, null);

        try {
            entity = remoteCacheEntityAdmin.find(remoteCacheGoid);

            if (entity != null) {
                if (entity.isEnabled()) {
                    result = entity.getName();
                } else {
                    result = "disabled: " + entity.getName();
                }
            }
        } catch (FindException findException) {
        }

        return result;
    }
}
