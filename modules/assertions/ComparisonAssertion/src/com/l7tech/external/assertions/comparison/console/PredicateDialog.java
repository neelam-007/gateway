/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.console;

import com.l7tech.common.gui.widgets.OkCancelDialog;
import com.l7tech.common.logic.*;

import java.awt.*;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * @author alex
 */
public class PredicateDialog extends OkCancelDialog<Predicate> {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.external.assertions.comparison.console.resources.ComparisonAssertion");

    private static final Map<Class<? extends Predicate>, Class<? extends PredicatePanel>> predicatePanelMap =
            new HashMap<Class<? extends Predicate>, Class<? extends PredicatePanel>>();

    static {
        predicatePanelMap.put(BinaryPredicate.class, BinaryPredicatePanel.class);
        predicatePanelMap.put(CardinalityPredicate.class, CardinalityPredicatePanel.class);
        predicatePanelMap.put(RegexPredicate.class, RegexPredicatePanel.class);
        predicatePanelMap.put(StringLengthPredicate.class, StringLengthPredicatePanel.class);
    }

    static PredicateDialog make(Frame owner, Predicate predicate, String expr) {
        return new PredicateDialog(predicate, owner, makePanel(predicate, expr));
    }

    static PredicateDialog make(Dialog owner, Predicate predicate, String expr) {
        return new PredicateDialog(predicate, owner, makePanel(predicate, expr));
    }

    private static PredicatePanel makePanel(Predicate predicate, String expr) {
        Class<? extends PredicatePanel> panelClass = predicatePanelMap.get(predicate.getClass());
        try {
            Constructor<? extends PredicatePanel> ctor = panelClass.getConstructor(predicate.getClass(), String.class);
            return ctor.newInstance(predicate, expr);
        } catch (Exception e) {
            throw new RuntimeException(e); // Shouldn't happen if the map is complete
        }
    }

    private PredicateDialog(Predicate pred, Frame owner, PredicatePanel panel) throws HeadlessException {
        super(owner, resources.getString(pred.getSimpleName() + "PredicatePanel.dialogTitle"), true, panel);
    }

    private PredicateDialog(Predicate pred, Dialog owner, PredicatePanel panel) throws HeadlessException {
        super(owner, resources.getString(pred.getSimpleName() + "PredicatePanel.dialogTitle"), true, panel);
    }
}
