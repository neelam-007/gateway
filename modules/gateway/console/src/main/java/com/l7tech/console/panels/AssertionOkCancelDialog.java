package com.l7tech.console.panels;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.gui.widgets.ValidatedPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Adapts OkCancelDialog so it can be used as an AssertionPropertiesEditor.
 */
public class AssertionOkCancelDialog<AT extends Assertion> extends OkCancelDialog<AT> implements AssertionPropertiesEditor<AT> {
    protected final AT initialAssertion;

    public AssertionOkCancelDialog(Frame owner, String title, ValidatedPanel panel, AT assertion) {
        super(owner, title, true, panel);
        initialAssertion = assertion;
    }

    public AssertionOkCancelDialog(Frame owner, String title, boolean modal, ValidatedPanel panel, boolean readOnly, AT assertion) {
        super(owner, title, modal, panel, readOnly);
        initialAssertion = assertion;
    }

    public AssertionOkCancelDialog(Dialog owner, String title, boolean modal, ValidatedPanel panel, boolean readOnly, AT assertion) {
        super(owner, title, modal, panel, readOnly);
        initialAssertion = assertion;
    }

    public JDialog getDialog() {
        return this;
    }

    public boolean isConfirmed() {
        return wasOKed();
    }

    public void setData(AT assertion) {
        // Limitation of dialogs based on OkCancelDialog/ValidatedPanel
        if (assertion != initialAssertion)
            throw new UnsupportedOperationException("Unable to change assertion bean after dialog construction");
    }

    public AT getData(AT assertion) {
        return getValue();
    }

    public void setParameter(String name, Object value) {
        if (PARAM_READONLY.equals(name))
            setReadOnly((Boolean)value);
    }

    public Object getParameter(String name) {
        if (PARAM_READONLY.equals(name))
            return isReadOnly();
        return null;
    }
}
