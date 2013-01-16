package com.l7tech.server.policy.variable;

import com.l7tech.policy.variable.Syntax;

import java.text.MessageFormat;
import java.util.Collection;

public class LengthSuffixSelector implements ExpandVariables.SuffixSelector {

    @Override
    public ExpandVariables.Selector.Selection select(Object value, Syntax.SyntaxErrorHandler handler, boolean strict) {
        if (value != null) {
            if (value.getClass().isArray()) {
                return new ExpandVariables.Selector.Selection(((Object[]) value).length);
            } else if (value instanceof Collection) {
                return new ExpandVariables.Selector.Selection(((Collection) value).size());
            }
        }
        String msg = handler.handleBadVariable(MessageFormat.format("{0} on {1}", getSuffix(), value == null ? "Null" : value.getClass().getName()));
        if (strict) throw new IllegalArgumentException(msg);
        return ExpandVariables.Selector.NOT_PRESENT;
    }

    @Override
    public String getSuffix() {
        return "length";
    }
}
