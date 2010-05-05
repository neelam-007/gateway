package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.PolicyVariableUtils;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathUtil;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Validator that will detect attempts to use an XPath expression that selects a single XPath variable, where
 * the XPath expression is expected to select one or more element nodes, but where the variable in question
 * is known not to be of Element type.
 */
public class ElementSelectingXpathValidator implements AssertionValidator {
    private static final Pattern varPattern = Pattern.compile("^\\s*\\$[A-Za-z0-9_\\-][A-Za-z0-9_\\-\\.]+\\s*$");
    private final Assertion assertion;
    private final String expectedElementVariable;

    public ElementSelectingXpathValidator(final XpathBasedAssertion assertion) {
        this.assertion = assertion;
        this.expectedElementVariable = getSingleTargetVariableName(assertion);
    }

    public static String getSingleTargetVariableName(XpathBasedAssertion xpathBasedAssertion) {
        String elementVar = null;
        final XpathExpression xpe = xpathBasedAssertion.getXpathExpression();
        if (xpe != null) {
            String expression = xpe.getExpression();
            if (expression != null) {
                List<String> varlist = XpathUtil.getUnprefixedVariablesUsedInXpath(expression);
                if (varlist.size() == 1 && looksLikeAttemptToSelectSingleElementVariable(expression)) {
                    elementVar = varlist.get(0);
                }
            }
        }
        return elementVar;
    }

    private static boolean looksLikeAttemptToSelectSingleElementVariable(String expression) {
        // Check if the expression is just a single XPath variable, ie "$requestXpath.element"
        return varPattern.matcher(expression).matches();
    }

    @Override
    public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {
        if (expectedElementVariable != null) {
            VariableMetadata elementMeta = null;
            Assertion[] assertionPath = path.getPath();
            for (Assertion a : assertionPath) {
                if (!a.isEnabled()) continue;
                if (a instanceof SetsVariables) {
                    SetsVariables sv = (SetsVariables) a;
                    VariableMetadata[] set = PolicyVariableUtils.getVariablesSetNoThrow(sv);
                    for (VariableMetadata vm : set) {
                        if (expectedElementVariable.equalsIgnoreCase(vm.getName()))
                            elementMeta = vm;
                    }
                }
            }

            if (elementMeta != null && !DataType.ELEMENT.equals(elementMeta.getType())) {
                result.addWarning(new PolicyValidatorResult.Warning(assertion, path, "Use of non-Element variable \"" + expectedElementVariable + "\" as XPath target element may not work as expected", null));
            }
        }
    }
}
