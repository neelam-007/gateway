package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.xml.soap.SoapVersion;
import com.l7tech.xml.xpath.XpathExpression;

import javax.xml.soap.SOAPConstants;

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
    public String[] getVariablesUsed() {
        return messageTargetableSupport.getVariablesUsed();
    }

    @Override
    public void updateSoapVersion(SoapVersion soapVersion) {
        if ( getXpathExpression() != null ) {
            String originalValue = XpathExpression.soapBodyXpathValue().getExpression();
            String originalValue12 = XpathExpression.soapBodyXpathValue().getExpression().replaceAll("soapenv:", "s12:");
            String currentValue = getXpathExpression().getExpression();

            if(originalValue.equals(currentValue) && soapVersion == SoapVersion.SOAP_1_2) {
                getXpathExpression().setExpression(currentValue.replaceAll("soapenv:", "s12:"));
                getXpathExpression().getNamespaces().put("s12", SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE);
            } else if(originalValue12.equals(currentValue) && soapVersion == SoapVersion.SOAP_1_1) {
                getXpathExpression().setExpression(currentValue.replaceAll("s12:", "soapenv:"));
                getXpathExpression().getNamespaces().remove("s12");
            }
        }
    }

    //- PROTECTED

    protected XmlSecurityAssertionBase( final TargetMessageType defaultTargetMessageType ) {
        this.messageTargetableSupport = new MessageTargetableSupport(defaultTargetMessageType);
    }

    //- PRIVATE

    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private MessageTargetableSupport messageTargetableSupport;

}
