package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.AddWssSecurityToken;

import javax.swing.*;
import java.awt.*;

/**
 * Properties dialog for "Add Security Token" assertion.
 */
public class AddWssSecurityTokenDialog extends AssertionPropertiesOkCancelSupport<AddWssSecurityToken> {
    AddWssSecurityTokenPanel tokenPanel;
    final AddWssSecurityToken assertion;

    public AddWssSecurityTokenDialog(Frame owner, AddWssSecurityToken assertion) {
        super(AddWssSecurityToken.class, owner, assertion, true);
        this.assertion = assertion;
        super.initComponents();
    }

    @Override
    public void setData(AddWssSecurityToken assertion) {
        tokenPanel.setModel(assertion, getPreviousAssertion());
    }

    @Override
    public AddWssSecurityToken getData(AddWssSecurityToken assertion) throws ValidationException {
        return tokenPanel.getData();
    }

    @Override
    protected JPanel createPropertyPanel() {
        tokenPanel = new AddWssSecurityTokenPanel(assertion);
        return tokenPanel;
    }
}
