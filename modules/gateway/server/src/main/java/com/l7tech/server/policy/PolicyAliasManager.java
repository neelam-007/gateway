package com.l7tech.server.policy;

import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.server.service.AliasManager;

/**
 * @author darmstrong
 */
public interface PolicyAliasManager extends AliasManager<PolicyAlias, Policy, PolicyHeader> { }