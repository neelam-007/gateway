package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.AddWssSecurityToken;
import com.l7tech.gui.widgets.OkCancelDialog;

import java.awt.*;
import java.util.ResourceBundle;

/**
 * @author alex
 */
public class AddWssSecurityTokenDialog extends OkCancelDialog<AddWssSecurityToken> {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.AddWssSecurityTokenDialog");


    public AddWssSecurityTokenDialog(Frame owner, boolean modal, AddWssSecurityToken assertion, boolean readOnly) {
        super(owner, resources.getString("dialog.title"), modal, new AddWssSecurityTokenPanel(assertion), readOnly);
    }

    public AddWssSecurityTokenDialog(Dialog owner, boolean modal, AddWssSecurityToken assertion, boolean readOnly) {
        super(owner, resources.getString("dialog.title"), modal, new AddWssSecurityTokenPanel(assertion), readOnly);
    }
}
