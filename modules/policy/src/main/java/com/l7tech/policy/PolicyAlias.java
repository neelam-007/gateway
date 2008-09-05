package com.l7tech.policy;

import com.l7tech.objectmodel.Alias;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 25, 2008
 * Time: 9:14:36 AM
 */
public class PolicyAlias<PolicyAlias> extends Alias {

    @Deprecated // For Serialization and persistence only
    public PolicyAlias(){
    }

    public PolicyAlias(Policy policy, long folderOid) {
        super(policy, folderOid);
    }
}
