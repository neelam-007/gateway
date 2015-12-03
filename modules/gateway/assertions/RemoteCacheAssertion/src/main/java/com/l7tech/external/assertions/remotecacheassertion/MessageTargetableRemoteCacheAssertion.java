package com.l7tech.external.assertions.remotecacheassertion;

import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.VariableMetadata;

/**
 * MessageTargetable RemoteCacheAssertion
 */
public class MessageTargetableRemoteCacheAssertion extends RemoteCacheAssertion implements MessageTargetable {
    private MessageTargetableSupport targetSupport = new MessageTargetableSupport(TargetMessageType.REQUEST);

    @Override
    public TargetMessageType getTarget() {
        return targetSupport.getTarget();
    }

    @Override
    public void setTarget(TargetMessageType target) {
        targetSupport.setTarget(target);
    }

    @Override
    public String getOtherTargetMessageVariable() {
        return targetSupport.getOtherTargetMessageVariable();
    }

    @Override
    public void setOtherTargetMessageVariable(String otherTargetMessageVariable) {
        targetSupport.setOtherTargetMessageVariable(otherTargetMessageVariable);
    }


    @Override
    public String getTargetName() {
        return targetSupport.getTargetName();
    }

    @Override
    public boolean isTargetModifiedByGateway() {
        return targetSupport.isTargetModifiedByGateway();
    }

    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    @Override
    public Object clone() {
        MessageTargetableRemoteCacheAssertion mta = (MessageTargetableRemoteCacheAssertion) super.clone();
        mta.targetSupport = new MessageTargetableSupport( targetSupport );
        return mta;
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
