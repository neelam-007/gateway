package com.l7tech.server.policy.variable;

import com.l7tech.common.io.failover.Feedback;
import com.l7tech.policy.variable.Syntax;

public class FeedbackSelector implements ExpandVariables.Selector<Feedback> {

    private static final String LATENCY = "latency";
    private static final String REASON = "reason";
    private static final String ROUTE = "route";
    private static final String STATUS = "status";

    @Override
    public Selection select(String contextName, Feedback context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        String attr = name.toLowerCase();
        if (attr.equals(LATENCY)) {
            return new Selection(context.getLatency());
        } else if (attr.equals(REASON)) {
            return new Selection(context.getReason());
        } else if (attr.equals(ROUTE)) {
            return new Selection(context.getRoute());
        } else if (attr.equals(STATUS)) {
            return new Selection(context.getStatus());
        } else {
            String msg = handler.handleBadVariable("Unable to process variable name: " + name);
            if (strict) throw new IllegalArgumentException(msg);
            return null;
        }
    }

    @Override
    public Class<Feedback> getContextObjectClass() {
        return Feedback.class;
    }
}
