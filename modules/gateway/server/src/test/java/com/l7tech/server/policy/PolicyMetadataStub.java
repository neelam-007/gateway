package com.l7tech.server.policy;

import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.variable.VariableMetadata;

/**
 *
 */
public class PolicyMetadataStub implements PolicyMetadata {
    @Override
    public boolean isTarariWanted() {
        return false;
    }

    @Override
    public boolean isWssInPolicy() {
        return false;
    }

    @Override
    public boolean isMultipart() {
        return false;
    }

    @Override
    public PolicyHeader getPolicyHeader() {
        return null;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[0];
    }

    @Override
    public String[] getVariablesUsed() {
        return new String[0];
    }
}
