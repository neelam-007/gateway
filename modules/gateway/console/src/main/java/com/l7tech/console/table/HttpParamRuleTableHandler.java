package com.l7tech.console.table;

import com.l7tech.policy.assertion.HttpPassthroughRule;
import com.l7tech.policy.assertion.HttpPassthroughRuleSet;

import javax.swing.*;

/**
 * Specific validation for http parameters (see base class).
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 1, 2007<br/>
 */
public class HttpParamRuleTableHandler extends HttpRuleTableHandler {
    public HttpParamRuleTableHandler(final JTable table, final JButton addButton, final JButton removeButton,
                                     final JButton editButton, HttpPassthroughRuleSet data) {
        super("Parameter", table, addButton, removeButton, editButton, data);

    }
    protected boolean validateNewRule(HttpPassthroughRule in) {
        // it's all good
        return true;
    }
}
