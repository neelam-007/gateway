/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.console;

import com.l7tech.external.assertions.comparison.CardinalityPredicate;

/**
 * @author alex
 */
public class CardinalityPredicatePanel extends MinMaxPredicatePanel<CardinalityPredicate> {
    public CardinalityPredicatePanel(CardinalityPredicate predicate, String expression) {
        super(predicate, expression);
        init();
    }
}
