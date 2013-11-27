/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */

package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.PolicyVariableUtils;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.xml.xpath.NoSuchXpathVariableException;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathUtil;
import com.l7tech.xml.xpath.XpathVariableFinder;

import java.util.*;
import java.util.logging.Logger;

/**
 * @author mike
 * @version 1.0
 */
public class XpathBasedAssertionValidator extends NamespaceMigratableAssertionValidator {
    private static final Logger logger = Logger.getLogger(XpathBasedAssertionValidator.class.getName());

    private final XpathBasedAssertion assertion;
    private String errString;
    private Throwable errThrowable;
    private final List<String> warnStrings;

    public XpathBasedAssertionValidator( final XpathBasedAssertion xpathBasedAssertion ) {
        super(xpathBasedAssertion);
        assertion = xpathBasedAssertion;
        warnStrings = new ArrayList<String>();
        String pattern = null;
        final XpathExpression xpathExpression = assertion.getXpathExpression();
        if (xpathExpression != null)
            pattern = xpathExpression.getExpression();

        if (pattern == null) {
            errString = "XPath pattern is missing";
            logger.info(errString);
        } else {
            try {
                final Map<String,String> namespaces = xpathBasedAssertion.namespaceMap();
                Set<String> varsUsed = null;

                if (assertion.permitsFullyDynamicExpression()) {
                    // Check for fully-dynamic xpath and skip XPath parsing if so
                    String dynamicXpathVar = Syntax.getSingleVariableReferencedNoSyntaxOrWhitespace(pattern);
                    if (dynamicXpathVar != null)
                        varsUsed = Collections.singleton(dynamicXpathVar);
                }

                if (varsUsed == null) {
                    // No fully-dynamic Xpath, so we need to validate the expression syntax.
                    varsUsed = new HashSet<>(Arrays.asList(assertion.getVariablesUsed()));
                    XpathUtil.validate(pattern, xpathExpression.getXpathVersion(), namespaces);
                }

                //test expression when it does not use any context variables
                if(varsUsed.isEmpty()){
                    XpathUtil.testXpathExpression(null, pattern, xpathExpression.getXpathVersion(), namespaces, buildXpathVariableFinder(varsUsed));
                }
                else {
                    final Set<String> predecessorVariables = PolicyVariableUtils.getVariablesSetByPredecessors(assertion).keySet();
                    for (String var : varsUsed) {
                        if(var == null){
                            errString = "This assertion uses context variable that needs to be specified.";
                            break;
                        }
                        if (var.startsWith(BuiltinVariables.PREFIX_SERVICE + "." + BuiltinVariables.SERVICE_SUFFIX_URL)) {
                            warnStrings.add("The context variable \"service.url\" has been deprecated.  Now HTTP routing " +
                                    "assertions use a new context variable \"httpRouting.url\" instead of \"service.url\".");
                        }

                        if (!BuiltinVariables.isPredefined(var) &&
                                Syntax.getMatchingName(var, predecessorVariables) == null) {
                            warnStrings.add(
                                    "This assertion refers to the variable '" +
                                            var +
                                            "', which is neither predefined " +
                                            "nor set in the policy so far." );
                        }

                        if (BuiltinVariables.isDeprecated(var)) {
                            warnStrings.add("Deprecated variable '" + var + "' should be replaced by '" + BuiltinVariables.getMetadata(var).getReplacedBy() + BuiltinVariables.getUnmatchedName(var) + "'.");
                        }
                    }
                }
            } catch (Exception e) {
                errString = "XPath pattern is not valid";
                errThrowable = e;
            }
        }
    }

    @Override
    public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
        super.validate(path, pvc, result);
        if (errString != null)
            result.addError(new PolicyValidatorResult.Error(assertion, errString, errThrowable));
        for ( String warnString : warnStrings )
            result.addWarning(new PolicyValidatorResult.Warning(assertion, warnString, null));
    }

    private XpathVariableFinder buildXpathVariableFinder( final Set<String> variables ) {
        return new XpathVariableFinder(){
            @Override
            public Object getVariableValue( final String namespaceUri,
                                            final String variableName ) throws NoSuchXpathVariableException {
                if ( namespaceUri != null && namespaceUri.length() > 0 )
                    throw new NoSuchXpathVariableException("Unsupported XPath variable namespace '"+namespaceUri+"'.");
                if ( !variables.contains(variableName) )
                    throw new NoSuchXpathVariableException("Unsupported XPath variable name '"+variableName+"'.");

                return "";
            }
        };
    }
}
