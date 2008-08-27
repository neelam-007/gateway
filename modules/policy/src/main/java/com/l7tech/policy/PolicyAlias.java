package com.l7tech.policy;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.objectmodel.EntityAlias;
import com.l7tech.objectmodel.AliasEntity;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 25, 2008
 * Time: 9:14:36 AM
 */
public class PolicyAlias<PolicyAlias> extends AliasEntity {

    @Deprecated // For Serialization and persistence only
    public PolicyAlias(){
    }

    public PolicyAlias(Policy policy, long folderOid) {
        super(policy, folderOid);
    }
}
