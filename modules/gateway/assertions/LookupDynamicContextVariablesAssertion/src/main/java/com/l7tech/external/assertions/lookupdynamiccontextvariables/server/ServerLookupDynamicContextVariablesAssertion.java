package com.l7tech.external.assertions.lookupdynamiccontextvariables.server;

import com.l7tech.external.assertions.lookupdynamiccontextvariables.LookupDynamicContextVariablesAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.w3c.dom.Element;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * Server side implementation of the LookupDynamicContextVariablesAssertion.
 *
 * @see com.l7tech.external.assertions.lookupdynamiccontextvariables.LookupDynamicContextVariablesAssertion
 */
public class ServerLookupDynamicContextVariablesAssertion extends AbstractServerAssertion<LookupDynamicContextVariablesAssertion> {

    public ServerLookupDynamicContextVariablesAssertion( final LookupDynamicContextVariablesAssertion assertion ) {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) {
        final DataType targetType = assertion.getTargetDataType() == null ? DataType.UNKNOWN : assertion.getTargetDataType();
        boolean unsupported = true;
        //make sure the target data type is one of the supported types
        for(DataType dt : LookupDynamicContextVariablesAssertion.SUPPORTED_TYPES){
            if(targetType == dt){
                unsupported = false;
                break;
            }
        }
        if(unsupported){
            logAndAudit(AssertionMessages.LOOKUP_DYNAMIC_VARIABLE_UNSUPPORTED_TYPE, targetType.getName());
            return AssertionStatus.FAILED;
        }
        final String sourceVariable = assertion.getSourceVariable();
        if(sourceVariable == null || sourceVariable.trim().isEmpty()){
            logAndAudit(AssertionMessages.LOOKUP_DYNAMIC_VARIABLE_MISSING_SOURCE);
            return AssertionStatus.FAILED;
        }
        final String targetVariable = assertion.getTargetOutputVariable();
        if(targetVariable == null || targetVariable.trim().isEmpty()){
            logAndAudit(AssertionMessages.LOOKUP_DYNAMIC_VARIABLE_MISSING_TARGET);
            return AssertionStatus.FAILED;
        }
        try{
            //lookup the variable name
            final Map<String, Object> lookup = context.getVariableMap(Syntax.getReferencedNames(sourceVariable), getAudit());
            final String process = ExpandVariables.process(sourceVariable, lookup, getAudit());

            //retrieve the variable
            final Map<String, Object> actual = context.getVariableMap(new String[]{process}, getAudit());
            final Object o = ExpandVariables.processSingleVariableAsObject(Syntax.getVariableExpression(process), actual, getAudit());
            if(o != null){
                //check if the retrieved value is one of the supported class based on the valueClass of the data type object.
                final String sourceName = o.getClass().getName();
                if((targetType == DataType.MESSAGE && !(o instanceof Message))
                        || (targetType == DataType.CERTIFICATE && !(o instanceof X509Certificate))
                        || (targetType == DataType.ELEMENT && !(o instanceof Element))){
                    logAndAudit(AssertionMessages.LOOKUP_DYNAMIC_VARIABLE_TYPE_MISMATCH, targetType.getName(), sourceName);
                    return AssertionStatus.FAILED;
                }
                if(targetType == DataType.DATE_TIME && (!(o instanceof Date) || !(o instanceof Calendar) || !(o instanceof Long))){
                    logAndAudit(AssertionMessages.LOOKUP_DYNAMIC_VARIABLE_TYPE_MISMATCH, targetType.getName(), sourceName);
                    return AssertionStatus.FAILED;
                }
                if(targetType == DataType.STRING && !(o instanceof String || o instanceof BigInteger || o instanceof BigDecimal || o instanceof Number || o instanceof Boolean)){
                    logAndAudit(AssertionMessages.LOOKUP_DYNAMIC_VARIABLE_TYPE_MISMATCH, targetType.getName(), sourceName);
                    return AssertionStatus.FAILED;
                }
            }
            context.setVariable(targetVariable, o);
        }
        catch(VariableNameSyntaxException e){
            logAndAudit(AssertionMessages.LOOKUP_DYNAMIC_VARIABLE_INVALID_SYNTAX , e.getMessage());
            return AssertionStatus.FAILED;
        }
        return AssertionStatus.NONE;
    }

}
