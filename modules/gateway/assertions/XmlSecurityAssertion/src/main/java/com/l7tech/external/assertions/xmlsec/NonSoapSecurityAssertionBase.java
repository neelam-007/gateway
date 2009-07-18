package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.XpathBasedAssertion;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Base class for non-SOAP immediate-mode XML dsig/xenc transformation assertions.
 */
public class NonSoapSecurityAssertionBase extends XpathBasedAssertion implements MessageTargetable {
    private MessageTargetableSupport messageTargetableSupport;

    protected NonSoapSecurityAssertionBase(TargetMessageType defaultTargetMessageType) {
        this.messageTargetableSupport = new MessageTargetableSupport(defaultTargetMessageType);
    }

    @Override
    public String[] getVariablesUsed() {
        List<String> variables = new ArrayList<String>();
        variables.addAll( Arrays.asList( super.getVariablesUsed() ) );
        variables.addAll( Arrays.asList( messageTargetableSupport.getVariablesUsed() ) );
        return variables.toArray( new String[variables.size()] );
    }

    @Override
    public TargetMessageType getTarget() {
        return messageTargetableSupport.getTarget();
    }

    @Override
    public void setTarget(TargetMessageType target) {
        messageTargetableSupport.setTarget(target);
    }

    @Override
    public String getOtherTargetMessageVariable() {
        return messageTargetableSupport.getOtherTargetMessageVariable();
    }

    @Override
    public void setOtherTargetMessageVariable(String otherMessageVariable) {
        messageTargetableSupport.setOtherTargetMessageVariable(otherMessageVariable);
    }

    @Override
    public String getTargetName() {
        return messageTargetableSupport.getTargetName();
    }
}
