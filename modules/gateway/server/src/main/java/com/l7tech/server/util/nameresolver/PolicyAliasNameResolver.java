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
import com.l7tech.policy.PolicyAlias;
import com.l7tech.server.util.nameresolver.EntityNameResolver;
import org.apache.commons.lang.StringUtils;
/**
 * Name resolver for Policy Alias Entity
 */
public class PolicyAliasNameResolver extends EntityNameResolver {
    private static final String ALIAS = " alias";
    private PolicyAdmin policyAdmin;

    public PolicyAliasNameResolver(PolicyAdmin policyAdmin, FolderAdmin folderAdmin) {
        super(folderAdmin);
        this.policyAdmin = policyAdmin;
    }

    @Override
    protected boolean canResolveName(final EntityHeader entityHeader) {
        return (EntityType.POLICY_ALIAS.equals(entityHeader.getType()));
    }

    @Override
    protected boolean canResolveName(final Entity entity) {
        return entity instanceof PolicyAlias;
    }

    @Override
    public String resolve(final EntityHeader entityHeader, final boolean includePath) throws FindException {
        String name = null;
        final Policy owningPolicy = policyAdmin.findByAlias(entityHeader.getGoid());
        validateFoundEntity(entityHeader, owningPolicy);
        name = owningPolicy.getName() + ALIAS;
        String path = null;
        if (includePath && entityHeader instanceof HasFolderId) {
            path = getPath((HasFolderId) entityHeader);
        }
        return buildName(name, StringUtils.EMPTY, path, true);
    }

    @Override
    public String resolve(final Entity entity, final boolean includePath) throws FindException {
        Entity relatedEntity = null;
        String name = StringUtils.EMPTY;
        if (entity instanceof PolicyAlias) {
            final PolicyAlias alias = (PolicyAlias) entity;
            final Policy owningPolicy = policyAdmin.findPolicyByPrimaryKey(alias.getEntityGoid());
            validateFoundEntity(EntityType.POLICY, alias.getGoid(), owningPolicy);
            name = owningPolicy.getName() + ALIAS;
            relatedEntity = owningPolicy;
        } else if (entity instanceof NamedEntityImp) {
            final NamedEntityImp named = (NamedEntityImp) entity;
            name = named.getName();
        }
        String path = null;
        if (includePath && entity instanceof HasFolder) {
            path = getPath((HasFolder) entity);
        }
        String uniqueInfo = getUniqueInfo(entity);
        if (StringUtils.isBlank(uniqueInfo) && relatedEntity != null) {
            uniqueInfo = getUniqueInfo((Entity) relatedEntity);
        }

        return buildName(name, uniqueInfo, path, true);
    }
}
