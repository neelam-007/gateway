/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server.evaluate;

import com.l7tech.external.assertions.comparison.RegexPredicate;

import java.util.regex.Pattern;

/**
 * Evaluator for {@link RegexPredicate}s.
 * @author alex
 */
public class RegexEvaluator extends SingleValuedEvaluator<RegexPredicate> {
    private final Pattern pattern;

    public RegexEvaluator(RegexPredicate predicate) {
        super(predicate);
        this.pattern = Pattern.compile(predicate.getPattern());
    }

    public boolean evaluate(Object leftValue) {
        return pattern.matcher(leftValue.toString()).matches();
    }
}
