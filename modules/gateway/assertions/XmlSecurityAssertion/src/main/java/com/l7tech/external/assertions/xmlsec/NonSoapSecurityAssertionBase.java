package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.assertion.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Base class for non-SOAP immediate-mode XML dsig/xenc transformation assertions.
 */
public abstract class NonSoapSecurityAssertionBase extends XpathBasedAssertion implements MessageTargetable {
    public static String META_PROP_VERB = "NonSoapSecurityAssertion.verb";
    private MessageTargetableSupport messageTargetableSupport;

    protected NonSoapSecurityAssertionBase(TargetMessageType defaultTargetMessageType) {
        this.messageTargetableSupport = new MessageTargetableSupport(defaultTargetMessageType);
    }

    /**
     * @return a verb describing what this assertion will do to each matching element, ie "encrypt", "verify".
     */
    public String getVerb() {
        String verb = meta().get(META_PROP_VERB);
        return verb == null ? "process" : String.valueOf(verb);
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

    public abstract String getDisplayName();

    protected final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<NonSoapEncryptElementAssertion>(){
        @Override
        public String getAssertionName( final NonSoapEncryptElementAssertion assertion, final boolean decorate) {
            final String displayName = assertion.getDisplayName();
            if(!decorate) return displayName;

            StringBuilder name = new StringBuilder(displayName + " ");
            if (assertion.getXpathExpression() == null) {
                name.append("[XPath expression not set]");
            } else {
                name.append(assertion.getXpathExpression().getExpression());
            }
            return AssertionUtils.decorateName(assertion, name);
        }
    };

}
