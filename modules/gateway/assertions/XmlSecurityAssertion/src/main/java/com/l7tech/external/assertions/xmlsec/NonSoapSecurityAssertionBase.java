package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.VariableUseSupport.VariablesSetSupport;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.xml.soap.SoapVersion;
import com.l7tech.xml.xpath.XpathExpression;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Base class for non-SOAP immediate-mode XML dsig/xenc transformation assertions.
 */
public abstract class NonSoapSecurityAssertionBase extends XpathBasedAssertion implements MessageTargetable {
    public static final String META_PROP_VERB = "NonSoapSecurityAssertion.verb";
    private final MessageTargetableSupport messageTargetableSupport;

    protected NonSoapSecurityAssertionBase(TargetMessageType defaultTargetMessageType, boolean targetModifiedByGateway) {
        this.messageTargetableSupport = new MessageTargetableSupport(defaultTargetMessageType, targetModifiedByGateway);
    }

    /**
     * @return a verb describing what this assertion will do to each matching element, ie "encrypt", "verify".
     */
    public String getVerb() {
        String verb = meta().get(META_PROP_VERB);
        return verb == null ? "process" : String.valueOf(verb);
    }

    @Override
    public boolean isTargetModifiedByGateway() {
        return messageTargetableSupport.isTargetModifiedByGateway();
    }

    @Override
    public final VariableMetadata[] getVariablesSet() {
        return doGetVariablesSet().asArray();
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

    public Map<String, String> getDefaultNamespaceMap() {
        final HashMap<String, String> m = new HashMap<String, String>();
        m.put("xenc", "http://www.w3.org/2001/04/xmlenc#");
        m.put("ds", "http://www.w3.org/2000/09/xmldsig#");
        return m;
    }

    @Override
    public XpathExpression createDefaultXpathExpression(boolean soapPolicy, @Nullable SoapVersion soapVersion) {
        return new XpathExpression(getDefaultXpathExpressionString(), getDefaultNamespaceMap());
    }

    public abstract String getDefaultXpathExpressionString();

    public abstract String getDisplayName();

    public String getPropertiesDialogTitle() {
        return String.valueOf(meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME));
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().with( messageTargetableSupport.getMessageTargetVariablesUsed() );
    }

    protected VariablesSet doGetVariablesSet() {
        return new VariablesSet( messageTargetableSupport.getMessageTargetVariablesSet().asArray() );
    }

    public static final class VariablesSet extends VariablesSetSupport<VariablesSet> {
        private VariablesSet( final VariableMetadata[] initialVariables ) {
            super( initialVariables );
        }

        @Override
        protected VariablesSet get() {
            return this;
        }
    }

    protected final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<NonSoapSecurityAssertionBase>(){
        @Override
        public String getAssertionName( final NonSoapSecurityAssertionBase assertion, final boolean decorate) {
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
