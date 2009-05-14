package com.l7tech.policy.assertion.credential;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathUtil;

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
        List<String> vars = Arrays.asList(super.getVariablesUsed());
        if (passwordExpression != null) {
            String passexpr = passwordExpression.getExpression();
            if (passexpr != null)
                vars.addAll(XpathUtil.getUnprefixedVariablesUsedInXpath(passexpr));
        }
        return vars.toArray(new String[vars.size()]);
    }
}
