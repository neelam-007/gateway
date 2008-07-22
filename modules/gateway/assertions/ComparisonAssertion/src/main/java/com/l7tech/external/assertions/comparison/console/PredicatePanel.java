/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.console;

import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.external.assertions.comparison.Predicate;

/**
 * @author alex
 */
public abstract class PredicatePanel<P extends Predicate> extends ValidatedPanel<P> {
    protected final P predicate;
    protected final String expression;

    public PredicatePanel(P predicate, String expr) {
        super("predicate");
        this.predicate = predicate;
        this.expression = expr;
    }

    protected P getModel() {
        return predicate;
    }
}
