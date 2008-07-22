/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.console;

import com.l7tech.external.assertions.comparison.StringLengthPredicate;

/**
 * @author alex
 */
public class StringLengthPredicatePanel extends MinMaxPredicatePanel<StringLengthPredicate> {
    public StringLengthPredicatePanel(StringLengthPredicate predicate, String expression) {
        super(predicate, expression);
        init();
    }
}