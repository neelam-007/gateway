package com.l7tech.server.identity.external;

import com.l7tech.identity.GroupManager;
import com.l7tech.identity.external.VirtualPolicyGroup;
import com.l7tech.identity.external.VirtualPolicyUser;

/**
 * Group manager for policy backed ID provider.
 */
public interface PolicyBackedGroupManager extends GroupManager<VirtualPolicyUser, VirtualPolicyGroup> {
}
