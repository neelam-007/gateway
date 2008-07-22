/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.message.Message;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class ServerSetVariableAssertion extends AbstractServerAssertion<SetVariableAssertion> {
    private static final Logger logger = Logger.getLogger(ServerSetVariableAssertion.class.getName());
    private final Auditor auditor;
    private final String[] varsUsed;

    public ServerSetVariableAssertion(SetVariableAssertion assertion, ApplicationContext spring) {
        super(assertion);
        auditor = new Auditor(this, spring, logger);
        varsUsed = Syntax.getReferencedNames(assertion.expression());
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Map vars = context.getVariableMap(varsUsed, auditor);
        final String strValue = ExpandVariables.process(assertion.expression(), vars, auditor);

        Object value = null;
        final DataType dataType = assertion.getDataType();
        if (dataType == DataType.STRING) {
            value = strValue;
        } else if (dataType == DataType.MESSAGE) {
            final ContentTypeHeader contentType = ContentTypeHeader.parseValue(assertion.getContentType());
            final Message message = new Message();
            message.initialize(contentType, strValue.getBytes(contentType.getEncoding()));
            value = message;
        } else {
            throw new RuntimeException("Not implemented yet for data type " + dataType.getName() + " (variable name=\"" + assertion.getVariableToSet() + "\").");
        }

        context.setVariable(assertion.getVariableToSet(), value);
        return AssertionStatus.NONE;
    }
}
