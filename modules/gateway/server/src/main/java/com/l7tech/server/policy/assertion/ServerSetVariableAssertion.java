package com.l7tech.server.policy.assertion;

import com.l7tech.common.io.NullOutputStream;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.CommonMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.policy.variable.ExpandVariablesTemplate;
import com.l7tech.util.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

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

    @Inject
    @Named( "stashManagerFactory" )
    private StashManagerFactory stashManagerFactory;

    private final ExpandVariablesTemplate compiledTemplate;
    private final String singleVariableName;

    public ServerSetVariableAssertion(SetVariableAssertion assertion) throws PolicyAssertionException {
        super(assertion);
        varsUsed = assertion.getVariablesUsed();
        compiledTemplate = new ExpandVariablesTemplate(assertion.expression());
        singleVariableName = Syntax.getSingleVariableReferenced( assertion.expression() );
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

        final DataType dataType = assertion.getDataType();
        if (dataType == DataType.STRING) {
            final String strValue = compiledTemplate.process(vars, getAudit());
            context.setVariable(assertion.getVariableToSet(), strValue);
        } else if (dataType == DataType.MESSAGE) {
            final ContentTypeHeader contentType = ContentTypeHeader.parseValue(assertion.getContentType());
            try {
                final Message message = context.getOrCreateTargetMessage( new MessageTargetableSupport( assertion.getVariableToSet() ), false );

                boolean initialized = false;
                if ( singleVariableName != null ) {
                    // If the expression is just one context variable reference,
                    // we may be able to copy over a binary object without trying to convert it to a string first
                    initialized = maybeInitializeFromBinaryObject( message, contentType, assertion.expression(), vars );
                }

                if ( !initialized ) {
                    final String strValue = compiledTemplate.process( vars, getAudit() );
                    message.initialize( contentType, strValue.getBytes( contentType.getEncoding() ) );
                }

            } catch (NoSuchVariableException e) {
                logAndAudit( CommonMessages.TEMPLATE_UNSUPPORTED_VARIABLE, assertion.getVariableToSet() );
                return AssertionStatus.FALSIFIED;
            } catch ( NoSuchPartException e ) {
                logAndAudit( AssertionMessages.NO_SUCH_PART, singleVariableName, e.getWhatWasMissing() );
                return AssertionStatus.FALSIFIED;
            }
        } else if(dataType == DataType.DATE_TIME) {
            final String strValue = compiledTemplate.process(vars, getAudit());
            try {

                // Is the assertion configured to use <auto> e.g. gateway's current time?
                // <auto> is saved as an empty expression, which is treated differently to the expression resolving to
                // nothing, which is a fail case.
                final boolean isGatewayTime = assertion.expression().isEmpty();
                final Date date;
                if (isGatewayTime) {
                    date = new Date(timeSource.currentTimeMillis());
                } else {
                    final String dateFormat = assertion.getDateFormat();
                    // the <auto> format is saved as an empty value, which is treated differently to the format resolving
                    // to nothing, which is a fail case.
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
                                // fail case, date format resolved to nothing
                                logAndAudit(AssertionMessages.SET_VARIABLE_UNRECOGNISED_DATE_FORMAT, "''");
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
            final String strValue = compiledTemplate.process(vars, getAudit());
            try{
                context.setVariable(assertion.getVariableToSet(), Integer.parseInt(strValue));
            } catch (NumberFormatException e){
                logAndAudit(AssertionMessages.SET_VARIABLE_UNABLE_TO_PARSE_INTEGER, strValue);
                return AssertionStatus.FALSIFIED;
            }
        } else {
            throw new RuntimeException("Not implemented yet for data type " + dataType.getName() + " (variable name=\"" + assertion.getVariableToSet() + "\").");
        }

        return AssertionStatus.NONE;
    }

    private boolean maybeInitializeFromBinaryObject( Message message, ContentTypeHeader contentType, String expression, Map<String, Object> vars ) throws IOException, NoSuchPartException {
        boolean initialized = false;
        Object value = ExpandVariables.processSingleVariableAsObject( expression, vars, getAudit() );

        // Some binary values can be copied over if they are the only thing in the expression and if the output
        // format is of type Message.

        // For now we will support copying of Message and PartInfo

        if ( value instanceof Message ) {
            Message messValue = (Message) value;
            message.initialize( stashManagerFactory.createStashManager(), contentType, messValue.getMimeKnob().getEntireMessageBodyAsInputStream( false ) );
            initialized = true;
        } else if ( value instanceof PartInfo ) {
            PartInfo partInfo = (PartInfo) value;
            message.initialize( stashManagerFactory.createStashManager(), contentType, partInfo.getInputStream( false ) );
            initialized = true;
        }

        if ( initialized ) {
            // Force early copy, in case source message is closed
            IOUtils.copyStream( message.getMimeKnob().getEntireMessageBodyAsInputStream(), new NullOutputStream() );
        }

        return initialized;
    }
}
