package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.VariableUseSupport.VariablesSetSupport;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.variable.VariableMetadata;

/**
 * Base class for XML Security Assertions (Confidentiality and Integrity). Shares the concept
 * of the XmlSecurityRecipientContext.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Jan 17, 2005<br/>
 */
@RequiresSOAP(wss=true)
public abstract class XmlSecurityAssertionBase extends XpathBasedAssertion implements MessageTargetable, UsesVariables, SecurityHeaderAddressable {

    //- PUBLIC

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
    public void setOtherTargetMessageVariable(String otherTargetMessageVariable) {
        messageTargetableSupport.setOtherTargetMessageVariable(otherTargetMessageVariable);
    }

    @Override
    public String getTargetName() {
        return messageTargetableSupport.getTargetName();
    }

    public void setTargetMessage( final MessageTargetableSupport messageTargetableSupport ) {
        if ( messageTargetableSupport != null ) {
            this.messageTargetableSupport.setTargetMessage( messageTargetableSupport );
        } else {
            this.messageTargetableSupport.setTargetMessage( new MessageTargetableSupport(TargetMessageType.REQUEST) );
        }
    }

    @Override
    public XmlSecurityRecipientContext getRecipientContext() {
        return recipientContext;
    }

    @Override
    public void setRecipientContext( final XmlSecurityRecipientContext recipientContext ) {
        this.recipientContext = recipientContext != null ?
                recipientContext :
                XmlSecurityRecipientContext.getLocalRecipient();
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
    public XmlSecurityAssertionBase clone() {
        XmlSecurityAssertionBase xmlSecurityAssertionBase = (XmlSecurityAssertionBase) super.clone();
        xmlSecurityAssertionBase.messageTargetableSupport = new MessageTargetableSupport( messageTargetableSupport );
        return xmlSecurityAssertionBase;
    }

    //- PROTECTED

    protected XmlSecurityAssertionBase( final TargetMessageType defaultTargetMessageType ) {
        this.messageTargetableSupport = new MessageTargetableSupport(defaultTargetMessageType);
    }

    protected XmlSecurityAssertionBase( final TargetMessageType defaultTargetMessageType, boolean targetModifiedByGateway ) {
        this.messageTargetableSupport = new MessageTargetableSupport(defaultTargetMessageType, targetModifiedByGateway);
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().with( messageTargetableSupport.getMessageTargetVariablesUsed() );
    }

    protected VariablesSet doGetVariablesSet() {
        return new VariablesSet().with( messageTargetableSupport.getMessageTargetVariablesSet() );
    }

    protected final class VariablesSet extends VariablesSetSupport<VariablesSet> {
        private VariablesSet() {
        }

        @Override
        protected VariablesSet get() {
            return this;
        }
    }

    //- PRIVATE

    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private MessageTargetableSupport messageTargetableSupport;

}
