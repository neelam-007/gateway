package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.policy.PolicyMetadata;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.util.Functions;

import java.util.*;

/**
 * Selector for trace.* suffixes.
 */
public class DebugTraceVariableContextSelector implements ExpandVariables.Selector<DebugTraceVariableContext> {
    private static final String SERVICE_OID = "service.oid";
    private static final String SERVICE_ID = "service.id";
    private static final String SERVICE_NAME = "service.name";
    private static final String POLICY_OID = "policy.oid";
    private static final String POLICY_ID = "policy.id";
    private static final String POLICY_GUID = "policy.guid";
    private static final String POLICY_NAME = "policy.name";
    private static final String POLICY_VERSION = "policy.version";
    private static final String ASSERTION_NUMBER = "assertion.number";
    private static final String ASSERTION_NUMBERSTR = "assertion.numberstr";
    private static final String ASSERTION_ORDINAL = "assertion.ordinal";
    private static final String ASSERTION_SHORTNAME = "assertion.shortname";
    private static final String ASSERTION_XML = "assertion.xml";
    private static final String STATUS = "status";
    private static final String STATUS_MESSAGE = "status.message";
    private static final String REQUEST = "request";
    private static final String RESPONSE = "response";
    private static final String FINAL = "final";
    private static final String OUT = "out";

    private static final String PREFIX_VAR = "var.";

    @Override
    public Selection select(String contextName, DebugTraceVariableContext context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        Functions.Unary<Selection,DebugTraceVariableContext> simpleGetter = simpleFields.get(name);
        if (simpleGetter != null)
            return simpleGetter.call(context);

        String remainingName;

        if (null != (remainingName = startsWith(name, REQUEST))) {
            return new Selection(context.getContext().getOriginalRequest(), remainingName);
        }

        if (null != (remainingName = startsWith(name, RESPONSE))) {
            return new Selection(context.getContext().getOriginalResponse(), remainingName);
        }

        if (null != (remainingName = startsWith(name, PREFIX_VAR))) {
            return new Selection(context.getContext().getOriginalContext(), remainingName);
        }

        return null;
    }

    private String startsWith(String name, String what) {
        if (!name.toLowerCase().startsWith(what))
            return null;

        final String remainingName = name.substring(what.length());
        return remainingName.startsWith(".") ? remainingName.substring(1) : remainingName;
    }

    @Override
    public Class<DebugTraceVariableContext> getContextObjectClass() {
        return DebugTraceVariableContext.class;
    }

