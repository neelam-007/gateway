package com.l7tech.console.table;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.policy.assertion.HttpPassthroughRuleSet;
import com.l7tech.policy.assertion.HttpPassthroughRule;

import javax.swing.*;

/**
 * Specific validations for http headers (see base class).
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 1, 2007<br/>
 */
public class HttpHeaderRuleTableHandler extends HttpRuleTableHandler{
    public HttpHeaderRuleTableHandler(final JTable table, final JButton addButton,
                                      final JButton removeButton, final JButton editButton, HttpPassthroughRuleSet data) {
        super("Header", table, addButton, removeButton, editButton, data);
    }

    protected boolean validateNewRule(HttpPassthroughRule in) {
        String name = in.getName().toLowerCase();
        //Allow to set the host, the host will be handled with virtual host
        if ( HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD.contains( name )
                && !HttpConstants.HEADER_HOST.equalsIgnoreCase(name)) {
                JOptionPane.showMessageDialog(table, "Custom rules can't be defined for this header name.",
                                              "Error", JOptionPane.WARNING_MESSAGE);
                return false;
        }
        return true;
    }
}
