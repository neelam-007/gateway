package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.common.io.CertUtils;
import com.l7tech.external.assertions.xmlsec.IndexLookupByItemAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.w3c.dom.Element;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 */
public class ServerIndexLookupByItemAssertion extends AbstractServerAssertion<IndexLookupByItemAssertion> {
    private static final Logger logger = Logger.getLogger(ServerIndexLookupByItemAssertion.class.getName());

    private final Auditor auditor;
    private final String multivaluedVariableName;
    private final String valueToSearchForVariableName;
    private final String outputVariableName;
    private final String[] varsUsed;
    private final boolean allowMulti;

    public ServerIndexLookupByItemAssertion(IndexLookupByItemAssertion assertion, BeanFactory beanFactory, ApplicationEventPublisher eventPub) throws PolicyAssertionException {
        super(assertion);
        this.auditor = new Auditor(this, beanFactory, eventPub, logger);
        this.multivaluedVariableName = assertion.getMultivaluedVariableName();
        this.valueToSearchForVariableName = "${" + assertion.getValueToSearchForVariableName() + "}";
        this.outputVariableName = assertion.getOutputVariableName();
        this.allowMulti = assertion.isAllowMultipleMatches();
        if (empty(multivaluedVariableName) || empty(valueToSearchForVariableName) || empty(outputVariableName))
            throw new PolicyAssertionException(assertion, "At least one required property is not configured");
        this.varsUsed = assertion.getVariablesUsed();
    }

    private boolean empty(String s) {
        return s == null || s.length() < 1;
    }

    private static class Matcher {
        boolean matches(Object a, Object b) {
            return a == b || a.equals(b);
        }
    }

    private static final Matcher DEFAULT_MATCHER = new Matcher();

    private static final Matcher EXACT_MATCHER = new Matcher() {
        @Override
        boolean matches(Object a, Object b) {
            return a == b;
        }
    };

    private static final Matcher X509_MATCHER = new Matcher() {
        @Override
        boolean matches(Object cert, Object b) {
            return b instanceof X509Certificate && CertUtils.certsAreEqual((X509Certificate)cert, (X509Certificate)b);
        }
    };

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            Object multi = context.getVariable(multivaluedVariableName);
            Map<String, Object> variableMap = context.getVariableMap(varsUsed, auditor);
            Object valueToMatch = ExpandVariables.processSingleVariableAsObject(valueToSearchForVariableName, variableMap, auditor, true);
            if (valueToMatch == null) {
                auditor.logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, multivaluedVariableName);
                return AssertionStatus.SERVER_ERROR;
            }

            final Matcher matcher = getMatcherForValue(valueToMatch);

            int idx = 0;
            List<Integer> matchingIndexes = new ArrayList<Integer>();

            if (multi instanceof Object[]) {
                Object[] array = (Object[]) multi;
                for (Object obj : array) {
                    if (matcher.matches(obj, valueToMatch))
                        matchingIndexes.add(idx);
                    idx++;
                }

            } else if (multi instanceof Collection) {
                Collection collection = (Collection) multi;
                for (Object obj : collection) {
                    if (matcher.matches(obj, valueToMatch))
                        matchingIndexes.add(idx);
                    idx++;
                }

            } else {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Not a multi-valued context variable: " + multivaluedVariableName);
                return AssertionStatus.SERVER_ERROR;
            }

            if (matchingIndexes.isEmpty()) {
                return AssertionStatus.FALSIFIED;
            }

            if (!allowMulti && matchingIndexes.size() > 1) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, "More than one value was matched");
                return AssertionStatus.FAILED;
            }

            context.setVariable(outputVariableName, allowMulti ? matchingIndexes.toArray(new Integer[matchingIndexes.size()]) : matchingIndexes.get(0));
            return AssertionStatus.NONE;

        } catch (NoSuchVariableException e) {
            auditor.logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, e.getVariable());
            return AssertionStatus.SERVER_ERROR;
        }
    }

    private Matcher getMatcherForValue(Object valueToMatch) {
        final Matcher matcher;
        if (valueToMatch == null) {
            // We will disallow this for the time being, even though it may be meaningful, as it would seem to be error prone
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Value to match is null");
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, "Value to match is null");
        } else if (valueToMatch instanceof Element) {
            matcher = EXACT_MATCHER;
        } else if (valueToMatch instanceof X509Certificate) {
            matcher = X509_MATCHER;
        } else {
            matcher = DEFAULT_MATCHER;
        }
        return matcher;
    }
}
