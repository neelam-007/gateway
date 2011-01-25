package com.l7tech.external.assertions.splitjoin.server;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.TextUtils;
import com.l7tech.external.assertions.splitjoin.JoinAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server side implementation of the JoinAssertion.
 *
 * @see com.l7tech.external.assertions.splitjoin.JoinAssertion
 */
public class ServerJoinAssertion extends AbstractServerAssertion<JoinAssertion> {
    private static final Logger logger = Logger.getLogger(ServerJoinAssertion.class.getName());

    private final Auditor auditor;
    private final String substring;
    private final String inputVariable;
    private final String outputVariable;

    private static final String DEFAULT_MV_DELIMITER = ",";

    public ServerJoinAssertion(JoinAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        //noinspection ThisEscapedInObjectConstruction
        this.auditor = context == null ? new LogOnlyAuditor(logger) : new Auditor(this, context, logger);
        this.substring = assertion.getJoinSubstring();
        inputVariable = assertion.getInputVariable();
        outputVariable = assertion.getOutputVariable();
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            final Object[] values = getInputVariables(context);

            final List<CharSequence> toJoin = new ArrayList();
            for(Object value : values){
                if (value instanceof Collection) {
                    Collection collection = (Collection)value;
                    List<String> got = new ArrayList<String>();
                    for (Object o : collection)
                        got.add(o == null ? "" : o.toString());
                    toJoin.addAll(got);
                } else if (value instanceof Object[]) {
                    Object[] objects = (Object[])value;
                    List<String> got = new ArrayList<String>();
                    for (Object o : objects)
                        got.add(o == null ? "" : o.toString());
                    toJoin.addAll(got);
                } else if(value instanceof String){
                    toJoin.add((String)value);
                } else {
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Input variable " + inputVariable + " is not a multi valued context variable." }, null);
                    return AssertionStatus.FAILED;
                }
            }

            final String output = TextUtils.join(substring, toJoin).toString();

            context.setVariable(outputVariable, output);
            return AssertionStatus.NONE;
        } catch (NoSuchVariableException e) {
            auditor.logAndAudit(AssertionMessages.NO_SUCH_VARIABLE_WARNING, e.getVariable());
            return AssertionStatus.SERVER_ERROR;
        }
    }

    private Object[] getInputVariables(PolicyEnforcementContext context) throws NoSuchVariableException{
        List<Object> objs = new ArrayList<Object>();      

        String refString = inputVariable.replaceAll("\\[.\\]","");
        List <String> refs = TextUtils.getTokensFromString(refString,DEFAULT_MV_DELIMITER);
        Map<String,Object> map = context.getVariableMap(refs.toArray(new String[refs.size()]),auditor);

        List<String> tokens  = TextUtils.getTokensFromString(inputVariable,DEFAULT_MV_DELIMITER);
        Pattern regexPattern = Pattern.compile("\\[.\\],?");

        for(String token : tokens){

            Syntax.getMatchingName(token, map.keySet());
            Matcher matcher = regexPattern.matcher(token);
            if(matcher.find()){
               objs.add(ExpandVariables.process("${"+token.trim()+"}",map,auditor));
            }
            else{
                Object retrieve = map.get(token.trim());
                objs.add(retrieve);
            }
        }
        return objs.toArray(new Object[objs.size()]);
    }
}