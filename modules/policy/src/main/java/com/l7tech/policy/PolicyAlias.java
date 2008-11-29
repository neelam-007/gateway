package com.l7tech.policy;

import com.l7tech.objectmodel.Alias;
import com.l7tech.objectmodel.folder.Folder;

/**
 * @author darmstrong
 */
public class PolicyAlias extends Alias<Policy> {
    @Deprecated // For Serialization and persistence only
    protected PolicyAlias() { }

    public PolicyAlias(Policy policy, Folder folder) {
        super(policy, folder);
    }
}
