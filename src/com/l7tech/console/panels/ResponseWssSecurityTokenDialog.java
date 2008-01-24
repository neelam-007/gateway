package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.ResponseWssSecurityToken;
import com.l7tech.common.gui.widgets.OkCancelDialog;

import java.awt.*;
import java.util.ResourceBundle;

/**
 * @author alex
 */
public class ResponseWssSecurityTokenDialog extends OkCancelDialog<ResponseWssSecurityToken> {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.ResponseWssSecurityTokenDialog");


    public ResponseWssSecurityTokenDialog(Frame owner, boolean modal, ResponseWssSecurityToken assertion, boolean readOnly) {
        super(owner, resources.getString("dialog.title"), modal, new ResponseWssSecurityTokenPanel(assertion), readOnly);
    }

    public ResponseWssSecurityTokenDialog(Dialog owner, boolean modal, ResponseWssSecurityToken assertion, boolean readOnly) {
        super(owner, resources.getString("dialog.title"), modal, new ResponseWssSecurityTokenPanel(assertion), readOnly);
    }
}
