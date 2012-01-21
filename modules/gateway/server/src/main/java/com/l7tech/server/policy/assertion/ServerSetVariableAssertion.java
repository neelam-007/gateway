package com.l7tech.server.policy.assertion;

import com.l7tech.message.Message;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.gateway.common.audit.CommonMessages;
import com.l7tech.util.DateUtils;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class ServerSetVariableAssertion extends AbstractServerAssertion<SetVariableAssertion> {

    private static final Logger log = Logger.getLogger(ServerSetVariableAssertion.class.getName());

    private final String[] varsUsed;

    public ServerSetVariableAssertion(SetVariableAssertion assertion) {
        super(assertion);
        varsUsed = Syntax.getReferencedNames(assertion.expression());
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        AssertionStatus status = AssertionStatus.NONE;
        final Map<String,Object> vars = context.getVariableMap(varsUsed, getAudit());
        final DataType dataType = assertion.getDataType();
        if(dataType == DataType.STRING || dataType == DataType.MESSAGE){
            final String strValue = ExpandVariables.process(assertion.expression(), vars, getAudit());
            if (dataType == DataType.STRING) {
                context.setVariable(assertion.getVariableToSet(), strValue);
            } else if (dataType == DataType.MESSAGE) {
                final ContentTypeHeader contentType = ContentTypeHeader.parseValue(assertion.getContentType());
                try {
                    final Message message = context.getOrCreateTargetMessage( new MessageTargetableSupport(assertion.getVariableToSet()), false );
                    message.initialize(contentType, strValue.getBytes(contentType.getEncoding()));
                } catch (NoSuchVariableException e) {
                    logAndAudit( CommonMessages.TEMPLATE_UNSUPPORTED_VARIABLE, assertion.getVariableToSet() );
                    return AssertionStatus.FALSIFIED;
                }
            }
        } else if(dataType == DataType.DATE_TIME) {
            Object value = ExpandVariables.processSingleVariableAsObject(assertion.expression(), vars, getAudit());
            if(value instanceof Date) {
                context.setVariable(assertion.getVariableToSet(), value);
            }
            else if(value instanceof String){
                String strValue = (String)value;
                try {
                    context.setVariable(assertion.getVariableToSet(), DateUtils.parseDateTime(strValue, assertion.getFormat()));
                } catch (ParseException e) {
                    log.warning("Unable to convert value: " + strValue + " to Date/Time using format:" + assertion.getFormat());
                    status = AssertionStatus.FALSIFIED;
                }
            }
            else {
                log.warning("Conversion of " + assertion.expression() + " to Date/Time is not supported!");
                status = AssertionStatus.FALSIFIED;
            }
        } else {
            throw new RuntimeException("Not implemented yet for data type " + dataType.getName() + " (variable name=\"" + assertion.getVariableToSet() + "\").");
        }

        return status;
    }
}
