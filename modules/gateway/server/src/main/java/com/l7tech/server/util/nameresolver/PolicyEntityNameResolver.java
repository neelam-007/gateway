package com.l7tech.server.util.nameresolver;

import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.objectmodel.folder.HasFolderId;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.policy.Policy;
import com.l7tech.server.util.nameresolver.EntityNameResolver;
import org.apache.commons.lang.StringUtils;

/**
 * Name resolver for Policy Entity
 */
public class PolicyEntityNameResolver extends EntityNameResolver {
    private PolicyAdmin policyAdmin;

    public PolicyEntityNameResolver(PolicyAdmin policyAdmin, FolderAdmin folderAdmin) {
        super(folderAdmin);
        this.policyAdmin = policyAdmin;
    }
    @Override
    protected boolean canResolveName(final EntityHeader entityHeader) {
        return (EntityType.POLICY.equals(entityHeader.getType()));
    }

    @Override
    protected boolean canResolveName(final Entity entity) {
        return entity instanceof Policy;
    }
    @Override
    public String resolve(final EntityHeader entityHeader, final boolean includePath) throws FindException {
        final Policy policy = policyAdmin.findPolicyByPrimaryKey(entityHeader.getGoid());
        validateFoundEntity(entityHeader, policy);
        String name = resolve(policy, includePath);
        String path = null;
        if (includePath && entityHeader instanceof HasFolderId) {
            path = getPath((HasFolderId) entityHeader);
        }
        return buildName(name, StringUtils.EMPTY, path, true);
    }

    @Override
    public String resolve(final Entity entity, final boolean includePath) throws FindException {
        NamedEntityImp named = (NamedEntityImp) entity;
        String name = named.getName();
        String path = null;
        if (includePath && entity instanceof HasFolder) {
            path = getPath((HasFolder) entity);
        }
        return buildName(name, StringUtils.EMPTY, path, true);
    }
}
