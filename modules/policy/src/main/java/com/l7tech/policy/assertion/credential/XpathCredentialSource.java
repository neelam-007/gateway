package com.l7tech.policy.assertion.credential;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.xml.soap.SoapVersion;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathUtil;

import java.util.*;

/**
 * Gathers credentials based on XPath expressions pointing to the login and password.
 *
 * The superclass's {@link #xpathExpression} is used to hold the XPath pointing to
 * the login.
 *
 * @author alex
 */
public class XpathCredentialSource extends XpathBasedAssertion implements MessageTargetable{
    private boolean removeLoginElement;
    private boolean removePasswordElement;
    private XpathExpression passwordExpression;
    private final MessageTargetableSupport messageTargetableSupport;

    public XpathCredentialSource() {
        this( TargetMessageType.REQUEST );
    }

    protected XpathCredentialSource(TargetMessageType defaultTargetMessageType) {
        this.messageTargetableSupport = new MessageTargetableSupport(defaultTargetMessageType, false);
    }

    public boolean isCredentialSource() {
        return true;
    }

    public XpathExpression getPasswordExpression() {
        return passwordExpression;
    }

    public void setPasswordExpression(XpathExpression passwordExpression) {
        this.passwordExpression = passwordExpression;
    }

    public boolean isRemoveLoginElement() {
        return removeLoginElement;
    }

    public void setRemoveLoginElement(boolean removeLoginElement) {
        this.removeLoginElement = removeLoginElement;
    }

    public boolean isRemovePasswordElement() {
        return removePasswordElement;
    }

    public void setRemovePasswordElement(boolean removePasswordElement) {
        this.removePasswordElement = removePasswordElement;
    }

    @Override
    public boolean isTargetModifiedByGateway() {
        return removeLoginElement || removePasswordElement;
    }

    @Override
    public final VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[0];
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

    @Override
    public XpathExpression createDefaultXpathExpression(boolean soapPolicy, SoapVersion soapVersion) {
        return createDefaultLoginOrPasswordExpression(soapPolicy, soapVersion, false);
    }

    public XpathExpression createDefaultLoginOrPasswordExpression(boolean soapPolicy, SoapVersion soapVersion, boolean forPassword) {
        XpathExpression x = super.createDefaultXpathExpression(soapPolicy, soapVersion);
        final String prefix = soapPolicy ? x.getExpression() : "";
        final String suffix = forPassword ? "//Password" : "//Username";
        x.setExpression(prefix + suffix);
        return x;
    }

    @Override
    public void migrateNamespaces(Map<String, String> nsUriSourceToDest) {
        super.migrateNamespaces(nsUriSourceToDest);
        if (passwordExpression != null)
            passwordExpression.migrateNamespaces(nsUriSourceToDest);
    }

    @Override
    public Set<String> findNamespaceUrisUsed() {
        HashSet<String> ret = new HashSet<String>(super.findNamespaceUrisUsed());
        if (passwordExpression != null)
            ret.addAll(passwordExpression.findNamespaceUrisUsed());
        return ret;
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        final VariablesUsed used = super.doGetVariablesUsed();
        if ( passwordExpression != null ) {
            final String passexpr = passwordExpression.getExpression();
            if ( passexpr != null ) {
                used.addVariables( XpathUtil.getUnprefixedVariablesUsedInXpath(passexpr, passwordExpression.getXpathVersion()) );
            }
        }
        used.add(messageTargetableSupport.getMessageTargetVariablesUsed());
        return used;
    }


    final static String baseName = "Require XPath Credentials";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<XpathCredentialSource>(){
        @Override
        public String getAssertionName( final XpathCredentialSource assertion, final boolean decorate) {
            if(!decorate) return baseName;

            String name  = baseName + ": login = '" + assertion.getXpathExpression().getExpression() +
                           "', password = '" + assertion.getPasswordExpression().getExpression() + "'";

            return AssertionUtils.decorateName(assertion, name);
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"accessControl"});

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Gateway retrieves login and password from current request using XPath expressions");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlWithCert16.gif");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "com.l7tech.console.tree.policy.advice.AddXpathCredentialSourceAdvice");
        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.NamespaceMigratableAssertionValidator");

        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "XPath Credentials Properties");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.EditXpathCredentialSourceAction");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/Edit16.gif");


        return meta;
    }
}