    static Map<String, Functions.Unary<Selection, DebugTraceVariableContext>> simpleFields =
            new TreeMap<String, Functions.Unary<Selection, DebugTraceVariableContext>>(String.CASE_INSENSITIVE_ORDER);
    static {
        simpleFields.put(FINAL, new Functions.Unary<Selection, DebugTraceVariableContext>() {
            @Override
            public Selection call(DebugTraceVariableContext ctx) {
                return new Selection(ctx.getContext().isFinalInvocation());
            }
        });

        simpleFields.put(SERVICE_OID, new Functions.Unary<Selection, DebugTraceVariableContext>() {
            @Override
            public Selection call(DebugTraceVariableContext ctx) {
                final PublishedService service = ctx.getContext().getOriginalContext().getService();
                return new Selection(service == null ? null : service.getGoid());
            }
        });

        simpleFields.put(SERVICE_ID, new Functions.Unary<Selection, DebugTraceVariableContext>() {
            @Override
            public Selection call(DebugTraceVariableContext ctx) {
                final PublishedService service = ctx.getContext().getOriginalContext().getService();
                return new Selection(service == null ? null : service.getGoid());
            }
        });

        simpleFields.put(SERVICE_NAME, new Functions.Unary<Selection, DebugTraceVariableContext>() {
            @Override
            public Selection call(DebugTraceVariableContext ctx) {
                final PublishedService service = ctx.getContext().getOriginalContext().getService();
                return new Selection(service == null ? null : service.getName());
            }
        });

        simpleFields.put(POLICY_OID, new Functions.Unary<Selection, DebugTraceVariableContext>() {
            @Override
            public Selection call(DebugTraceVariableContext ctx) {
                PolicyMetadata meta = ctx.getContext().getOriginalContext().getCurrentPolicyMetadata();
                PolicyHeader head = meta == null ? null : meta.getPolicyHeader();
                return new Selection(head == null ? null : head.getGoid());
            }
        });

        simpleFields.put(POLICY_ID, new Functions.Unary<Selection, DebugTraceVariableContext>() {
            @Override
            public Selection call(DebugTraceVariableContext ctx) {
                PolicyMetadata meta = ctx.getContext().getOriginalContext().getCurrentPolicyMetadata();
                PolicyHeader head = meta == null ? null : meta.getPolicyHeader();
                return new Selection(head == null ? null : head.getGoid());
            }
        });

        simpleFields.put(POLICY_GUID, new Functions.Unary<Selection, DebugTraceVariableContext>() {
            @Override
            public Selection call(DebugTraceVariableContext ctx) {
                PolicyMetadata meta = ctx.getContext().getOriginalContext().getCurrentPolicyMetadata();
                PolicyHeader head = meta == null ? null : meta.getPolicyHeader();
                return new Selection(head == null ? null : head.getGuid());
            }
        });

        simpleFields.put(POLICY_NAME, new Functions.Unary<Selection, DebugTraceVariableContext>() {
            @Override
            public Selection call(DebugTraceVariableContext ctx) {
                PolicyMetadata meta = ctx.getContext().getOriginalContext().getCurrentPolicyMetadata();
                PolicyHeader head = meta == null ? null : meta.getPolicyHeader();
                return new Selection(head == null ? null : head.getName());
            }
        });

        simpleFields.put(POLICY_VERSION, new Functions.Unary<Selection, DebugTraceVariableContext>() {
            @Override
            public Selection call(DebugTraceVariableContext ctx) {
                PolicyMetadata meta = ctx.getContext().getOriginalContext().getCurrentPolicyMetadata();
                PolicyHeader head = meta == null ? null : meta.getPolicyHeader();
                return new Selection(head == null || head.getPolicyRevision()==0L ? null : head.getPolicyRevision());
            }
        });

        simpleFields.put(ASSERTION_NUMBER, new Functions.Unary<Selection, DebugTraceVariableContext>() {
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

        simpleFields.put(ASSERTION_NUMBERSTR, new Functions.Unary<Selection, DebugTraceVariableContext>() {
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

        simpleFields.put(ASSERTION_XML, new Functions.Unary<Selection, DebugTraceVariableContext>() {
            @Override
            public Selection call(DebugTraceVariableContext ctx) {
                ServerAssertion tsass = ctx.getContext().getTracedAssertion();
                Assertion ass = tsass == null ? null : tsass.getAssertion();
                return new Selection(ass == null ? null : WspWriter.getPolicyXml(ass));
            }
        });

        simpleFields.put(STATUS, new Functions.Unary<Selection, DebugTraceVariableContext>() {
            @Override
            public Selection call(DebugTraceVariableContext ctx) {
                final AssertionStatus status = ctx.getContext().getTracedStatus();
                return new Selection(status == null ? null : status.getNumeric());
            }
        });

        simpleFields.put(STATUS_MESSAGE, new Functions.Unary<Selection, DebugTraceVariableContext>() {
            @Override
            public Selection call(DebugTraceVariableContext ctx) {
                final AssertionStatus status = ctx.getContext().getTracedStatus();
                return new Selection(status == null ? null : status.getMessage());
            }
        });

        simpleFields.put(OUT, new Functions.Unary<Selection, DebugTraceVariableContext>() {
            @Override
            public Selection call(DebugTraceVariableContext ctx) {
                return new Selection(ctx.getContext().getTraceOut());
            }
        });
    }
}
