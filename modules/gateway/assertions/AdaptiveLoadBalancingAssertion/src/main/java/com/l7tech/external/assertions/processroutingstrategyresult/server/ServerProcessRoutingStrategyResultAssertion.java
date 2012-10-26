package com.l7tech.external.assertions.processroutingstrategyresult.server;

import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.Feedback;
import com.l7tech.common.io.failover.Service;
import com.l7tech.external.assertions.processroutingstrategyresult.ProcessRoutingStrategyResultAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Server side implementation of the ProcessRoutingStrategyResultAssertion.
 *
 * @see com.l7tech.external.assertions.processroutingstrategyresult.ProcessRoutingStrategyResultAssertion
 */
public class ServerProcessRoutingStrategyResultAssertion extends AbstractServerAssertion<ProcessRoutingStrategyResultAssertion> {

    private static final int FAILED = -1;
    private static final int UNDEFINED_REASON_CODE = -5;
    private static final Functions.Unary<Integer, String> parseInt = new Functions.Unary<Integer, String>(){
        @Override
        public Integer call(String s) {
            return Integer.valueOf(s);
        }
    };
    private static final Functions.Unary<Long, String> parseLong = new Functions.Unary<Long, String>() {
        @Override
        public Long call(String s) {
            return Long.valueOf(s);
        }
    };

    private final String[] variablesUsed;

    public ServerProcessRoutingStrategyResultAssertion(final ProcessRoutingStrategyResultAssertion assertion) throws PolicyAssertionException {
        super(assertion);
        variablesUsed = assertion.getVariablesUsed();

    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Map<String, Object> varMap = context.getVariableMap(variablesUsed, getAudit());
        //check if strategy exists and the correct type
        if(!varMap.containsKey(assertion.getStrategy()) || !(varMap.get(assertion.getStrategy()) instanceof FailoverStrategy)){
            logAndAudit(AssertionMessages.ADAPTIVE_LOAD_BALANCING_VAR_NOT_FOUND, assertion.getStrategy());
            return AssertionStatus.FALSIFIED;
        }
        FailoverStrategy<Service> fs = (FailoverStrategy) varMap.get(assertion.getStrategy());
        //check if feedback route exists and the correct type
        if(!varMap.containsKey(assertion.getFeedbackRoute()) || !(varMap.get(assertion.getFeedbackRoute()) instanceof Service)) {
            logAndAudit(AssertionMessages.ADAPTIVE_LOAD_BALANCING_VAR_NOT_FOUND, assertion.getFeedbackRoute());
            return AssertionStatus.FALSIFIED;
        }
        Service route = (Service) varMap.get(assertion.getFeedbackRoute());
        //check if feedback array is created and it has a correct type
        if(!varMap.containsKey(assertion.getFeedback()) || !(varMap.get(assertion.getFeedback()).getClass().isAssignableFrom(new ArrayList<Feedback>().getClass()))){
            logAndAudit(AssertionMessages.ADAPTIVE_LOAD_BALANCING_VAR_NOT_FOUND, assertion.getFeedback());
            return AssertionStatus.FALSIFIED;
        }
        List<Feedback> feedbacks = (List<Feedback>) varMap.get(assertion.getFeedback());

        Long latency = objectToNumber(Long.class,  varMap.get(assertion.getFeedbackLatency()), 0L, parseLong, new Functions.UnaryVoid<Throwable>() {
            @Override
            public void call(Throwable t) {
                logAndAudit(AssertionMessages.ADAPTIVE_LOAD_BALANCING_WRONG_VAR_TYPE, new String[]{assertion.getFeedbackLatency()}, ExceptionUtils.getDebugException(t));
            }
        });
        Integer status = objectToNumber(Integer.class, varMap.get(assertion.getFeedbackStatus()), FAILED, parseInt, new Functions.UnaryVoid<Throwable>() {
            @Override
            public void call(Throwable t) {
                logAndAudit(AssertionMessages.ADAPTIVE_LOAD_BALANCING_WRONG_VAR_TYPE, new String[]{assertion.getFeedbackStatus()}, ExceptionUtils.getDebugException(t));
            }
        });
        Integer reasonCode = objectToNumber(Integer.class, varMap.get(assertion.getReasonCode()), UNDEFINED_REASON_CODE, parseInt, new Functions.UnaryVoid<Throwable>() {
            @Override
            public void call(Throwable t) {
                logAndAudit(AssertionMessages.ADAPTIVE_LOAD_BALANCING_WRONG_VAR_TYPE, new String[]{assertion.getReasonCode()}, ExceptionUtils.getDebugException(t));
            }
        });

        Feedback fb = new Feedback(latency, reasonCode, route.getName(), status);
        feedbacks.add(fb);
        if (!status.equals(FAILED)) {
            fs.reportSuccess(route);
        } else {
            fs.reportFailure(route);
            logAndAudit(AssertionMessages.ADAPTIVE_LOAD_BALANCING_FAIL, fb.toString());
        }

        fs.reportContent(context, fb);

        return AssertionStatus.NONE;
    }

    @Nullable
    private static <T extends Number> T objectToNumber(@NotNull final Class<T> type, @Nullable final Object val, @Nullable final T defaultVal, @NotNull final Functions.Unary<T, String> parseFunc, @Nullable Functions.UnaryVoid<Throwable> loggingFunc) {
        if(val != null) {
            if(val.getClass().isAssignableFrom(type)) {
               return (T)val;
            }
            else if(val instanceof String) {
                try{
                    return parseFunc.call((String)val);
                } catch (Throwable e) {
                    //logging can be suppressed
                    if(loggingFunc != null)
                        loggingFunc.call(e);
                }
            }
        }
        return defaultVal;
    }


}
