package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.policy.PolicyMetadata;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.util.Functions;

import java.util.*;

/**
 * Selector for trace.* suffixes.
 */
public class DebugTraceVariableContextSelector implements ExpandVariables.Selector<DebugTraceVariableContext> {
    public static final String SERVICE_OID = "service.oid";
    public static final String POLICY_OID = "policy.oid";
    public static final String ASSERTION_PATH = "assertion.path";
    public static final String ASSERTION_PATHSTR = "assertion.pathstr";
    public static final String ASSERTION_ORDINAL = "assertion.ordinal";
    public static final String ASSERTION_SHORTNAME = "assertion.shortname";
    public static final String STATUS = "status";
    public static final String REQUEST = "request";
    public static final String RESPONSE = "response";

    public static final String PREFIX_VAR = "var.";

    @Override
    public Selection select(String contextName, DebugTraceVariableContext context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        Functions.Unary<Selection,DebugTraceVariableContext> simpleGetter = simpleFields.get(name);
        if (simpleGetter != null)
            return simpleGetter.call(context);

        if (name.startsWith(PREFIX_VAR)) {
            // Delegate to PolicyEnforcementContextGetter
            String remainingName = name.substring(PREFIX_VAR.length());
            return new Selection(context.getContext(), remainingName);
        }

        return null;
    }

    @Override
    public Class<DebugTraceVariableContext> getContextObjectClass() {
        return DebugTraceVariableContext.class;
    }

    static Map<String, Functions.Unary<Selection, DebugTraceVariableContext>> simpleFields =
            new TreeMap<String, Functions.Unary<Selection, DebugTraceVariableContext>>(String.CASE_INSENSITIVE_ORDER);
    static {
        simpleFields.put(SERVICE_OID, new Functions.Unary<Selection, DebugTraceVariableContext>() {
            @Override
            public Selection call(DebugTraceVariableContext ctx) {
                final PublishedService service = ctx.getContext().getOriginalContext().getService();
                return new Selection(service == null ? null : service.getOid());
            }
        });

        simpleFields.put(POLICY_OID, new Functions.Unary<Selection, DebugTraceVariableContext>() {
            @Override
            public Selection call(DebugTraceVariableContext ctx) {
                PolicyMetadata meta = ctx.getContext().getOriginalContext().getCurrentPolicyMetadata();
                PolicyHeader head = meta == null ? null : meta.getPolicyHeader();
                return new Selection(head == null ? null : head.getOid());
            }
        });

        simpleFields.put(ASSERTION_PATH, new Functions.Unary<Selection, DebugTraceVariableContext>() {
            @Override
            public Selection call(DebugTraceVariableContext ctx) {
                Collection<Integer> steps = ctx.getContext().getOriginalContext().getAssertionOrdinalPath();
                List<Integer> ret = new ArrayList<Integer>(steps);
                ServerAssertion tsass = ctx.getContext().getTracedAssertion();
                Assertion ass = tsass == null ? null : tsass.getAssertion();
                if (ass != null)
                    ret.add(ass.getOrdinal());
                return new Selection(ret.toArray(new Integer[ret.size()]));
            }
        });

        simpleFields.put(ASSERTION_PATHSTR, new Functions.Unary<Selection, DebugTraceVariableContext>() {
            @Override
            public Selection call(DebugTraceVariableContext ctx) {
                StringBuilder ret = new StringBuilder();

                Collection<Integer> steps = ctx.getContext().getOriginalContext().getAssertionOrdinalPath();
                boolean first = true;
                for (Integer step : steps) {
                    if (!first) ret.append('.');
                    ret.append(step);
                    first = false;
                }

                ServerAssertion tsass = ctx.getContext().getTracedAssertion();
                Assertion ass = tsass == null ? null : tsass.getAssertion();
                if (ass != null) {
                    if (!first) ret.append('.');
                    ret.append(ass.getOrdinal());
                }

                return new Selection(ret.toString());
            }
        });

        simpleFields.put(ASSERTION_ORDINAL, new Functions.Unary<Selection, DebugTraceVariableContext>() {
            @Override
            public Selection call(DebugTraceVariableContext ctx) {
                ServerAssertion tsass = ctx.getContext().getTracedAssertion();
                Assertion ass = tsass == null ? null : tsass.getAssertion();
                return new Selection(ass == null ? null : ass.getOrdinal());
            }
        });

        simpleFields.put(ASSERTION_SHORTNAME, new Functions.Unary<Selection, DebugTraceVariableContext>() {
            @Override
            public Selection call(DebugTraceVariableContext ctx) {
                ServerAssertion tsass = ctx.getContext().getTracedAssertion();
                Assertion ass = tsass == null ? null : tsass.getAssertion();
                return new Selection(ass == null ? null : ass.meta().get(AssertionMetadata.SHORT_NAME));
            }
        });

        simpleFields.put(STATUS, new Functions.Unary<Selection, DebugTraceVariableContext>() {
            @Override
            public Selection call(DebugTraceVariableContext ctx) {
                final AssertionStatus status = ctx.getContext().getTracedStatus();
                return new Selection(status == null ? null : status.getNumeric());
            }
        });
    }
}
