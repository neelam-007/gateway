package com.l7tech.policy;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.AliasableHeader;
import com.l7tech.objectmodel.folder.HasFolder;

/**
 * Extension of EntityHeader with some policy information.
 *
 * @author Steve Jones
 */
public class PolicyHeader extends EntityHeader implements HasFolder, AliasableHeader {

    //- PUBLIC
    public PolicyHeader(final Policy policy) {
        this(policy.getOid(), policy.isSoap(), policy.getName(), policy.getInternalTag(), policy.getGuid(), policy.getFolderOid());
    }

    public PolicyHeader(final long oid,
                        final boolean isSoap,
                        final String name,
                        final String description,
                        final String policyGuid,
                        final Long folderOid)
    {
        super(oid, EntityType.POLICY, name, description);

        this.guid = policyGuid;
        this.isSoap = isSoap;
        this.folderOid = folderOid;
    }

    public boolean isSoap() {
        return isSoap;
    }

    public Long getFolderOid() {
        return folderOid;
    }

    public void setFolderOid(long folderOid) {
        this.folderOid = folderOid;
    }

    public String getGuid() {
        return guid;
    }

    @Override
    public String toString() {
        return getName();
    }

    public boolean isAlias() {
        return isAlias;
    }

    public void setIsAlias(boolean isAlias) {
        this.isAlias = isAlias;
    }
    
    //- PRIVATE

    private final boolean isSoap;
    private final String guid;
    private Long folderOid;
    private boolean isAlias = false;
}
