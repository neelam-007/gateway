package com.l7tech.policy.assertion.credential;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.AssertionNodeNameFactory;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathUtil;
import com.l7tech.util.Functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Gathers credentials based on XPath expressions pointing to the login and password.
 *
 * The superclass's {@link #xpathExpression} is used to hold the XPath pointing to
 * the login.
 *
 * @author alex
 */
@ProcessesRequest
public class XpathCredentialSource extends XpathBasedAssertion {
    private boolean removeLoginElement;
    private boolean removePasswordElement;
    private XpathExpression passwordExpression;

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
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        List<String> vars = new ArrayList<String>(Arrays.asList(super.getVariablesUsed()));
        if (passwordExpression != null) {
            String passexpr = passwordExpression.getExpression();
            if (passexpr != null)
                vars.addAll(XpathUtil.getUnprefixedVariablesUsedInXpath(passexpr));
        }
        return vars.toArray(new String[vars.size()]);
    }

    final static String baseName = "Require XPath Credentials";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<XpathCredentialSource>(){
        @Override
        public String getAssertionName( final XpathCredentialSource assertion, final boolean decorate) {
            if(!decorate) return baseName;
            return baseName + ": login = '" + assertion.getXpathExpression().getExpression() +
                           "', password = '" + assertion.getPasswordExpression().getExpression() + "'";
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

        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "XPath Credentials Properties");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.EditXpathCredentialSourceAction");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/Edit16.gif");


        return meta;
    }
}
