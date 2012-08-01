package com.l7tech.server.policy.assertion;

import com.l7tech.util.*;
import com.l7tech.gateway.common.audit.AssertionMessages;
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

import javax.inject.Inject;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author alex
 */
public class ServerSetVariableAssertion extends AbstractServerAssertion<SetVariableAssertion> {
    private final String[] varsUsed;
    @Inject
    private DateTimeConfigUtils dateParser;
    @Inject
    private TimeSource timeSource;

    @Inject
    private Config config;

    public ServerSetVariableAssertion(SetVariableAssertion assertion) throws PolicyAssertionException {
        super(assertion);
        varsUsed = assertion.getVariablesUsed();
        final String dateFormat = assertion.getDateFormat();
        if (dateFormat != null && !Syntax.isAnyVariableReferenced(dateFormat) && !DateTimeConfigUtils.isTimestampFormat(dateFormat)) {
            try {
                //validate the format is valid
                new SimpleDateFormat(dateFormat);
            } catch (IllegalArgumentException e) {
                throw new PolicyAssertionException(assertion, "Invalid date format: " + dateFormat);
            }
        }
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Map<String,Object> vars = context.getVariableMap(varsUsed, getAudit());
        final String strValue = ExpandVariables.process(assertion.expression(), vars, getAudit());

        final DataType dataType = assertion.getDataType();
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
        } else if(dataType == DataType.DATE_TIME) {
            try {
                final Date date;
                if (strValue.trim().isEmpty()) {
                    date = new Date(timeSource.currentTimeMillis());
                } else {
                    final String dateFormat = assertion.getDateFormat();
                    if (dateFormat != null && !dateFormat.trim().isEmpty()) {
                        //assertion configured with a format
                        if (dateFormat.equalsIgnoreCase(DateTimeConfigUtils.TIMESTAMP)) {
                            date = DateTimeConfigUtils.parseTimestamp(strValue);
                        } else if (dateFormat.equalsIgnoreCase(DateTimeConfigUtils.MILLISECOND_TIMESTAMP)){
                            date = DateTimeConfigUtils.parseMilliTimestamp(strValue);
                        } else if (dateFormat.equalsIgnoreCase(DateTimeConfigUtils.SECOND_TIMESTAMP)) {
                            date = DateTimeConfigUtils.parseSecondTimestamp(strValue);
                        } else {
                            final String dateFormatEvaled = ExpandVariables.process(dateFormat, vars, getAudit());
                            if (dateFormatEvaled.trim().isEmpty()) {
                                logAndAudit(AssertionMessages.SET_VARIABLE_UNRECOGNISED_DATE_FORMAT, dateFormatEvaled);
                                return AssertionStatus.FALSIFIED;
                            }
                            final SimpleDateFormat simpleDateFormat;
                            try {
                                simpleDateFormat = new SimpleDateFormat(dateFormatEvaled);
                            } catch (IllegalArgumentException e) {
                                throw new DateTimeConfigUtils.InvalidDateFormatException(ExceptionUtils.getMessage(e));
                            }
                            simpleDateFormat.setLenient(config.getBooleanProperty("com.l7tech.util.lenientDateFormat", false));
                            //time zone is overridden if the parsed string contains timezone information
                            simpleDateFormat.setTimeZone(DateUtils.getZuluTimeZone());
                            date = simpleDateFormat.parse(strValue);
                        }
                    } else {
                        date = dateParser.parseDateFromString(strValue);
                    }
                }

                final String offsetExpression = assertion.getDateOffsetExpression();
                if (offsetExpression != null && !offsetExpression.trim().isEmpty()) {
                    final String offsetEvaled = ExpandVariables.process(offsetExpression, vars, getAudit());
                    final Integer addAmount;
                    try {
                        addAmount = Integer.valueOf(offsetEvaled);
                    } catch (NumberFormatException e) {
                        logAndAudit(AssertionMessages.SET_VARIABLE_INVALID_DATE_OFFSET, offsetEvaled);
                        return AssertionStatus.FALSIFIED;
                    }
                    final int offsetField = assertion.getDateOffsetField();

                    final Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);
                    calendar.add(offsetField, addAmount);
                    context.setVariable(assertion.getVariableToSet(), calendar.getTime());
                } else {
                    context.setVariable(assertion.getVariableToSet(), date);
                }
            } catch (ParseException e) {
                logAndAudit(AssertionMessages.SET_VARIABLE_UNABLE_TO_PARSE_DATE, strValue);
                return AssertionStatus.FALSIFIED;
            } catch (DateTimeConfigUtils.UnknownDateFormatException e) {
                logAndAudit(AssertionMessages.SET_VARIABLE_UNRECOGNISED_DATE_FORMAT, e.getMessage());
                return AssertionStatus.FALSIFIED;
            } catch (DateTimeConfigUtils.InvalidDateFormatException e) {
                // this is a configuration error / runtime error if format came from a context variable
                logAndAudit(AssertionMessages.SET_VARIABLE_INVALID_DATE_PATTERN, e.getMessage());
                return AssertionStatus.FALSIFIED;
            }
        } else if (dataType == DataType.INTEGER){
            context.setVariable(assertion.getVariableToSet(), Integer.parseInt(strValue));
        } else {
            throw new RuntimeException("Not implemented yet for data type " + dataType.getName() + " (variable name=\"" + assertion.getVariableToSet() + "\").");
        }

        return AssertionStatus.NONE;
    }
}
