package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.ResponseWssSecurityToken;

import java.awt.*;
import java.util.ResourceBundle;

/**
 * @author alex
 */
public class ResponseWssSecurityTokenDialog extends OkCancelDialog {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.ResponseWssSecurityTokenDialog");


    public ResponseWssSecurityTokenDialog(Frame owner, boolean modal, ResponseWssSecurityToken assertion) {
        super(owner, resources.getString("dialog.title"), modal, new ResponseWssSecurityTokenPanel(assertion));
    }

    public ResponseWssSecurityTokenDialog(Dialog owner, boolean modal, ResponseWssSecurityToken assertion) {
        super(owner, resources.getString("dialog.title"), modal, new ResponseWssSecurityTokenPanel(assertion));
    }
}
