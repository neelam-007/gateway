/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.policy.assertion.xmlsec.RequestWssTimestamp;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * @author alex
 */
public class RequestWssTimestampDialog extends OkCancelDialog<RequestWssTimestamp> implements AssertionPropertiesEditor<RequestWssTimestamp> {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.RequestWssTimestampDialog");

    private final RequestWssTimestamp initialAssertion;

    //public FooBarPropertiesDialog(Frame parent, FooBarAssertion bean)
    public RequestWssTimestampDialog(Frame owner, RequestWssTimestamp assertion) {
        super(owner, resources.getString("dialog.title"), true, new RequestWssTimestampPanel(assertion));
        initialAssertion = assertion;
    }

    public RequestWssTimestampDialog(Frame owner, boolean modal, RequestWssTimestamp assertion, boolean readOnly) {
        super(owner, resources.getString("dialog.title"), modal, new RequestWssTimestampPanel(assertion), readOnly);
        initialAssertion = assertion;
    }

    public RequestWssTimestampDialog(Dialog owner, boolean modal, RequestWssTimestamp assertion, boolean readOnly) {
        super(owner, resources.getString("dialog.title"), modal, new RequestWssTimestampPanel(assertion), readOnly);
        initialAssertion = assertion;
    }

    public JDialog getDialog() {
        return this;
    }

    public boolean isConfirmed() {
        return wasOKed();
    }

    public void setData(RequestWssTimestamp assertion) {
        // Limitation of dialogs based on OkCancelDialog/ValidatedPanel
        if (assertion != initialAssertion)
            throw new UnsupportedOperationException("Unable to change assertion bean after dialog construction");
    }

    public RequestWssTimestamp getData(RequestWssTimestamp assertion) {
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
